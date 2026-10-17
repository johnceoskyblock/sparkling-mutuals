from pathlib import Path
import tempfile
import unittest
from tools.capture_shots.package_tools import build_package,PACKAGE_FILES

class PackageTest(unittest.TestCase):
    def test_standalone_kit_reads_old_and_new_writer_fixtures(self):
        import subprocess,sys
        with tempfile.TemporaryDirectory() as temp:
            out=Path(temp)/'kit';build_package(out)
            run=subprocess.run([sys.executable,'-c',"from pathlib import Path;from tools.capture_shots.protocol import read_session;from tools.capture_shots.contacts import summarize;assert read_session(Path('src/test/resources/shots/isolated.jsonl')).complete;assert summarize(read_session(Path('src/test/resources/shots/contact-probe-v2.jsonl')))['contact_prediction_available']"],cwd=out,capture_output=True)
            self.assertEqual(0,run.returncode,run.stderr.decode())
    def test_collection_kit_preserves_only_explicit_source_document_and_synthetic_fixture_paths(self):
        with tempfile.TemporaryDirectory() as temp:
            output=Path(temp)/'kit';build_package(output)
            actual={p.relative_to(output).as_posix() for p in output.rglob('*') if p.is_file()}
            self.assertEqual(set(PACKAGE_FILES),actual)
            self.assertFalse(any('__pycache__' in p or '.devAuth' in p or p.startswith('run/') for p in actual))
            with self.assertRaises(ValueError):build_package(output)

if __name__=='__main__':unittest.main()
