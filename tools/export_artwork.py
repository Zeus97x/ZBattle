#!/usr/bin/env python3
"""Export runtime WebP copies of the ChatGPT artwork drop (CLAUDE-003).

Reads design/artwork/manifest.json, verifies every source PNG against its SHA-256, and writes
region maps and primary location heroes to app/src/main/assets/art/<art_key>.webp.
Variants and reference boards are not exported. Source PNGs are never modified.

Usage: python3 tools/export_artwork.py   (requires Pillow with WebP support)
"""
import hashlib
import io
import json
import pathlib
import sys

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parent.parent
MANIFEST = ROOT / "design/artwork/manifest.json"
OUT_DIR = ROOT / "app/src/main/assets/art"
RECORD = ROOT / "design/artwork/runtime-exports.json"

LONG_SIDE = 1280  # ≥ the largest on-screen use (hero card ≈ 1080px wide at xxhdpi); loader keeps ≥ 768
QUALITY = 82      # chosen by side-by-side 1:1 crops: 75 softens skies, 82 matches the source
KINDS = {"region_map", "location_hero"}


def main() -> int:
    assets = json.loads(MANIFEST.read_text())["assets"]
    records = []
    for asset in assets:
        src = ROOT / asset["path"]
        data = src.read_bytes()
        if hashlib.sha256(data).hexdigest() != asset["sha256"]:
            print(f"checksum mismatch: {asset['path']}", file=sys.stderr)
            return 1
        if asset["kind"] not in KINDS:
            continue
        image = Image.open(io.BytesIO(data)).convert("RGB")
        image.thumbnail((LONG_SIDE, LONG_SIDE), Image.LANCZOS)
        out = OUT_DIR / f"{asset['art_key']}.webp"
        out.parent.mkdir(parents=True, exist_ok=True)
        image.save(out, "WEBP", quality=QUALITY, method=6)
        records.append({
            "art_key": asset["art_key"],
            "source": asset["path"],
            "source_sha256": asset["sha256"],
            "runtime": str(out.relative_to(ROOT)),
            "width": image.width,
            "height": image.height,
            "bytes": out.stat().st_size,
            "quality": QUALITY,
        })
    RECORD.write_text(json.dumps({"long_side": LONG_SIDE, "quality": QUALITY, "exports": records}, indent=2) + "\n")
    total = sum(r["bytes"] for r in records)
    print(f"exported {len(records)} images, {total / 1_048_576:.1f} MiB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
