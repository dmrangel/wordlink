#!/usr/bin/env python3
"""Draws the web app icons in web/public/icons/ from the launcher design:
a brand-green tile with a "W" traced through five linked letters
(same geometry as AppLogo in HomeScreen.kt and ic_launcher_foreground.xml).

Usage:  pip install pillow
        python tools/gen_web_icons.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "web" / "public" / "icons"
GREEN = (0x1B, 0xD9, 0x6A, 255)
INK = (0, 0, 0, 255)
POINTS = [(0.22, 0.32), (0.36, 0.70), (0.50, 0.42), (0.64, 0.70), (0.78, 0.32)]
SUPERSAMPLE = 4


def draw(size: int, *, rounded: bool, inset: float) -> Image.Image:
    """inset: margin around the logo as a fraction of the icon (maskable icons need a safe zone)."""
    s = size * SUPERSAMPLE
    image = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(image)
    if rounded:
        d.rounded_rectangle((0, 0, s - 1, s - 1), radius=s * 0.28, fill=GREEN)
    else:
        d.rectangle((0, 0, s, s), fill=GREEN)
    span = s * (1 - 2 * inset)
    points = [(s * inset + x * span, s * inset + y * span) for x, y in POINTS]
    stroke = span * 0.075
    d.line(points, fill=INK, width=round(stroke), joint="curve")
    for x, y in points:
        d.ellipse((x - stroke, y - stroke, x + stroke, y + stroke), fill=INK)
    return image.resize((size, size), Image.LANCZOS)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    icons = {
        "icon-192.png": draw(192, rounded=True, inset=0),
        "icon-512.png": draw(512, rounded=True, inset=0),
        # Maskable: full bleed, logo inside the central 80% safe zone.
        "maskable-512.png": draw(512, rounded=False, inset=0.12),
        # iOS rounds the corners itself and shows transparency as black.
        "apple-touch-icon.png": draw(180, rounded=False, inset=0.06),
        "favicon-32.png": draw(32, rounded=True, inset=0),
    }
    for name, image in icons.items():
        image.save(OUT / name, optimize=True)
        print(f"{name}: {(OUT / name).stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
