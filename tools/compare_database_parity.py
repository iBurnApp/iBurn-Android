#!/usr/bin/env python3
"""Compare host and Android-generated read-only database tables canonically."""

from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from pathlib import Path


TABLES = ("arts", "camps", "events", "event_occurrences")


def canonical_table(database: sqlite3.Connection, table: str, columns: list[str]) -> tuple[int, str]:
    selected = [column for column in columns if column != "_id"]
    rows = database.execute(
        f"SELECT {','.join(f'`{column}`' for column in selected)} "
        f"FROM `{table}` ORDER BY p_id"
    ).fetchall()
    encoded = json.dumps(rows, ensure_ascii=False, separators=(",", ":")).encode()
    return len(rows), hashlib.sha256(encoded).hexdigest()


def compare(host_path: Path, device_path: Path) -> dict[str, object]:
    result: dict[str, object] = {"status": "passed", "tables": {}}
    with sqlite3.connect(host_path) as host, sqlite3.connect(device_path) as device:
        for label, database in (("host", host), ("device", device)):
            if database.execute("PRAGMA integrity_check").fetchone()[0] != "ok":
                raise ValueError(f"{label} database integrity_check failed")
            if database.execute("PRAGMA foreign_key_check").fetchall():
                raise ValueError(f"{label} database foreign_key_check failed")

        for table in TABLES:
            host_columns = [
                row[1] for row in host.execute(f"PRAGMA table_info(`{table}`)")
            ]
            device_columns = [
                row[1] for row in device.execute(f"PRAGMA table_info(`{table}`)")
            ]
            if not set(host_columns).issubset(device_columns):
                raise ValueError(f"device {table} schema does not contain host schema")
            host_count, host_hash = canonical_table(host, table, host_columns)
            device_count, device_hash = canonical_table(device, table, host_columns)
            if host_count != device_count or host_hash != device_hash:
                raise ValueError(f"{table} host/device canonical rows differ")
            result["tables"][table] = {
                "rowCount": host_count,
                "canonicalSha256": host_hash,
            }
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", type=Path, required=True)
    parser.add_argument("--device", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = compare(args.host, args.device)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    counts = ", ".join(
        f"{table}={details['rowCount']}"
        for table, details in result["tables"].items()
    )
    print(f"Host/device database parity passed: {counts}")


if __name__ == "__main__":
    main()
