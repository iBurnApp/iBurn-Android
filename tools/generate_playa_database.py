#!/usr/bin/env python3
"""Build the read-only playa tables from iBurn-Data without an Android device."""

from __future__ import annotations

import argparse
import json
import os
import sqlite3
import subprocess
import tempfile
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path


BASE_COLUMNS = """
`_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
`name` TEXT, `desc` TEXT, `url` TEXT, `contact` TEXT,
`p_addr` TEXT, `p_addr_unof` TEXT, `p_id` TEXT,
`lat` REAL NOT NULL, `lon` REAL NOT NULL,
`lat_unof` REAL NOT NULL, `lon_unof` REAL NOT NULL
"""


def load_array(path: Path) -> list[dict]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, list):
        raise ValueError(f"{path} must contain a JSON array")
    return value


def deduplicate_records(items: list[dict], source_name: str) -> list[dict]:
    """Collapse byte-equivalent duplicate UIDs and reject conflicting records."""
    records: dict[str, dict] = {}
    for item in items:
        uid = item.get("uid")
        if not isinstance(uid, str) or not uid:
            raise ValueError(f"{source_name} contains a record without a uid")
        previous = records.get(uid)
        if previous is not None and previous != item:
            raise ValueError(f"{source_name} contains conflicting records for one uid")
        records[uid] = item
    return list(records.values())


def location_values(item: dict) -> tuple[object, ...]:
    official = item.get("location") or {}
    unofficial = item.get("burnermap_location") or {}
    unofficial_address = unofficial.get("location_string")
    if unofficial and not unofficial_address:
        unofficial_address = " ".join(
            str(unofficial.get(key) or "")
            for key in ("frontage", "intersection_type", "intersection")
        ).strip() or None
    return (
        item.get("location_string"),
        unofficial_address,
        official.get("gps_latitude", 0) or 0,
        official.get("gps_longitude", 0) or 0,
        unofficial.get("gps_latitude", 0) or 0,
        unofficial.get("gps_longitude", 0) or 0,
    )


def base_values(item: dict) -> tuple[object, ...]:
    address, unofficial_address, lat, lon, lat_unofficial, lon_unofficial = location_values(item)
    return (
        item.get("name") or item.get("title") or "?",
        item.get("description"),
        item.get("url"),
        item.get("contact_email"),
        address,
        unofficial_address,
        item.get("uid"),
        lat,
        lon,
        lat_unofficial,
        lon_unofficial,
    )


def iso_and_pretty(value: str, all_day: bool) -> tuple[str, str]:
    parsed = datetime.fromisoformat(value)
    # Match PlayaDateTypeAdapter's yyyy-MM-dd'T'HH:mm:ssZ output exactly.
    iso_value = parsed.strftime("%Y-%m-%dT%H:%M:%S%z")
    day = parsed.strftime("%a %-m/%-d")
    pretty = day if all_day else f"{day} {parsed.strftime('%-I:%M %p')}"
    return iso_value, pretty


def populate_missing_coordinates(items: list[dict], geocoder: Path | None) -> None:
    pending = [
        item for item in items
        if item.get("location") and item.get("location_string")
        and not item["location"].get("gps_latitude")
        and not item["location"].get("gps_longitude")
    ]
    if not pending:
        return
    if geocoder is None:
        return
    node_script = r"""
const fs = require('fs');
global.window = global;
eval(fs.readFileSync(process.argv[1], 'utf8'));
const coder = window.prepare();
const addresses = JSON.parse(fs.readFileSync(0, 'utf8'));
process.stdout.write(JSON.stringify(addresses.map(address => {
  const result = coder.forward(address);
  return result && result.geometry ? result.geometry.coordinates : null;
})));
"""
    result = subprocess.run(
        ["node", "-e", node_script, str(geocoder)],
        input=json.dumps([item["location_string"] for item in pending]),
        capture_output=True,
        check=True,
        text=True,
    )
    coordinates = json.loads(result.stdout)
    if len(pending) != len(coordinates):
        raise ValueError("geocoder returned an unexpected number of results")
    for item, coordinate in zip(pending, coordinates):
        if coordinate:
            item["location"]["gps_longitude"] = coordinate[0]
            item["location"]["gps_latitude"] = coordinate[1]


def build_database(api_root: Path, output: Path, geocoder: Path | None = None) -> dict[str, int]:
    art = deduplicate_records(load_array(api_root / "art.json"), "art.json")
    camps = deduplicate_records(load_array(api_root / "camp.json"), "camp.json")
    events = deduplicate_records(load_array(api_root / "event.json"), "event.json")
    populate_missing_coordinates(art + camps, geocoder)
    locations = {
        item["uid"]: location_values(item)
        for item in art + camps
        if item.get("uid")
    }

    output.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary_name = tempfile.mkstemp(prefix=f".{output.name}.", dir=output.parent)
    os.close(fd)
    temporary = Path(temporary_name)
    try:
        connection = sqlite3.connect(temporary)
        with connection:
            connection.execute(f"CREATE TABLE arts (`artist` TEXT, `a_loc` TEXT, `i_url` TEXT, {BASE_COLUMNS})")
            connection.execute(f"CREATE TABLE camps (`hometown` TEXT, {BASE_COLUMNS})")
            connection.execute(
                f"CREATE TABLE events (`e_type` TEXT, `all_day` INTEGER NOT NULL, "
                f"`check_loc` INTEGER NOT NULL, `c_id` TEXT, `a_id` TEXT, "
                f"`s_time` TEXT, `s_time_p` TEXT, `e_time` TEXT, `e_time_p` TEXT, {BASE_COLUMNS})"
            )
            for item in art:
                images = item.get("images") or []
                image_url = images[0].get("thumbnail_url") if images else None
                connection.execute(
                    "INSERT INTO arts (artist,a_loc,i_url,name,desc,url,contact,p_addr,p_addr_unof,p_id,lat,lon,lat_unof,lon_unof) "
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    (item.get("artist"), item.get("artist_location"), image_url, *base_values(item)),
                )
            for item in camps:
                connection.execute(
                    "INSERT INTO camps (hometown,name,desc,url,contact,p_addr,p_addr_unof,p_id,lat,lon,lat_unof,lon_unof) "
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    (item.get("hometown"), *base_values(item)),
                )
            event_rows = 0
            for item in events:
                occurrences = sorted(item.get("occurrence_set") or [], key=lambda occurrence: occurrence.get("start_time") or "")
                host_id = item.get("hosted_by_camp") or item.get("located_at_art")
                event_item = dict(item)
                if host_id in locations:
                    address, unofficial_address, lat, lon, lat_u, lon_u = locations[host_id]
                    event_item["location_string"] = address
                    event_item["location"] = {"gps_latitude": lat, "gps_longitude": lon}
                    event_item["burnermap_location"] = {
                        "location_string": unofficial_address,
                        "gps_latitude": lat_u,
                        "gps_longitude": lon_u,
                    }
                for index, occurrence in enumerate(occurrences):
                    start = datetime.fromisoformat(occurrence["start_time"])
                    end = datetime.fromisoformat(occurrence["end_time"])
                    if end < start:
                        end = end.replace(year=start.year, month=start.month, day=start.day)
                        if end <= start:
                            end += timedelta(days=1)
                    start_iso, start_pretty = iso_and_pretty(start.isoformat(), bool(item.get("all_day")))
                    end_iso, end_pretty = iso_and_pretty(end.isoformat(), bool(item.get("all_day")))
                    values = list(base_values(event_item))
                    values[6] = f"{item['uid']}-{index}"
                    connection.execute(
                        "INSERT INTO events (e_type,all_day,check_loc,c_id,a_id,s_time,s_time_p,e_time,e_time_p,"
                        "name,desc,url,contact,p_addr,p_addr_unof,p_id,lat,lon,lat_unof,lon_unof) "
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        (
                            (item.get("event_type") or {}).get("abbr", "unknwn"),
                            int(bool(item.get("all_day"))), int(bool(item.get("check_location"))),
                            item.get("hosted_by_camp"), item.get("located_at_art"),
                            start_iso, start_pretty, end_iso, end_pretty, *values,
                        ),
                    )
                    event_rows += 1
            connection.execute("PRAGMA user_version = 2")
            expected_ids = {
                "arts": Counter(item["uid"] for item in art),
                "camps": Counter(item["uid"] for item in camps),
                "events": Counter(
                    f"{item['uid']}-{index}"
                    for item in events
                    for index, _ in enumerate(sorted(
                        item.get("occurrence_set") or [],
                        key=lambda occurrence: occurrence.get("start_time") or "",
                    ))
                ),
            }
            for table, expected in expected_ids.items():
                actual = Counter(row[0] for row in connection.execute(f"SELECT p_id FROM {table}"))
                if actual != expected:
                    raise ValueError(f"{table} stable identifiers do not match source JSON")
            integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
            foreign_keys = connection.execute("PRAGMA foreign_key_check").fetchall()
            if integrity != "ok" or foreign_keys:
                raise ValueError(f"database validation failed: integrity={integrity}, foreignKeys={foreign_keys}")
        connection.close()
        os.replace(temporary, output)
        return {"arts": len(art), "camps": len(camps), "events": event_rows}
    finally:
        temporary.unlink(missing_ok=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--api-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--geocoder", type=Path)
    args = parser.parse_args()
    counts = build_database(args.api_root, args.output, args.geocoder)
    print("Generated playa database: " + ", ".join(f"{key}={value}" for key, value in counts.items()))


if __name__ == "__main__":
    main()
