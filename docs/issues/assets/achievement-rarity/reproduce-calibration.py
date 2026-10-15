"""Read-only workbook reproduction and approved mapping audit (Python standard library)."""
import csv
import hashlib
import json
import math
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[4]
ASSETS = Path(__file__).resolve().parent
NS = {"s": "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
SHA = "f111ebfb4e88d4f488728b4ea6c6ca4c22d2b7cded357f6f790fb0a7e2863e36"
workbook = ASSETS / "expected-runs-diamond-emblem.xlsx"
assert hashlib.sha256(workbook.read_bytes()).hexdigest() == SHA
extraction = json.loads((ASSETS / "workbook-extract.json").read_text(encoding="utf-8"))
assert extraction["source_sha256"] == SHA
with zipfile.ZipFile(workbook) as archive:
    cells = {c.attrib["r"]: c for c in ET.fromstring(archive.read("xl/worksheets/sheet4.xml")).findall(".//s:c", NS)}
    def value(cell):
        return float(cells[cell].find("s:v", NS).text)
    rates = [row["spawns_per_run"] for row in extraction["species"]]
    for row in extraction["species"]:
        assert row["spawns_per_run"] == value(row["spawn_source_formula"].split("!")[1])
    full = [value("B" + row["spawn_source_formula"].split("!")[1][1:]) for row in extraction["species"]]
    lazy = [value("C" + row["spawn_source_formula"].split("!")[1][1:]) for row in extraction["species"]]

logq = math.log(2047 / 2048)
def reproduce(exposures):
    def log_cdf(n):
        return sum(math.log(-math.expm1(x * n * logq)) for x in exposures) if n else -math.inf
    def survival(n):
        return -math.expm1(log_cdf(n))
    mean = sum(survival(n) for n in range(20001))
    second = sum((2*n + 1)*survival(n) for n in range(20001))
    quantiles = {}
    for p in [.5, .75, .9, .95, .99, .999]:
        quantiles[str(p)] = next(n for n in range(1, 100001) if math.exp(log_cdf(n)) >= p)
    return dict(total=sum(exposures), dry10=math.exp(sum(exposures)*10*logq),
                mean20000=mean, sd20000=math.sqrt(second-mean*mean),
                survival20000=survival(20000), quantiles=quantiles)

rows = list(csv.DictReader((ASSETS / "achievement-mapping-v2.csv").open(encoding="utf-8", newline="")))
source = (ROOT / "src/main/kotlin/net/johnceo/sparklingmutuals/history/Achievements.kt").read_text(encoding="utf-8")
definitions = re.findall(r'Achievement\("([^"]+)", "[^"]*", "[^"]*", AchievementTier\.(\w+)([^\n]*)', source)
existing = {identifier: (tier, re.search(r'Measure\.\w+, ([\d_L]+)', rest)) for identifier, tier, rest in definitions}
old = [row for row in rows if row["id"] != "NEW"]
assert len(old) == len(existing) == 59
assert len({row["id"] for row in old}) == 59
assert {row["id"] for row in old} == set(existing)
assert len({row["new_id"] for row in rows}) == len(rows) == 78
for row in rows:
    assert all(row[key] for key in ["id", "new_id", "new_goal", "new_tier", "scope", "sbo_anchor", "rationale"])
    if row["id"] == "NEW":
        assert not row["old_goal"] and not row["old_tier"]
    else:
        tier, goal = existing[row["id"]]
        assert row["old_tier"] == tier, row["id"]
        assert int(row["old_goal"]) == (int(goal[1].replace("_", "").replace("L", "")) if goal else 1), row["id"]
print(json.dumps(dict(source_sha256=SHA, baseline=reproduce(rates), full=reproduce(full), lazy=reproduce(lazy),
                     mapping=dict(existing=59, additions=19, rows=78, unique_destinations=78)), indent=2))
