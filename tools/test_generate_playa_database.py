import json
import sqlite3
import tempfile
import unittest
from pathlib import Path

from generate_playa_database import (
    build_database,
    deduplicate_records,
    populate_missing_coordinates,
)


class GeneratePlayaDatabaseTest(unittest.TestCase):
    def test_deduplicates_identical_records_and_rejects_conflicts(self):
        record = {"uid": "same", "name": "Same"}
        self.assertEqual(
            [record],
            deduplicate_records([record, dict(record)], "fixture.json"),
        )
        with self.assertRaisesRegex(ValueError, "conflicting records"):
            deduplicate_records(
                [record, {"uid": "same", "name": "Different"}],
                "fixture.json",
            )

    def test_fills_missing_coordinates_with_bundled_geocoder(self):
        with tempfile.TemporaryDirectory() as directory:
            geocoder = Path(directory) / "bundle.js"
            geocoder.write_text(
                "window.prepare = () => ({ forward: (_) => "
                "({ geometry: { coordinates: [-119.2, 40.7] } }) });"
            )
            items = [{
                "location_string": "A & 1:00",
                "location": {"gps_latitude": 0, "gps_longitude": 0},
            }]

            populate_missing_coordinates(items, geocoder)

            self.assertEqual(40.7, items[0]["location"]["gps_latitude"])
            self.assertEqual(-119.2, items[0]["location"]["gps_longitude"])

    def test_generates_valid_tables_and_expands_event_occurrences(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            api = root / "api"
            api.mkdir()
            (api / "art.json").write_text(json.dumps([{
                "uid": "art-1", "name": "Art", "artist": "Artist",
                "location": {"gps_latitude": 1.0, "gps_longitude": 2.0},
                "location_string": "Open Playa"
            }]))
            (api / "camp.json").write_text(json.dumps([{
                "uid": "camp-1", "name": "Camp", "hometown": "Home",
                "location": {"gps_latitude": 3.0, "gps_longitude": 4.0},
                "location_string": "A & 1:00"
            }]))
            (api / "event.json").write_text(json.dumps([{
                "uid": "event-1", "title": "Event", "hosted_by_camp": "camp-1",
                "event_type": {"abbr": "prty"}, "occurrence_set": [
                    {"start_time": "2025-08-25T12:00:00-07:00", "end_time": "2025-08-25T13:00:00-07:00"},
                    {"start_time": "2025-08-26T12:00:00-07:00", "end_time": "2025-08-26T13:00:00-07:00"}
                ]
            }]))
            output = root / "playa.db"

            counts = build_database(api, output)

            self.assertEqual({"arts": 1, "camps": 1, "events": 2}, counts)
            with sqlite3.connect(output) as database:
                self.assertEqual("ok", database.execute("PRAGMA integrity_check").fetchone()[0])
                rows = database.execute("SELECT p_id, lat, lon, s_time FROM events ORDER BY p_id").fetchall()
                self.assertEqual([
                    ("event-1-0", 3.0, 4.0, "2025-08-25T12:00:00-0700"),
                    ("event-1-1", 3.0, 4.0, "2025-08-26T12:00:00-0700"),
                ], rows)


if __name__ == "__main__":
    unittest.main()
