#!/usr/bin/env python3
"""Create a deterministic inventory of annual Android asset outputs."""

from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
from collections import Counter
from pathlib import Path
from typing import Iterable


SCHEMA_VERSION = 2
TOOL_VERSION = 1
ASSET_GROUPS = {
    "geocoder": "js",
    "map": "map",
    "artImages": "art_images",
    "audioTour": "audio_tour",
    "apiJson": "json",
    "databases": "databases",
}


def _files(root: Path) -> Iterable[Path]:
    if not root.is_dir():
        return ()
    return sorted(
        (path for path in root.rglob("*") if path.is_file()),
        key=lambda path: path.relative_to(root).as_posix(),
    )


def inventory(root: Path) -> dict[str, object]:
    """Return counts, total bytes, suffix counts, and a reproducible tree hash."""
    digest = hashlib.sha256()
    suffixes: Counter[str] = Counter()
    count = 0
    total_bytes = 0

    files = []
    for path in _files(root):
        relative_path = path.relative_to(root).as_posix()
        content = path.read_bytes()
        file_hash = hashlib.sha256(content).hexdigest()
        size = len(content)

        # Length prefixes make the stream unambiguous without depending on a
        # platform-specific path separator or line ending.
        encoded_path = relative_path.encode("utf-8")
        digest.update(len(encoded_path).to_bytes(8, "big"))
        digest.update(encoded_path)
        digest.update(size.to_bytes(8, "big"))
        digest.update(bytes.fromhex(file_hash))

        suffixes[path.suffix.lower() or "<none>"] += 1
        count += 1
        total_bytes += size
        files.append({
            "path": relative_path,
            "size": size,
            "sha256": file_hash,
        })

    return {
        "fileCount": count,
        "totalBytes": total_bytes,
        "sha256": digest.hexdigest(),
        "extensions": dict(sorted(suffixes.items())),
        "files": files,
    }


def build_manifest(
    assets_root: Path,
    year: int,
    data_revision: str,
    annual_config: Path | None = None,
) -> dict[str, object]:
    manifest = {
        "schemaVersion": SCHEMA_VERSION,
        "toolVersion": TOOL_VERSION,
        "year": year,
        "dataRevision": data_revision,
        "assetGroups": {
            name: inventory(assets_root / relative_path)
            for name, relative_path in ASSET_GROUPS.items()
        },
    }
    if annual_config:
        manifest["annualConfigSha256"] = hashlib.sha256(
            annual_config.read_bytes()
        ).hexdigest()
    return manifest


def resolve_data_revision(repo_root: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo_root / "iBurn-Data"), "rev-parse", "HEAD"],
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    parser.add_argument("--assets-root", type=Path)
    parser.add_argument("--year", type=int, required=True)
    parser.add_argument("--data-revision")
    parser.add_argument("--annual-config", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    repo_root = args.repo_root.resolve()
    assets_root = (args.assets_root or repo_root / "iBurn/src/main/assets").resolve()
    revision = args.data_revision or resolve_data_revision(repo_root)
    manifest = build_manifest(
        assets_root, args.year, revision, args.annual_config
    )

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
