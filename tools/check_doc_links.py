#!/usr/bin/env python3
"""Check that relative Markdown links in ai/ and docs/ point at files that exist."""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
LINK = re.compile(r"\]\(([^)\s]+)\)")
bad = []
for md in sorted(list((ROOT / "ai").rglob("*.md")) + list((ROOT / "docs").rglob("*.md")) + [ROOT / "README.md"]):
    for target in LINK.findall(md.read_text()):
        if re.match(r"^(https?:|mailto:|#)", target):
            continue
        path = (md.parent / target.split("#")[0]).resolve()
        if not path.exists():
            bad.append(f"{md.relative_to(ROOT)} -> {target}")
if bad:
    print("Broken links:")
    print("\n".join(" - " + b for b in bad))
    sys.exit(1)
print("doc links OK")
