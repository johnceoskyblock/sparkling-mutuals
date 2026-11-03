"""One manually started bounded official-quote research run; no mod integration."""

import argparse
from dataclasses import asdict, dataclass
import json
import multiprocessing
import os
from pathlib import Path
import time

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

    def __init__(self, path, limits=Limits()):
        self.path, self.limits = Path(path), limits
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
        if samples.exists() and samples.stat().st_size:
            with samples.open("rb") as stream:
                stream.seek(max(0, samples.stat().st_size - 65_536))
                tail = stream.read()
            if not tail.endswith(b"\n"):
                raise ValueError("partial_record_preserve_evidence")
            row = json.loads(tail.splitlines()[-1])
            if not isinstance(row, dict) or not 0 < row.get("attempt", 0) <= state["attempts"]:
                raise ValueError("invalid_last_record")
        self.state = state

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
        content = encoded(self.state)
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
                          "status": "running", "pid": os.getpid(), "lastGoodServerAt": 0}
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
        if now < s["nextAttemptAt"]:
            return None
        limit = min(ENCODED_LIMIT, self.limits.download - s["chargedBytes"])
        s.update(attempts=s["attempts"] + 1, reserved=limit,
                 chargedBytes=s["chargedBytes"] + limit, lastAttemptAt=now,
                 nextAttemptAt=now + 300_000, pid=os.getpid())
        # Persist conservative charge BEFORE making any request. A crash never refunds it.
        self.save()
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
        if result.get("status") == "ok":
            source_at = result["serverAt"]
            if source_at <= s["lastGoodServerAt"]:
                result["status"] = "duplicate_snapshot" if source_at == s["lastGoodServerAt"] else "out_of_order_snapshot"
            else:
                s["lastGoodServerAt"] = source_at
        row = {"schema": 1, "attempt": s["attempts"], "attemptedAt": s["lastAttemptAt"], "finishedAt": now, **result}
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
                 acceptedSnapshots=s["acceptedSnapshots"] + (result.get("status") == "ok"),
                 lastResult=result.get("status"), lastReceivedAt=result.get("receivedAt"),
                 lastServerAt=result.get("serverAt"), lastQuoteCount=len(result.get("quotes", {})))
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


def _fetch_child(sender, target, limit):
    try:
        sender.send(target(limit))
    except Exception:
        sender.send({"status": "worker_error", "bodyBytes": None})
    finally:
        sender.close()


def timed_fetch(limit, timeout=20, target=fetch_official):
    context = multiprocessing.get_context("spawn")
    receiver, sender = context.Pipe(duplex=False)
    child = context.Process(target=_fetch_child, args=(sender, target, limit), daemon=True)
    started = time.monotonic()
    try:
        child.start()
        sender.close()
        if receiver.poll(max(0, timeout - (time.monotonic() - started))):
            try:
                return receiver.recv()
            except EOFError:
                return {"status": "worker_error", "bodyBytes": None}
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


def run(path):
    with SingleWorker(path):
        store = Store(path)
        wall_start, mono_start = time.time(), time.monotonic()
        try:
            while True:
                now = time.time_ns() // 1_000_000
                if abs(time.time() - wall_start - (time.monotonic() - mono_start)) > 60:
                    if store.state:
                        store.terminal("clock_change")
                    return
                reservation = store.begin(now)
                if reservation:
                    remaining = max(0, (store.state["deadlineAt"] - now) / 1000)
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
    state = Store(path).state.copy()
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
