import sqlite3
import tempfile
import unittest
from pathlib import Path

from compare_database_parity import compare


def create_database(path: Path, value: str) -> None:
    with sqlite3.connect(path) as database:
        for table in ("arts", "camps", "events", "event_occurrences"):
            database.execute(
                f"CREATE TABLE {table} "
                "(_id INTEGER PRIMARY KEY, p_id TEXT, name TEXT)"
            )
            database.execute(
                f"INSERT INTO {table} (p_id, name) VALUES (?, ?)",
                (f"{table}-1", value),
            )


class CompareDatabaseParityTest(unittest.TestCase):
    def test_matches_canonical_rows_and_detects_difference(self):
        with tempfile.TemporaryDirectory() as directory:
            host = Path(directory) / "host.db"
            device = Path(directory) / "device.db"
            create_database(host, "same")
            create_database(device, "same")
            self.assertEqual("passed", compare(host, device)["status"])

            with sqlite3.connect(device) as database:
                database.execute("UPDATE events SET name = 'different'")
            with self.assertRaisesRegex(ValueError, "canonical rows differ"):
                compare(host, device)


if __name__ == "__main__":
    unittest.main()
