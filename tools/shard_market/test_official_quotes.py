import gzip
import http.client
import io
import json
import unittest

import official_quotes as q


NOW = 1_793_686_200_000


def payload():
    return {"success": True, "lastUpdated": NOW - 5_000, "products": {
        "SHARD_HIDEONWALL": {"buy_summary": [
            {"pricePerUnit": 105, "amount": 10, "orders": 1},
            {"pricePerUnit": 101, "amount": 2, "orders": 1}],
            "sell_summary": [{"pricePerUnit": 99, "amount": 3, "orders": 1}],
            "quick_status": {"buyPrice": 500, "sellPrice": 2}},
        "UNRELATED": {"buy_summary": [], "sell_summary": []}}}


class QuoteTests(unittest.TestCase):
    def parse(self, root):
        return q.parse_quotes(json.dumps(root).encode(), NOW)

    def test_exact_book_prices_and_receipt_time_not_weighted_values(self):
        result = self.parse(payload())
        self.assertEqual({"ask": 101.0, "bid": 99.0}, result["quotes"]["SHARD_HIDEONWALL"])
        self.assertEqual(NOW, result["receivedAt"])
        self.assertEqual(NOW - 5_000, result["serverAt"])
        self.assertNotIn("UNRELATED", result["quotes"])

    def test_invalid_crossed_and_empty_books_are_missing_not_weighted_fallback(self):
        for mutation in ("crossed", "empty", "zero_amount", "boolean", "identity"):
            with self.subTest(mutation=mutation):
                root = payload()
                product = root["products"]["SHARD_HIDEONWALL"]
                if mutation == "crossed":
                    product["sell_summary"][0]["pricePerUnit"] = 120
                elif mutation == "empty":
                    product["buy_summary"] = []
                elif mutation == "zero_amount":
                    for order in product["buy_summary"]:
                        order["amount"] = 0
                elif mutation == "boolean":
                    for order in product["buy_summary"]:
                        order["pricePerUnit"] = True
                else:
                    product["product_id"] = "SHARD_OTHER"
                result = self.parse(root)
                self.assertNotIn("SHARD_HIDEONWALL", result["quotes"])
                self.assertIn("SHARD_HIDEONWALL", result["missing"])

    def test_bad_envelopes_and_untrustworthy_times_are_rejected(self):
        for at in (NOW + 60_001, NOW - 180_001, True, 0):
            root = payload()
            root["lastUpdated"] = at
            with self.subTest(at=at), self.assertRaises(ValueError):
                self.parse(root)
        with self.assertRaises(ValueError):
            q.parse_quotes(b'{"success":false}', NOW)

    def test_encoded_body_never_reads_past_reservation(self):
        stream = io.BytesIO(b"abcdefghi")
        counter = [0]
        with self.assertRaises(ValueError):
            q.read_body(stream, 5, counter)
        self.assertEqual(5, counter[0])
        self.assertEqual(5, stream.tell())
        self.assertEqual(b"abc", q.read_body(io.BytesIO(b"abc"), 5, [0]))

    def test_gzip_expansion_is_bounded_and_truncation_is_rejected(self):
        self.assertEqual(b"{}", q.decode_body(gzip.compress(b"{}"), "gzip", 20))
        with self.assertRaises(ValueError):
            q.decode_body(gzip.compress(b"x" * 100), "gzip", 20)
        with self.assertRaises(ValueError):
            q.decode_body(gzip.compress(b"{}")[:-4], "gzip", 20)
        with self.assertRaises(ValueError):
            q.decode_body(b"{}", "br", 20)

    def test_partial_transport_failure_is_charged_as_uncertain_not_zero(self):
        class BrokenResponse:
            status = 200

            def getheader(self, name, default=""):
                return default

            def read(self, size):
                raise http.client.IncompleteRead(b"partial", 100)

        class Connection:
            def __init__(self, *args, **kwargs):
                pass

            def request(self, *args, **kwargs):
                pass

            def getresponse(self):
                return BrokenResponse()

            def close(self):
                pass

        from unittest.mock import patch
        with patch.object(q.http.client, "HTTPSConnection", Connection):
            result = q.fetch_official(100)
        self.assertIsNone(result["bodyBytes"], "partial bytes inside a failed read cannot be refunded")

    def test_one_malformed_best_level_cannot_certify_a_worse_price(self):
        root = payload()
        root["products"]["SHARD_HIDEONWALL"]["buy_summary"].append(
            {"pricePerUnit": 100, "amount": "unknown", "orders": 1})
        result = self.parse(root)
        self.assertNotIn("SHARD_HIDEONWALL", result["quotes"])
        self.assertIn("SHARD_HIDEONWALL", result["missing"])


if __name__ == "__main__":
    unittest.main()
