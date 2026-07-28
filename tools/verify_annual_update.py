#!/usr/bin/env python3
"""Validate synchronized annual assets and emit a concise, non-sensitive report."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
import subprocess
from collections import Counter
from pathlib import Path
from typing import Iterable


READ_ONLY_SCHEMA = {
    "arts": [
        "_id", "artist", "a_loc", "i_url", "name", "desc", "url", "contact",
        "p_addr", "p_addr_unof", "p_id", "lat", "lon", "lat_unof", "lon_unof",
    ],
    "camps": [
        "_id", "hometown", "name", "desc", "url", "contact", "p_addr",
        "p_addr_unof", "p_id", "lat", "lon", "lat_unof", "lon_unof",
    ],
    "events": [
        "_id", "e_type", "all_day", "check_loc", "c_id", "a_id",
        "name", "desc", "url", "contact",
        "p_addr", "p_addr_unof", "p_id", "lat", "lon", "lat_unof", "lon_unof",
    ],
    "event_occurrences": [
        "_id", "event_id", "p_id", "s_time", "e_time",
    ],
}
MEDIA_SUFFIXES = {".jpg", ".jpeg", ".png", ".webp", ".m4a"}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_records(path: Path) -> tuple[list[dict], int]:
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, list):
        raise ValueError(f"{path.name} must contain an array")
    by_uid: dict[str, dict] = {}
    duplicates = 0
    for item in value:
        if not isinstance(item, dict):
            raise ValueError(f"{path.name} contains a non-object record")
        uid = item.get("uid")
        if not isinstance(uid, str) or not uid:
            raise ValueError(f"{path.name} contains a record without a uid")
        previous = by_uid.get(uid)
        if previous is not None:
            if previous != item:
                raise ValueError(f"{path.name} contains conflicting duplicate uids")
            duplicates += 1
        by_uid[uid] = item
    return list(by_uid.values()), duplicates


def verify_database(
    database_path: Path,
    art: list[dict],
    camps: list[dict],
    events: list[dict],
) -> dict[str, object]:
    expected_counts = {
        "arts": len(art),
        "camps": len(camps),
        "events": len(events),
        "event_occurrences": sum(
            len(item.get("occurrence_set") or []) for item in events
        ),
    }
    with sqlite3.connect(f"file:{database_path}?mode=ro", uri=True) as database:
        integrity = database.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise ValueError("bundled database integrity_check failed")
        if database.execute("PRAGMA foreign_key_check").fetchall():
            raise ValueError("bundled database foreign_key_check failed")
        actual_counts = {}
        for table, expected_columns in READ_ONLY_SCHEMA.items():
            columns = [
                row[1] for row in database.execute(f"PRAGMA table_info(`{table}`)")
            ]
            if Counter(columns) != Counter(expected_columns):
                raise ValueError(f"bundled database {table} schema mismatch")
            count = database.execute(f"SELECT COUNT(*) FROM `{table}`").fetchone()[0]
            unique = database.execute(
                f"SELECT COUNT(DISTINCT p_id) FROM `{table}`"
            ).fetchone()[0]
            if count != unique:
                raise ValueError(f"bundled database {table} has duplicate stable ids")
            actual_counts[table] = count
        if actual_counts != expected_counts:
            raise ValueError("bundled database row counts do not match source JSON")
    return {
        "fileBytes": database_path.stat().st_size,
        "sha256": sha256(database_path),
        "rowCounts": actual_counts,
    }


def verify_mbtiles(path: Path) -> dict[str, object]:
    with sqlite3.connect(f"file:{path}?mode=ro", uri=True) as database:
        integrity = database.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise ValueError("MBTiles integrity_check failed")
        tables = {
            row[0] for row in database.execute(
                "SELECT name FROM sqlite_master WHERE type IN ('table', 'view')"
            )
        }
        if not {"metadata", "tiles"}.issubset(tables):
            raise ValueError("MBTiles is missing metadata or tiles")
        metadata = dict(database.execute("SELECT name, value FROM metadata"))
        missing_metadata = {"name", "format"} - metadata.keys()
        if missing_metadata:
            raise ValueError(f"MBTiles missing metadata keys {sorted(missing_metadata)}")
        tile_count = database.execute("SELECT COUNT(*) FROM tiles").fetchone()[0]
        if tile_count <= 0:
            raise ValueError("MBTiles contains no tiles")
    return {
        "fileBytes": path.stat().st_size,
        "sha256": sha256(path),
        "tileCount": tile_count,
        "format": metadata["format"],
    }


def nested_strings(value: object) -> Iterable[str]:
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from nested_strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from nested_strings(child)


def verify_styles(map_root: Path) -> dict[str, object]:
    style_paths = sorted(map_root.glob("iburn-*.json"))
    if len(style_paths) != 2:
        raise ValueError("expected exactly two rewritten map styles")
    references = 0
    for style_path in style_paths:
        value = json.loads(style_path.read_text(encoding="utf-8"))
        text = style_path.read_text(encoding="utf-8")
        if "iBurnData_iBurn" in text or "{{mbtiles_path}}" in text:
            raise ValueError(f"{style_path.name} contains an unresolved annual reference")
        for string in nested_strings(value):
            if not string.startswith("asset://"):
                continue
            references += 1
            relative = string.removeprefix("asset://").removeprefix("map/")
            if "{" in relative:
                parent = (map_root / relative).parent
                if not parent.is_dir():
                    raise ValueError(f"{style_path.name} references a missing asset family")
            elif not (map_root / relative).is_file():
                raise ValueError(f"{style_path.name} references a missing local asset")
    return {"styleCount": len(style_paths), "localReferenceCount": references}


def verify_media(
    assets_root: Path,
    source_records: Iterable[dict],
    expected_art_images: set[str],
) -> dict[str, object]:
    media = sorted(
        path
        for directory in ("art_images", "audio_tour")
        for path in (assets_root / directory).glob("*")
        if path.is_file()
    )
    hashes: Counter[str] = Counter()
    format_mismatches = 0
    for path in media:
        header = path.read_bytes()[:32]
        suffix = path.suffix.lower()
        detected = (
            "jpeg" if header.startswith(b"\xff\xd8\xff")
            else "png" if header.startswith(b"\x89PNG\r\n\x1a\n")
            else "webp" if header[:4] == b"RIFF" and header[8:12] == b"WEBP"
            else "m4a" if b"ftyp" in header
            else None
        )
        if detected is None:
            raise ValueError(f"invalid media header for {path.name}")
        expected = {
            ".jpg": "jpeg",
            ".jpeg": "jpeg",
            ".png": "png",
            ".webp": "webp",
            ".m4a": "m4a",
        }[suffix]
        format_mismatches += detected != expected
        hashes[sha256(path)] += 1

    referenced_names = {
        Path(string.split("?", 1)[0]).name
        for record in source_records
        for string in nested_strings(record)
        if Path(string.split("?", 1)[0]).suffix.lower() in MEDIA_SUFFIXES
    }
    referenced_names.update(expected_art_images)
    orphan_count = sum(path.name not in referenced_names for path in media)
    available_names = {path.name for path in media}
    missing_count = len(expected_art_images - available_names)
    duplicate_count = sum(count - 1 for count in hashes.values() if count > 1)
    return {
        "fileCount": len(media),
        "totalBytes": sum(path.stat().st_size for path in media),
        "duplicateContentFiles": duplicate_count,
        "formatExtensionMismatches": format_mismatches,
        "missingReferencedFiles": missing_count,
        "unreferencedFiles": orphan_count,
    }


def verify_embargo_assets_not_tracked(repo_root: Path) -> None:
    tracked = subprocess.run(
        ["git", "ls-files"],
        cwd=repo_root,
        check=True,
        capture_output=True,
        text=True,
    ).stdout.splitlines()
    restricted = (
        "iBurn/src/main/assets/json/",
        "iBurn/src/main/assets/databases/",
        "iBurn/src/main/assets/art_images/",
        "iBurn/src/main/assets/audio_tour/",
        "iBurn/src/main/java/com/gaiagps/iburn/SECRETS.",
    )
    leaked = [path for path in tracked if path.startswith(restricted)]
    if leaked:
        raise ValueError("restricted annual assets or secrets are tracked by Git")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--annual-config", type=Path, required=True)
    parser.add_argument("--data-revision", required=True)
    parser.add_argument("--database", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    repo_root = args.repo_root.resolve()
    config = json.loads(args.annual_config.read_text(encoding="utf-8"))
    year = config["year"]
    assets_root = repo_root / "iBurn/src/main/assets"
    api_root = assets_root / "json"

    art, art_duplicates = load_records(api_root / "art.json")
    camps, camp_duplicates = load_records(api_root / "camp.json")
    events, event_duplicates = load_records(api_root / "event.json")
    source_duplicates = art_duplicates + camp_duplicates + event_duplicates

    database = verify_database(args.database, art, camps, events)
    mbtiles = verify_mbtiles(assets_root / "map/map.mbtiles")
    styles = verify_styles(assets_root / "map")
    expected_art_images = {
        f"{item['uid']}.jpg" for item in art if item.get("images")
    }
    media = verify_media(
        assets_root,
        [*art, *camps, *events],
        expected_art_images,
    )
    verify_embargo_assets_not_tracked(repo_root)

    manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    if manifest["dataRevision"] != args.data_revision or manifest["year"] != year:
        raise ValueError("annual manifest metadata does not match the selected inputs")

    warnings = []
    if source_duplicates:
        warnings.append(
            f"collapsed {source_duplicates} byte-identical duplicate source records"
        )
    if media["duplicateContentFiles"]:
        warnings.append("media bundle contains duplicate file content")
    if media["formatExtensionMismatches"]:
        warnings.append("media bundle contains decodable files with mismatched extensions")
    if media["unreferencedFiles"]:
        warnings.append("media bundle contains files not referenced by synchronized JSON")
    if media["missingReferencedFiles"]:
        warnings.append("some referenced art images are absent and require network fallback")

    report = {
        "schemaVersion": 1,
        "status": "passed",
        "year": year,
        "versionCode": config["versionCode"],
        "versionName": config["versionName"],
        "dataRevision": args.data_revision,
        "sourceRecords": {
            "art": len(art),
            "camps": len(camps),
            "events": len(events),
            "identicalDuplicatesCollapsed": source_duplicates,
        },
        "database": database,
        "mbtiles": mbtiles,
        "styles": styles,
        "media": media,
        "manifestSha256": sha256(args.manifest),
        "warnings": warnings,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(
        "Annual verification passed: "
        f"art={len(art)}, camps={len(camps)}, events={len(events)}, "
        f"databaseRows={sum(database['rowCounts'].values())}, "
        f"media={media['fileCount']}"
    )


if __name__ == "__main__":
    main()
