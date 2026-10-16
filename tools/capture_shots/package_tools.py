"""Create a standalone collection-tool directory from an explicit source allowlist."""
from pathlib import Path
import argparse
import shutil

ROOT=Path(__file__).resolve().parents[2]
PACKAGE_FILES=(
    'tools/capture_shots/__init__.py','tools/capture_shots/protocol.py','tools/capture_shots/replay.py',
    'tools/capture_shots/models.py','tools/capture_shots/analyze.py','tools/capture_shots/package_tools.py','tools/capture_shots/README.md',
    'tools/capture_shots/tests/test_protocol.py','tools/capture_shots/tests/test_replay.py','tools/capture_shots/tests/test_models.py',
    'tools/capture_shots/tests/test_analysis.py','tools/capture_shots/tests/test_package.py',
    'docs/schemas/capture-shots-v1.schema.json','docs/CAPTURE_SHOT_RECORDING.md',
    'src/test/resources/shots/isolated.jsonl','src/test/resources/shots/ambiguous.jsonl','src/test/resources/shots/partial.jsonl',
    'src/test/resources/shots/invalid.jsonl','src/test/resources/shots/nonfinite.jsonl','src/test/resources/shots/reused-token.jsonl',
    'src/test/resources/shots/utf8-tail.jsonl.part','src/test/resources/shots/linear-fit.jsonl','src/test/resources/shots/irregular-fit.jsonl',
    'src/test/resources/shots/accelerating-fit.jsonl','src/test/resources/shots/accelerating-validation.jsonl','src/test/resources/shots/service-export.jsonl')

def build_package(output):
    output=Path(output)
    if output.exists() or output.is_symlink():raise ValueError('package output must be a new directory')
    sources=[ROOT/relative for relative in PACKAGE_FILES]
    if not all(p.is_file() for p in sources):raise ValueError('missing allowlisted collection-kit source')
    output.mkdir(parents=True,exist_ok=False)
    for relative,source in zip(PACKAGE_FILES,sources):
        destination=output/relative;destination.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,destination)
    return output

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',type=Path,required=True)
    print(build_package(parser.parse_args().output))

if __name__=='__main__':main()
