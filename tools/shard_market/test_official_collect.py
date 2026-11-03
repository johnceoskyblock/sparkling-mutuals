import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import time
import unittest

import official_collect as c


NOW = 1_793_686_200_000


def slow_fetch(limit):
    time.sleep(10)
    return {"status": "ok", "bodyBytes": 0}


class SimulatedStore(c.Store):
    """Old wall-time fixtures advance an independent fake elapsed clock alongside time."""
    def __init__(self, path, limits):
        self.tick = 0.0
        super().__init__(path, limits, monotonic=lambda: self.tick)

    def begin(self, now):
        self.tick = max(self.tick, (now - NOW) / 1000)
        return super().begin(now)


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name)

    def store(self, **limits):
        return SimulatedStore(self.path, c.Limits(**limits))

    def test_restart_keeps_deadline_throttle_attempts_and_crash_reservation(self):
        store = self.store()
        first = store.begin(NOW)
        self.assertEqual(1, first["attempt"])
        resumed = self.store()
        self.assertIsNone(resumed.begin(NOW + 1))
        self.assertEqual(NOW + 15 * 86_400_000, resumed.state["deadlineAt"])
        self.assertEqual(1_048_576, resumed.state["chargedBytes"])
        second = resumed.begin(NOW + 300_000)
        self.assertEqual(2, second["attempt"])
        self.assertEqual(2_097_152, resumed.state["chargedBytes"])

    def test_success_persists_only_selected_result_and_refunds_unused_reservation(self):
        store = self.store()
        store.begin(NOW)
        store.finish({"status": "ok", "bodyBytes": 100, "quotes": {
            "SHARD_HIDEONWALL": {"ask": 101, "bid": 99}}, "serverAt": NOW,
            "receivedAt": NOW + 1}, NOW + 1)
        row = json.loads((self.path / "samples.jsonl").read_text())
        self.assertEqual(1, row["attempt"])
        self.assertEqual(101, row["quotes"]["SHARD_HIDEONWALL"]["ask"])
        self.assertEqual(100, self.store().state["chargedBytes"])

    def test_uncertain_timeout_keeps_full_download_charge(self):
        store = self.store()
        store.begin(NOW)
        store.finish({"status": "timeout", "bodyBytes": None}, NOW + 20_000)
        self.assertEqual(1_048_576, self.store().state["chargedBytes"])

    def test_request_cap_cannot_be_reset_by_restart(self):
        store = self.store(attempts=1)
        store.begin(NOW)
        self.assertIsNone(self.store(attempts=1).begin(NOW + 300_000))
        self.assertEqual("attempt_limit", self.store(attempts=1).state["status"])

    def test_download_budget_limits_final_reservation_and_never_goes_negative(self):
        store = self.store(download=1_048_580)
        store.begin(NOW)
        self.assertEqual(4, store.begin(NOW + 300_000)["limit"])
        self.assertIsNone(store.begin(NOW + 600_000))
        self.assertEqual(1_048_580, store.state["chargedBytes"])

    def test_original_deadline_stop_and_clock_regression_prevent_requests(self):
        store = self.store()
        store.begin(NOW)
        self.assertIsNone(store.begin(NOW + 15 * 86_400_000))
        self.assertEqual("deadline", store.state["status"])
        # A fresh collection has separate storage; no resetting an existing run.
        with tempfile.TemporaryDirectory() as second:
            other = c.Store(Path(second))
            other.begin(NOW)
            self.assertIsNone(other.begin(NOW - 60_001))
            self.assertEqual("clock_regression", other.state["status"])

    def test_stop_marker_prevents_next_request_and_survives_restart(self):
        store = self.store()
        store.begin(NOW)
        result = subprocess.run([sys.executable, "-B", str(Path(c.__file__)), "stop", "--data-dir", str(self.path)],
                                capture_output=True, text=True, timeout=5)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIsNone(store.begin(NOW + 300_000))
        self.assertEqual("stopped", self.store().state["status"])

    def test_status_distinguishes_a_live_lock_from_a_dead_process(self):
        store = self.store()
        store.begin(NOW)
        with c.SingleWorker(self.path):
            self.assertTrue(c.read_status(self.path)["workerRunning"])
        self.assertFalse(c.read_status(self.path)["workerRunning"])

    def test_missing_state_or_partial_record_is_not_silently_reset(self):
        (self.path / "samples.jsonl").write_text('{"attempt":1}\n')
        with self.assertRaises(ValueError):
            self.store()
        (self.path / "samples.jsonl").unlink()
        store = self.store()
        store.begin(NOW)
        (self.path / "samples.jsonl").write_text('{"attempt":1')
        with self.assertRaises(ValueError):
            self.store()

    def test_existing_state_with_corrupt_limits_cannot_restart(self):
        store = self.store()
        store.begin(NOW)
        state = json.loads((self.path / "state.json").read_text())
        state["chargedBytes"] = -1
        (self.path / "state.json").write_text(json.dumps(state))
        with self.assertRaises(ValueError):
            self.store()

    def test_output_bound_stops_before_a_record_would_exceed_it(self):
        store = self.store(disk=40_000)
        store.begin(NOW)
        store.finish({"status": "invalid_response", "bodyBytes": 100, "padding": "x" * 35_000}, NOW + 1)
        self.assertEqual("disk_limit", store.state["status"])
        self.assertLessEqual(sum(p.stat().st_size for p in self.path.iterdir()), 40_000)
        self.assertEqual("disk_limit", self.store(disk=40_000).state["status"])

    def test_corrupt_next_attempt_time_does_not_bypass_restart_throttle(self):
        store = self.store()
        store.begin(NOW)
        state = json.loads((self.path / "state.json").read_text())
        state["nextAttemptAt"] = 0
        (self.path / "state.json").write_text(json.dumps(state))
        with self.assertRaises(ValueError):
            self.store()

    def test_duplicate_or_older_source_time_cannot_count_as_a_fresh_snapshot(self):
        store = self.store()
        for attempted_at, source_at in ((NOW, NOW), (NOW + 300_000, NOW), (NOW + 600_000, NOW - 1000)):
            store.begin(attempted_at)
            store.finish({"status": "ok", "bodyBytes": 10, "serverAt": source_at,
                          "receivedAt": attempted_at, "quotes": {}}, attempted_at)
        self.assertEqual(1, store.state["acceptedSnapshots"])
        self.assertEqual("out_of_order_snapshot", store.state["lastResult"])

    def test_retry_after_delays_next_attempt_without_catchup(self):
        store = self.store()
        store.begin(NOW)
        store.finish({"status": "http_error", "httpStatus": 429, "bodyBytes": 100,
                      "retryAfter": 600}, NOW + 1)
        self.assertIsNone(store.begin(NOW + 300_000))
        self.assertIsNotNone(store.begin(NOW + 600_001))

    def test_duplicate_worker_is_excluded_until_lock_released(self):
        with c.SingleWorker(self.path):
            with self.assertRaises(RuntimeError):
                with c.SingleWorker(self.path):
                    self.fail("duplicate worker acquired the lock")
        with c.SingleWorker(self.path):
            pass

    def test_stalled_request_child_is_terminated_and_retains_uncertain_charge(self):
        start = time.monotonic()
        result = c.timed_fetch(100, timeout=0.3, target=slow_fetch)
        self.assertEqual("timeout", result["status"])
        self.assertIsNone(result["bodyBytes"])
        self.assertLess(time.monotonic() - start, 5)

    def test_forward_clock_jump_cannot_shorten_monotonic_spacing(self):
        tick = [0.0]
        store = c.Store(self.path, monotonic=lambda: tick[0])
        store.begin(NOW)
        store.finish({"status": "invalid_response", "bodyBytes": 10}, NOW)
        tick[0] = 241.0
        self.assertIsNone(store.begin(NOW + 300_000))
        tick[0] = 300.0
        self.assertIsNotNone(store.begin(NOW + 359_000))

    def test_restart_waits_a_full_monotonic_interval_even_after_forward_clock_change(self):
        tick = [0.0]
        store = c.Store(self.path, monotonic=lambda: tick[0])
        store.begin(NOW)
        tick[0] = 1.0
        resumed = c.Store(self.path, monotonic=lambda: tick[0])
        self.assertIsNone(resumed.begin(NOW + 600_000))
        tick[0] = 301.0
        self.assertIsNotNone(resumed.begin(NOW + 900_000))

    def test_tolerated_backward_clock_adjustment_cannot_extend_run_deadline(self):
        from unittest.mock import patch
        tick = [0.0]
        original_store = c.Store

        def wall():
            return NOW / 1000 + tick[0] - (59 if tick[0] else 0)

        def sleep(seconds):
            tick[0] += seconds

        def store(path):
            return original_store(path, c.Limits(duration=1000), monotonic=lambda: tick[0])

        with patch.object(c, "Store", store), patch.object(c.time, "time", wall), \
                patch.object(c.time, "time_ns", lambda: int(wall() * 1_000_000_000)), \
                patch.object(c.time, "monotonic", lambda: tick[0]), patch.object(c.time, "sleep", sleep), \
                patch.object(c, "timed_fetch", lambda *a, **k: {"status": "invalid_response", "bodyBytes": 0}):
            c.run(self.path)
        self.assertLessEqual(tick[0], 2, "one-second test run must not stretch by a 59-second clock adjustment")
        self.assertEqual("deadline", json.loads((self.path / "state.json").read_text())["status"])

    def test_interior_corruption_and_missing_completed_evidence_fail_closed(self):
        store = self.store()
        store.begin(NOW)
        store.finish({"status": "invalid_response", "bodyBytes": 10}, NOW)
        file = self.path / "samples.jsonl"
        original = file.read_bytes()
        for bad in (b"corrupt earlier evidence\n" + original, b"", original.replace(b'invalid_response', b'other_response')):
            file.write_bytes(bad)
            with self.assertRaises(ValueError):
                self.store()
        file.unlink()
        with self.assertRaises(ValueError):
            self.store()

    def test_positive_counter_corruption_is_detected_before_a_request(self):
        store = self.store()
        store.begin(NOW)
        state_path = self.path / "state.json"
        state = json.loads(state_path.read_text())
        state["chargedBytes"] = 1
        state["reserved"] = 0
        state_path.write_text(json.dumps(state))
        with self.assertRaises(ValueError):
            self.store()

    def test_append_before_state_save_crash_recovers_without_refunding_unknown_bytes(self):
        store = self.store()
        store.begin(NOW)
        old_state = (self.path / "state.json").read_bytes()
        store.finish({"status": "invalid_response", "bodyBytes": 10}, NOW)
        (self.path / "state.json").write_bytes(old_state)
        resumed = self.store()
        self.assertEqual(1, resumed.state["records"])
        self.assertEqual(1_048_576, resumed.state["chargedBytes"])
        self.assertEqual(2, resumed.begin(NOW + 300_000)["attempt"])

    @unittest.skipUnless(os.name == "nt", "Windows orphan-lifetime regression")
    def test_abrupt_parent_death_stops_its_request_child(self):
        import ctypes
        parent_script = self.path / "orphan_probe.py"
        child_id = self.path / "child.pid"
        parent_script.write_text(
            "import os,sys,time\n"
            f"sys.path.insert(0,{str(Path(c.__file__).parent)!r})\n"
            "from official_collect import timed_fetch\n"
            "def slow(limit):\n"
            f"    open({str(child_id)!r},'w').write(str(os.getpid()))\n"
            "    time.sleep(10)\n"
            "    return {'status':'timeout','bodyBytes':None}\n"
            "if __name__=='__main__': timed_fetch(100,timeout=1,target=slow)\n")
        parent = subprocess.Popen([sys.executable, "-B", str(parent_script)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        kernel = ctypes.WinDLL("kernel32", use_last_error=True)
        kernel.OpenProcess.restype = ctypes.c_void_p
        kernel.WaitForSingleObject.argtypes = [ctypes.c_void_p, ctypes.c_uint32]
        kernel.CloseHandle.argtypes = [ctypes.c_void_p]
        handle = None
        try:
            until = time.monotonic() + 5
            while not child_id.exists() and parent.poll() is None and time.monotonic() < until:
                time.sleep(0.02)
            self.assertTrue(child_id.exists(), "request child must start before killing its parent")
            handle = kernel.OpenProcess(0x100000 | 1, False, int(child_id.read_text()))
            self.assertTrue(handle)
            parent.kill()
            parent.wait(timeout=3)
            self.assertEqual(0, kernel.WaitForSingleObject(handle, 2000), "orphan must exit before a restarted collector can overlap it")
        finally:
            if parent.poll() is None:
                parent.kill()
                parent.wait(timeout=3)
            if handle:
                kernel.TerminateProcess.argtypes = [ctypes.c_void_p, ctypes.c_uint32]
                kernel.TerminateProcess(handle, 1)
                kernel.CloseHandle(handle)


if __name__ == "__main__":
    unittest.main()
