"""Bounded anonymous official quotes. Never substitute quick_status prices."""

import hashlib
import http.client
import json
import math
import time
import zlib

IDS = tuple("SHARD_" + name for name in """
FOXTROT BLUEBIRD HONEYBUG TREEFROG WOODCHUCKER FLUFFLING HIDEONFLOOR
PARAKEET MACAW CAVERNFISH FLITTER SHYWORM DRIFTLING CHUCKWALLA ROCKMITE
SCRAPPY SNOOZLE GEMZIE STRONGARM TEPID POLARIS SHUDDERSQUID BILLYGOAT
MANTIS_SHRIMP NOZZLENOSE TROODON WUMPA AREITA BLOODBAT DUPLICO GAZER
LITTERBUG SOLSNATCHER GIMMIEGOLD HIDEONWALL HIDEYHO DOOMSPIRAL
""".split())
ENCODED_LIMIT = 1024 * 1024
EXPANDED_LIMIT = 8 * 1024 * 1024


def positive(value):
    return type(value) in (int, float) and math.isfinite(value) and value > 0


def parse_quotes(raw, received_at):
    root = json.loads(raw)
    if not isinstance(root, dict) or root.get("success") is not True:
        raise ValueError("invalid_envelope")
    at = root.get("lastUpdated")
    if type(at) is not int or at <= 0 or not -60_000 <= received_at - at <= 180_000:
        raise ValueError("invalid_source_time")
    products = root.get("products")
    if not isinstance(products, dict):
        raise ValueError("missing_products")
    quotes, missing = {}, {}
    for product_id in IDS:
        product = products.get(product_id)
        if not isinstance(product, dict) or product.get("product_id", product_id) != product_id:
            missing[product_id] = "missing_or_wrong_identity"
            continue
        sides = []
        for key in ("buy_summary", "sell_summary"):
            entries = product.get(key)
            valid = []
            if isinstance(entries, list):
                for order in entries:
                    if (isinstance(order, dict) and positive(order.get("pricePerUnit"))
                            and positive(order.get("amount")) and positive(order.get("orders"))):
                        valid.append(float(order["pricePerUnit"]))
            sides.append(valid)
        asks, bids = sides
        if asks and bids and max(bids) <= min(asks):
            quotes[product_id] = {"ask": min(asks), "bid": max(bids)}
        else:
            missing[product_id] = "invalid_or_crossed_book"
    return {"serverAt": at, "receivedAt": received_at, "quotes": quotes, "missing": missing,
            "priceBasis": "official_best_book", "source": "https://api.hypixel.net/v2/skyblock/bazaar"}


def read_body(stream, limit, counter):
    body = bytearray()
    while len(body) < limit:
        chunk = stream.read(min(65_536, limit - len(body)))
        if not chunk:
            return bytes(body)
        counter[0] += len(chunk)
        body.extend(chunk)
    # Conservatively reject a response exactly at the bound without reading a probe byte.
    raise ValueError("encoded_body_limit")


def decode_body(body, encoding, expanded_limit=EXPANDED_LIMIT):
    if encoding in ("", "identity"):
        if len(body) > expanded_limit:
            raise ValueError("expanded_body_limit")
        return body
    if encoding != "gzip":
        raise ValueError("unsupported_encoding")
    decoder = zlib.decompressobj(16 + zlib.MAX_WBITS)
    result = decoder.decompress(body, expanded_limit)
    if not decoder.eof or decoder.unconsumed_tail or decoder.unused_data:
        raise ValueError("truncated_or_oversized_gzip")
    return result


def fetch_official(limit):
    """Called only in a deadline-limited child; no redirects, API keys or disk output."""
    count = [0]
    connection = http.client.HTTPSConnection("api.hypixel.net", timeout=20)
    result = {"status": "network_error", "bodyBytes": 0}
    uncertain = False
    try:
        connection.request("GET", "/v2/skyblock/bazaar", headers={
            "Accept-Encoding": "gzip", "User-Agent": "SparklingMutuals-local-shard-research/1"})
        response = connection.getresponse()
        result["httpStatus"] = response.status
        retry = response.getheader("Retry-After", "")
        if retry.isdigit():
            result["retryAfter"] = min(int(retry), 86_400)
        body = read_body(response, limit, count)
        received_at = time.time_ns() // 1_000_000
        result["receivedAt"] = received_at
        result["bodySha256"] = hashlib.sha256(body).hexdigest()
        if response.status != 200:
            result["status"] = "http_error"
        else:
            expanded = decode_body(body, response.getheader("Content-Encoding", "").lower())
            result.update(parse_quotes(expanded, received_at))
            result["status"] = "ok"
    except (OSError, ValueError, http.client.HTTPException, zlib.error) as error:
        # HTTP/socket reads can consume partial data before raising; never refund unknown bytes.
        uncertain = isinstance(error, (OSError, http.client.HTTPException))
        result["status"] = "invalid_response" if isinstance(error, ValueError) else "network_error"
        result["errorType"] = type(error).__name__
    finally:
        result["bodyBytes"] = None if uncertain else count[0]
        connection.close()
    return result
