import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from verify_annual_update import load_records, verify_mbtiles, verify_media


class VerifyAnnualUpdateTest(unittest.TestCase):
    def test_identical_duplicate_uids_are_counted_but_conflicts_fail(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "records.json"
            path.write_text(json.dumps([
                {"uid": "one", "name": "Same"},
                {"uid": "one", "name": "Same"},
            ]))
            records, duplicate_count = load_records(path)
            self.assertEqual(1, len(records))
            self.assertEqual(1, duplicate_count)

            path.write_text(json.dumps([
                {"uid": "one", "name": "First"},
                {"uid": "one", "name": "Second"},
            ]))
            with self.assertRaisesRegex(ValueError, "conflicting duplicate"):
                load_records(path)

    def test_mbtiles_requires_metadata_and_tiles(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "map.mbtiles"
            with sqlite3.connect(path) as database:
                database.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
                database.execute(
                    "CREATE TABLE tiles "
                    "(zoom_level INTEGER, tile_column INTEGER, "
                    "tile_row INTEGER, tile_data BLOB)"
                )
                database.executemany(
                    "INSERT INTO metadata VALUES (?, ?)",
                    [("name", "Fixture"), ("format", "pbf")],
                )
                database.execute("INSERT INTO tiles VALUES (0, 0, 0, x'00')")

            result = verify_mbtiles(path)
            self.assertEqual(1, result["tileCount"])
            self.assertEqual("pbf", result["format"])

    def test_media_reports_missing_bundled_images(self):
        with tempfile.TemporaryDirectory() as directory:
            assets_root = Path(directory)
            (assets_root / "art_images").mkdir()
            (assets_root / "audio_tour").mkdir()
            (assets_root / "art_images" / "art-1.jpg").write_bytes(b"\xff\xd8\xff")

            result = verify_media(
                assets_root,
                [{"uid": "art-1"}, {"uid": "mv-1"}],
                {"art-1.jpg", "mv-1.jpg"},
            )

            self.assertEqual(1, result["missingReferencedFiles"])


if __name__ == "__main__":
    unittest.main()
