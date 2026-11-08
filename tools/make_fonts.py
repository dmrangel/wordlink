#!/usr/bin/env python3
"""Creates the bundled Inter font files in app/src/main/res/font/ (or, with --web,
the single variable woff2 in web/public/fonts/).

Takes the official Inter variable font (SIL Open Font License 1.1), pins the
weights the Modrinth design system uses (400/500/600/700/800) and subsets each
instance to Latin + Latin-1, which covers English and Brazilian Portuguese.
Each file ends up around 60 KB instead of ~880 KB for the full variable font.

Usage:  pip install fonttools brotli
        python tools/make_fonts.py [--source Inter.ttf] [--web]
"""

from __future__ import annotations

import argparse
import urllib.request
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "app" / "src" / "main" / "res" / "font"
WEB_OUT = ROOT / "web" / "public" / "fonts" / "inter-latin.woff2"
SOURCE_URL = "https://raw.githubusercontent.com/google/fonts/main/ofl/inter/Inter%5Bopsz,wght%5D.ttf"

WEIGHTS = {
    "inter_regular": 400,
    "inter_medium": 500,
    "inter_semibold": 600,
    "inter_bold": 700,
    "inter_extrabold": 800,
}

# Google Fonts "latin" range: ASCII, Latin-1, common punctuation and symbols.
UNICODES = (
    list(range(0x20, 0x7F))
    + list(range(0xA0, 0x100))
    + [0x131, 0x152, 0x153, 0x2BB, 0x2BC, 0x2C6, 0x2DA, 0x2DC, 0x2074, 0x20AC, 0x2122, 0x2191, 0x2193, 0x2212, 0x2215]
    + list(range(0x2000, 0x2070))
)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--source", type=Path, default=Path(__file__).resolve().parent / ".cache" / "Inter.ttf")
    parser.add_argument("--web", action="store_true", help="only write the web font (weights 400-800 in one variable woff2)")
    args = parser.parse_args()

    if not args.source.exists():
        args.source.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(SOURCE_URL) as response:
            args.source.write_bytes(response.read())

    OUT.mkdir(parents=True, exist_ok=True)
    options = subset.Options()
    options.layout_features = ["kern", "liga", "calt", "tnum", "case", "ccmp", "locl", "mark", "mkmk"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    options.hinting = False

    if args.web:
        # Subset before instancing: the instancer can leave lazy tables the subsetter trips over.
        font = TTFont(args.source, lazy=False)
        subsetter = subset.Subsetter(options)
        subsetter.populate(unicodes=UNICODES)
        subsetter.subset(font)
        font = instancer.instantiateVariableFont(font, {"wght": (400, 800), "opsz": 14})
        font.flavor = "woff2"
        WEB_OUT.parent.mkdir(parents=True, exist_ok=True)
        font.save(WEB_OUT)
        print(f"{WEB_OUT.relative_to(ROOT)}: {WEB_OUT.stat().st_size // 1024} KB")
        return

    for name, weight in WEIGHTS.items():
        font = TTFont(args.source)
        font = instancer.instantiateVariableFont(font, {"wght": weight, "opsz": 14})
        subsetter = subset.Subsetter(options)
        subsetter.populate(unicodes=UNICODES)
        subsetter.subset(font)
        target = OUT / f"{name}.ttf"
        font.save(target)
        print(f"{target.name}: {target.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
