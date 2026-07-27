import json
import tempfile
import unittest
from pathlib import Path

from annual_manifest import ASSET_GROUPS, build_manifest, inventory


FIXTURE_ROOT = Path(__file__).parent / "testdata/annual_manifest/assets"


class AnnualManifestTest(unittest.TestCase):
    def test_inventory_is_deterministic_and_characterizes_fixture(self):
        expected = {
            "fileCount": 2,
            "totalBytes": 11,
            "sha256": "40a6f22031032d2cf3331f871328e64d9ac3049643fcc5ad29567cd9b5f05efe",
            "extensions": {".json": 1, ".txt": 1},
        }

        self.assertEqual(expected, inventory(FIXTURE_ROOT / "map"))
        self.assertEqual(expected, inventory(FIXTURE_ROOT / "map"))

    def test_manifest_includes_empty_or_missing_asset_groups(self):
        manifest = build_manifest(FIXTURE_ROOT, 2025, "abc123")

        self.assertEqual(1, manifest["schemaVersion"])
        self.assertEqual(2025, manifest["year"])
        self.assertEqual("abc123", manifest["dataRevision"])
        self.assertEqual(set(ASSET_GROUPS), set(manifest["assetGroups"]))
        self.assertEqual(0, manifest["assetGroups"]["databases"]["fileCount"])

    def test_json_serialization_is_byte_stable(self):
        manifest = build_manifest(FIXTURE_ROOT, 2025, "abc123")
        encoded = json.dumps(manifest, indent=2, sort_keys=True) + "\n"

        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / "first.json"
            second = Path(directory) / "second.json"
            first.write_text(encoded, encoding="utf-8")
            second.write_text(encoded, encoding="utf-8")
            self.assertEqual(first.read_bytes(), second.read_bytes())


if __name__ == "__main__":
    unittest.main()
