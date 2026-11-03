import json
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


class StoreTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.path = Path(self.temp.name)

    def store(self, **limits):
        return c.Store(self.path, c.Limits(**limits))

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


if __name__ == "__main__":
    unittest.main()
