#!/usr/bin/env python3
"""Synthesizes the game's sound effects into app/src/main/res/raw/.

Everything is generated from sine partials and filtered noise, so there are no
third-party audio assets to license and the files stay tiny (mono, 22 kHz).

Usage:  python tools/make_sounds.py
"""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"
RATE = 22050


def note(freq: float, length: float, decay: float, partials=((1, 1.0), (2, 0.35), (3, 0.12)), attack=0.004):
    samples = []
    for i in range(int(length * RATE)):
        t = i / RATE
        env = min(1.0, t / attack) * math.exp(-t / decay)
        value = sum(amp * math.sin(2 * math.pi * freq * mult * t) for mult, amp in partials)
        samples.append(env * value)
    return samples


def mix(*tracks):
    """Each track is (start_seconds, samples, gain)."""
    end = max(int(start * RATE) + len(samples) for start, samples, _ in tracks)
    out = [0.0] * end
    for start, samples, gain in tracks:
        offset = int(start * RATE)
        for i, s in enumerate(samples):
            out[offset + i] += s * gain
    return out


def save(name: str, samples, peak=0.55):
    top = max(abs(s) for s in samples) or 1.0
    fade = int(0.01 * RATE)
    frames = bytearray()
    for i, s in enumerate(samples):
        if i > len(samples) - fade:
            s *= (len(samples) - i) / fade
        frames += struct.pack("<h", int(max(-1.0, min(1.0, s / top * peak)) * 32767))
    OUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(RATE)
        f.writeframes(bytes(frames))
    print(f"{name}.wav: {len(frames) // 1024} KB")


def sweep(start: float, end: float, length: float, decay: float):
    samples, phase = [], 0.0
    for i in range(int(length * RATE)):
        t = i / RATE
        freq = start * (end / start) ** (t / length)
        phase += 2 * math.pi * freq / RATE
        samples.append(min(1.0, t / 0.003) * math.exp(-t / decay) * math.sin(phase))
    return samples


def swish(length: float):
    rng = random.Random(7)
    samples, low, band = [], 0.0, 0.0
    for i in range(int(length * RATE)):
        t = i / RATE
        cutoff = 0.05 + 0.25 * math.sin(math.pi * t / length)
        low += cutoff * (rng.uniform(-1, 1) - low)
        band += 0.5 * (low - band)
        samples.append((low - band) * math.sin(math.pi * t / length) ** 2)
    return samples


C5, E5, G5, A5, C6, E6, G6 = 523.25, 659.25, 783.99, 880.0, 1046.5, 1318.5, 1568.0

if __name__ == "__main__":
    # Played with a rising playback rate for each letter added to the word.
    save("tap", note(C5, 0.16, 0.05, ((1, 1.0), (2, 0.2), (4, 0.05))), peak=0.45)
    save("word", mix((0, note(E5, 0.5, 0.14), 1.0), (0.08, note(A5, 0.55, 0.18), 1.0)))
    save("bonus", mix((0, note(G5, 0.3, 0.07), 0.8), (0.06, note(C6, 0.3, 0.08), 0.8), (0.12, note(E6, 0.4, 0.12), 0.9)), peak=0.45)
    save("error", sweep(210, 120, 0.2, 0.07), peak=0.5)
    save("shuffle", swish(0.22), peak=0.35)
    save("complete", mix(
        (0.00, note(C5, 0.6, 0.2), 0.9),
        (0.10, note(E5, 0.6, 0.2), 0.9),
        (0.20, note(G5, 0.7, 0.25), 0.9),
        (0.32, note(C6, 1.0, 0.4), 1.0),
        (0.32, note(G6, 1.0, 0.3), 0.25),
    ))
