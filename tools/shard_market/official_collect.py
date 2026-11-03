"""One manually started bounded official-quote research run; no mod integration."""

import argparse
import ctypes
from dataclasses import asdict, dataclass
import hashlib
import json
import multiprocessing
import os
from pathlib import Path
import time
import threading

from official_quotes import ENCODED_LIMIT, fetch_official


@dataclass(frozen=True)
class Limits:
    attempts: int = 4320
    download: int = 3 * 1024 ** 3
    disk: int = 64 * 1024 ** 2
    duration: int = 15 * 86_400_000


def encoded(value):
    return (json.dumps(value, separators=(",", ":"), allow_nan=False) + "\n").encode()


class Store:
    HEADROOM = 32_768

    def __init__(self, path, limits=Limits(), monotonic=time.monotonic):
        self.path, self.limits = Path(path), limits
        self.monotonic = monotonic
        self.not_before = monotonic()
        self.path.mkdir(parents=True, exist_ok=True)
        self.state = None
        self.reservation = None
        self.usage()
        state_path = self.path / "state.json"
        samples = self.path / "samples.jsonl"
        if not state_path.exists():
            if samples.exists() or (self.path / "state.tmp").exists():
                raise ValueError("missing_state_preserve_existing_evidence")
            return
        if state_path.stat().st_size > 8192:
            raise ValueError("invalid_state_size")
        state = json.loads(state_path.read_bytes())
        state_hash = state.pop("stateHash", None)
        if state_hash != hashlib.sha256(encoded(state)).hexdigest():
            raise ValueError("corrupt_state_preserve_evidence")
        required = ("startedAt", "deadlineAt", "attempts", "chargedBytes", "lastAttemptAt",
                    "nextAttemptAt", "reserved", "records", "acceptedSnapshots", "lastGoodServerAt")
        if (state.get("version") != 1 or state.get("limits") != asdict(limits)
                or any(type(state.get(k)) is not int or state[k] < 0 for k in required)
                or state["deadlineAt"] != state["startedAt"] + limits.duration
                or state["attempts"] > limits.attempts or state["chargedBytes"] > limits.download
                or state["acceptedSnapshots"] > state["records"] or state["records"] > state["attempts"]
                or state["lastAttemptAt"] < state["startedAt"]
                or (state["attempts"] > 0 and state["nextAttemptAt"] < state["lastAttemptAt"] + 300_000)
                or state["reserved"] > min(ENCODED_LIMIT, state["chargedBytes"])):
            raise ValueError("invalid_state_no_reset")
        self.state = state
        self.validate_evidence(samples)
        # A restart cannot know how the wall clock changed while stopped.
        self.not_before = monotonic() + 300

    def validate_evidence(self, samples):
        s = self.state
        count, accepted, good_at, previous_attempt = 0, 0, 0, 0
        previous_hash = "0" * 64
        prefix_matches = s["records"] == 0 and s.get("recordHash") == previous_hash and s["acceptedSnapshots"] == 0 and s["lastGoodServerAt"] == 0
        last = None
        if samples.exists():
            if samples.stat().st_size > self.limits.disk:
                raise ValueError("evidence_size_limit")
            with samples.open("rb") as stream:
                while line := stream.readline(65_537):
                    if len(line) > 65_536 or not line.endswith(b"\n"):
                        raise ValueError("partial_or_oversized_record")
                    row = json.loads(line)
                    if not isinstance(row, dict):
                        raise ValueError("invalid_record")
                    claimed_hash = row.pop("recordHash", None)
                    actual_hash = hashlib.sha256(encoded(row)).hexdigest()
                    attempt = row.get("attempt")
                    if (claimed_hash != actual_hash or row.get("previousRecordHash") != previous_hash
                            or row.get("schema") != 1 or type(attempt) is not int
                            or not previous_attempt < attempt <= s["attempts"]
                            or type(row.get("attemptedAt")) is not int or type(row.get("finishedAt")) is not int
                            or not isinstance(row.get("status"), str)):
                        raise ValueError("corrupt_evidence")
                    count += 1
                    if count > self.limits.attempts:
                        raise ValueError("too_many_records")
                    if row["status"] == "ok":
                        at = row.get("serverAt")
                        if type(at) is not int or at <= good_at:
                            raise ValueError("invalid_fresh_source_time")
                        accepted += 1
                        good_at = at
                    previous_hash, previous_attempt, last = actual_hash, attempt, row
                    if count == s["records"]:
                        prefix_matches = (actual_hash == s.get("recordHash") and accepted == s["acceptedSnapshots"]
                                          and good_at == s["lastGoodServerAt"])
        if not prefix_matches or count < s["records"] or count > s["records"] + 1:
            raise ValueError("missing_or_inconsistent_evidence")
        if count > s["records"]:
            if not s["reserved"] or last["attempt"] != s["attempts"]:
                raise ValueError("unexplained_extra_record")
            # The append was durable before its state update. Preserve the full byte charge.
            s.update(records=count, acceptedSnapshots=accepted, lastGoodServerAt=good_at,
                     recordHash=previous_hash, reserved=0, lastResult="recovered_completed_record")
            self.save()

    def usage(self):
        if self.path.is_symlink():
            raise ValueError("symlink_output_not_allowed")
        total = 0
        for item in self.path.iterdir():
            if item.is_symlink() or not item.is_file() or item.name not in {
                    "state.json", "state.tmp", "samples.jsonl", "worker.lock", "STOP"}:
                raise ValueError("unexpected_output_preserve_evidence")
            total += item.stat().st_size
        return total

    def save(self):
        snapshot = self.state.copy()
        snapshot["stateHash"] = hashlib.sha256(encoded(self.state)).hexdigest()
        content = encoded(snapshot)
        temporary = self.path / "state.tmp"
        old_temp = temporary.stat().st_size if temporary.exists() else 0
        if len(content) > 8192 or self.usage() - old_temp + len(content) > self.limits.disk:
            raise ValueError("state_disk_limit")
        with temporary.open("wb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, self.path / "state.json")

    def terminal(self, reason):
        self.state["status"] = reason
        self.save()

    def begin(self, now):
        if self.state is None:
            self.state = {"version": 1, "limits": asdict(self.limits), "startedAt": now,
                          "deadlineAt": now + self.limits.duration, "attempts": 0,
                          "chargedBytes": 0, "reserved": 0, "lastAttemptAt": now,
                          "nextAttemptAt": now, "records": 0, "acceptedSnapshots": 0,
                          "status": "running", "pid": os.getpid(), "lastGoodServerAt": 0,
                          "recordHash": "0" * 64}
        s = self.state
        reason = None
        if s["status"] != "running":
            return None
        if (self.path / "STOP").exists():
            reason = "stopped"
        elif now < s["lastAttemptAt"] - 60_000:
            reason = "clock_regression"
        elif now >= s["deadlineAt"]:
            reason = "deadline"
        elif s["attempts"] >= self.limits.attempts:
            reason = "attempt_limit"
        elif s["chargedBytes"] >= self.limits.download:
            reason = "download_limit"
        elif self.usage() >= self.limits.disk - self.HEADROOM:
            reason = "disk_limit"
        if reason:
            self.terminal(reason)
            return None
        if now < s["nextAttemptAt"] or self.monotonic() < self.not_before:
            return None
        limit = min(ENCODED_LIMIT, self.limits.download - s["chargedBytes"])
        s.update(attempts=s["attempts"] + 1, reserved=limit,
                 chargedBytes=s["chargedBytes"] + limit, lastAttemptAt=now,
                 nextAttemptAt=now + 300_000, pid=os.getpid())
        # Persist conservative charge BEFORE making any request. A crash never refunds it.
        self.save()
        self.not_before = self.monotonic() + 300
        self.reservation = {"attempt": s["attempts"], "limit": limit}
        return self.reservation.copy()

    def finish(self, result, now):
        if self.reservation is None:
            raise ValueError("no_current_request")
        s = self.state
        actual = result.get("bodyBytes")
        if actual is not None and (type(actual) is not int or not 0 <= actual <= s["reserved"]):
            raise ValueError("invalid_transfer_count")
        if actual is not None:
            s["chargedBytes"] -= s["reserved"] - actual
        s["reserved"] = 0
        result = dict(result)
        good_at = s["lastGoodServerAt"]
        if result.get("status") == "ok":
            source_at = result["serverAt"]
            if source_at <= s["lastGoodServerAt"]:
                result["status"] = "duplicate_snapshot" if source_at == s["lastGoodServerAt"] else "out_of_order_snapshot"
            else:
                good_at = source_at
        row = {"schema": 1, "attempt": s["attempts"], "attemptedAt": s["lastAttemptAt"], "finishedAt": now, **result}
        row["previousRecordHash"] = s["recordHash"]
        row["recordHash"] = hashlib.sha256(encoded(row)).hexdigest()
        content = encoded(row)
        if len(content) + self.usage() > self.limits.disk - self.HEADROOM:
            self.reservation = None
            self.terminal("disk_limit")
            return
        with (self.path / "samples.jsonl").open("ab") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        s.update(reserved=0, records=s["records"] + 1,
                 recordHash=row["recordHash"], lastGoodServerAt=good_at,
                 acceptedSnapshots=s["acceptedSnapshots"] + (result.get("status") == "ok"),
                 lastResult=result.get("status"), lastReceivedAt=result.get("receivedAt"),
                 lastServerAt=result.get("serverAt"), lastQuoteCount=len(result.get("quotes", {})))
        s["nextAttemptAt"] = max(s["nextAttemptAt"], now + 300_000)
        self.not_before = self.monotonic() + 300
        retry = result.get("retryAfter", 0)
        if type(retry) is int and retry > 0:
            s["nextAttemptAt"] = max(s["nextAttemptAt"], now + min(retry, 86_400) * 1000)
        self.save()
        self.reservation = None


class SingleWorker:
    def __init__(self, path):
        self.path = Path(path)
        self.stream = None

    def __enter__(self):
        self.path.mkdir(parents=True, exist_ok=True)
        if self.path.is_symlink() or (self.path / "worker.lock").is_symlink():
            raise ValueError("symlink_lock_not_allowed")
        self.stream = (self.path / "worker.lock").open("a+b")
        if self.stream.seek(0, 2) == 0:
            self.stream.write(b"0")
            self.stream.flush()
        self.stream.seek(0)
        try:
            if os.name == "nt":
                import msvcrt
                msvcrt.locking(self.stream.fileno(), msvcrt.LK_NBLCK, 1)
            else:
                import fcntl
                fcntl.flock(self.stream, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except OSError as error:
            self.stream.close()
            raise RuntimeError("collector_already_running") from error
        return self

    def __exit__(self, *args):
        self.stream.seek(0)
        if os.name == "nt":
            import msvcrt
            msvcrt.locking(self.stream.fileno(), msvcrt.LK_UNLCK, 1)
        else:
            import fcntl
            fcntl.flock(self.stream, fcntl.LOCK_UN)
        self.stream.close()


class RequestJob:
    """Windows closes this non-inherited handle on parent death, killing its request child."""

    def __init__(self):
        self.handle = None
        if os.name != "nt":
            return
        from ctypes import wintypes as w

        class Basic(ctypes.Structure):
            _fields_ = [("process_time", ctypes.c_longlong), ("job_time", ctypes.c_longlong),
                        ("flags", w.DWORD), ("min_ws", ctypes.c_size_t), ("max_ws", ctypes.c_size_t),
                        ("process_count", w.DWORD), ("affinity", ctypes.c_size_t),
                        ("priority", w.DWORD), ("scheduling", w.DWORD)]

        class Extended(ctypes.Structure):
            _fields_ = [("basic", Basic), ("io", ctypes.c_ulonglong * 6),
                        ("process_memory", ctypes.c_size_t), ("job_memory", ctypes.c_size_t),
                        ("peak_process", ctypes.c_size_t), ("peak_job", ctypes.c_size_t)]

        self.kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        self.kernel.CreateJobObjectW.argtypes = [ctypes.c_void_p, ctypes.c_wchar_p]
        self.kernel.CreateJobObjectW.restype = ctypes.c_void_p
        self.kernel.SetInformationJobObject.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_void_p, w.DWORD]
        self.kernel.OpenProcess.argtypes = [w.DWORD, w.BOOL, w.DWORD]
        self.kernel.OpenProcess.restype = ctypes.c_void_p
        self.kernel.AssignProcessToJobObject.argtypes = [ctypes.c_void_p, ctypes.c_void_p]
        self.kernel.CloseHandle.argtypes = [ctypes.c_void_p]
        self.handle = self.kernel.CreateJobObjectW(None, None)
        if not self.handle:
            raise ctypes.WinError(ctypes.get_last_error())
        limits = Extended()
        limits.basic.flags = 0x2000  # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        if not self.kernel.SetInformationJobObject(self.handle, 9, ctypes.byref(limits), ctypes.sizeof(limits)):
            error = ctypes.get_last_error()
            self.close()
            raise ctypes.WinError(error)

    def assign(self, pid):
        if not self.handle:
            return
        process = self.kernel.OpenProcess(0x0101, False, pid)  # SET_QUOTA | TERMINATE
        if not process:
            raise ctypes.WinError(ctypes.get_last_error())
        try:
            if not self.kernel.AssignProcessToJobObject(self.handle, process):
                raise ctypes.WinError(ctypes.get_last_error())
        finally:
            self.kernel.CloseHandle(process)

    def close(self):
        if self.handle:
            self.kernel.CloseHandle(self.handle)
            self.handle = None


def _fetch_child(sender, target, limit, gate, deadline, parent_pid):
    watchdog = threading.Timer(max(0, deadline - time.monotonic()), os._exit, args=(124,))
    watchdog.daemon = True
    watchdog.start()
    try:
        if os.name != "nt":
            libc = ctypes.CDLL(None, use_errno=True)
            if not hasattr(libc, "prctl") or libc.prctl(1, 9) != 0 or os.getppid() != parent_pid:
                raise RuntimeError("parent_lifetime_control_unavailable")
        # No network before the parent installs OS lifetime control.
        if not gate.wait(max(0, deadline - time.monotonic())):
            return
        sender.send(target(limit))
    except Exception:
        sender.send({"status": "worker_error", "bodyBytes": None})
    finally:
        watchdog.cancel()
        sender.close()


def timed_fetch(limit, timeout=20, target=fetch_official):
    context = multiprocessing.get_context("spawn")
    receiver, sender = context.Pipe(duplex=False)
    started = time.monotonic()
    deadline = started + timeout
    gate = context.Event()
    child = context.Process(target=_fetch_child, args=(sender, target, limit, gate, deadline, os.getpid()), daemon=True)
    job = None
    try:
        job = RequestJob()
        child.start()
        job.assign(child.pid)
        gate.set()
        sender.close()
        if receiver.poll(max(0, timeout - (time.monotonic() - started))):
            try:
                return receiver.recv()
            except EOFError:
                return {"status": "timeout" if time.monotonic() >= deadline else "worker_error", "bodyBytes": None}
        return {"status": "timeout", "bodyBytes": None}
    finally:
        if child.pid is not None:
            child.join(0.05)
            if child.is_alive():
                child.terminate()
                child.join(1)
            if child.is_alive():
                child.kill()
                child.join(1)
            if child.is_alive():
                raise RuntimeError("request_worker_did_not_stop")
            child.close()
        receiver.close()
        sender.close()
        if job:
            job.close()


def run(path):
    with SingleWorker(path):
        store = Store(path)
        wall_start, mono_start = time.time(), time.monotonic()
        remaining_ms = (store.state["deadlineAt"] - int(wall_start * 1000)) if store.state else store.limits.duration
        elapsed_deadline = mono_start + max(0, min(remaining_ms, store.limits.duration)) / 1000
        try:
            while True:
                now = time.time_ns() // 1_000_000
                if time.monotonic() >= elapsed_deadline:
                    if store.state:
                        store.terminal("deadline")
                    return
                if abs(time.time() - wall_start - (time.monotonic() - mono_start)) > 60:
                    if store.state:
                        store.terminal("clock_change")
                    return
                reservation = store.begin(now)
                if reservation:
                    remaining = max(0, min((store.state["deadlineAt"] - now) / 1000,
                                           elapsed_deadline - time.monotonic()))
                    result = timed_fetch(reservation["limit"], timeout=min(20, remaining))
                    store.finish(result, time.time_ns() // 1_000_000)
                elif store.state["status"] != "running":
                    return
                time.sleep(1)
        except KeyboardInterrupt:
            store.terminal("stopped")


def read_status(path):
    path = Path(path)
    state_file = path / "state.json"
    if not state_file.exists():
        return {"status": "not_started", "workerRunning": False}
    if state_file.stat().st_size > 8192:
        raise ValueError("invalid_state_size")
    # State replacement is atomic. Do not audit an append that an active worker may be writing.
    state = json.loads(state_file.read_bytes())
    state["workerRunning"] = False
    if (path / "worker.lock").exists():
        try:
            with SingleWorker(path):
                pass
        except RuntimeError:
            state["workerRunning"] = True
    return state


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("run", "status", "stop"))
    parser.add_argument("--data-dir", required=True, type=Path)
    args = parser.parse_args()
    if args.command == "run":
        run(args.data_dir)
    elif args.command == "stop":
        if not (args.data_dir / "state.json").exists():
            raise ValueError("no_existing_collection")
        (args.data_dir / "STOP").touch()
        print("Stop requested; worker checks within one second between requests, at most 20 seconds during a request.")
    else:
        print(json.dumps(read_status(args.data_dir), indent=2))


if __name__ == "__main__":
    main()
