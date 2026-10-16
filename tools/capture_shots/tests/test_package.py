from pathlib import Path
import tempfile
import unittest
from tools.capture_shots.package_tools import build_package,PACKAGE_FILES

class PackageTest(unittest.TestCase):
    def test_collection_kit_preserves_only_explicit_source_document_and_synthetic_fixture_paths(self):
        with tempfile.TemporaryDirectory() as temp:
            output=Path(temp)/'kit';build_package(output)
            actual={p.relative_to(output).as_posix() for p in output.rglob('*') if p.is_file()}
            self.assertEqual(set(PACKAGE_FILES),actual)
            self.assertFalse(any('__pycache__' in p or '.devAuth' in p or p.startswith('run/') for p in actual))
            with self.assertRaises(ValueError):build_package(output)

if __name__=='__main__':unittest.main()
