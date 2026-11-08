"""Minimal Hunspell affix expander ("unmunch") for the dictionary build.

Only what build_dictionaries.py needs: expand every entry of a .dic file with
its prefix/suffix rules and return the resulting word forms. Compounding,
morphology fields and other advanced options are ignored.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path


@dataclass
class Rule:
    strip: str
    add: str
    flags: str
    condition: re.Pattern[str]


@dataclass
class Affix:
    prefix: bool
    cross: bool
    rules: list[Rule]


class Hunspell:
    def __init__(self, aff: Path, dic: Path):
        self.flag_mode = "char"
        self.affixes: dict[str, Affix] = {}
        self.need_affix = ""
        self.forbidden = ""
        self._read_aff(aff.read_text(encoding="utf-8-sig", errors="replace").splitlines())
        self.dic_lines = dic.read_text(encoding="utf-8-sig", errors="replace").splitlines()[1:]

    def _split_flags(self, flags: str) -> list[str]:
        if self.flag_mode == "long":
            return [flags[i:i + 2] for i in range(0, len(flags), 2)]
        if self.flag_mode == "num":
            return [f for f in flags.split(",") if f]
        return list(flags)

    def _read_aff(self, lines: list[str]) -> None:
        pending: tuple[str, str, int] | None = None
        for line in lines:
            parts = line.split()
            if not parts or parts[0].startswith("#"):
                continue
            key = parts[0]
            if key == "FLAG" and len(parts) > 1:
                self.flag_mode = {"long": "long", "num": "num"}.get(parts[1], "char")
            elif key == "NEEDAFFIX":
                self.need_affix = parts[1]
            elif key == "FORBIDDENWORD":
                self.forbidden = parts[1]
            elif key in ("PFX", "SFX") and len(parts) >= 4:
                flag = parts[1]
                if pending is None or pending[1] != flag or pending[2] == 0:
                    # header line: PFX flag cross_product count
                    self.affixes[flag] = Affix(key == "PFX", parts[2] == "Y", [])
                    pending = (key, flag, int(parts[3]) if parts[3].isdigit() else 0)
                    continue
                strip = "" if parts[2] == "0" else parts[2]
                add, _, flags = parts[3].partition("/")
                add = "" if add == "0" else add
                condition = parts[4] if len(parts) > 4 else "."
                pattern = ("^" + condition) if key == "PFX" else (condition + "$")
                try:
                    compiled = re.compile(pattern)
                except re.error:
                    compiled = re.compile("^$x")  # never matches
                self.affixes[flag].rules.append(Rule(strip, add, flags, compiled))
                pending = (key, flag, pending[2] - 1)

    def _apply(self, word: str, flags: list[str], depth: int, out: set[str], suffixed: list[str]) -> None:
        for flag in flags:
            affix = self.affixes.get(flag)
            if affix is None or affix.prefix:
                continue
            for rule in affix.rules:
                if not rule.condition.search(word):
                    continue
                if rule.strip and not word.endswith(rule.strip):
                    continue
                form = word[: len(word) - len(rule.strip)] + rule.add
                out.add(form)
                if affix.cross:
                    suffixed.append(form)
                if rule.flags and depth < 2:
                    self._apply(form, self._split_flags(rule.flags), depth + 1, out, suffixed)

    def forms(self, keep=lambda entry: True, max_base_length: int | None = None) -> set[str]:
        """All word forms whose dictionary entry passes `keep`."""
        out: set[str] = set()
        for line in self.dic_lines:
            entry = line.split("\t")[0].split(" ")[0]
            if not entry:
                continue
            word, _, flag_text = entry.partition("/")
            if max_base_length is not None and len(word) > max_base_length:
                continue
            if not keep(word):
                continue
            flags = self._split_flags(flag_text)
            if self.forbidden and self.forbidden in flags:
                continue
            if not (self.need_affix and self.need_affix in flags):
                out.add(word)
            suffixed: list[str] = []
            self._apply(word, flags, 0, out, suffixed)
            for flag in flags:
                affix = self.affixes.get(flag)
                if affix is None or not affix.prefix:
                    continue
                for rule in affix.rules:
                    for base in [word] + (suffixed if affix.cross else []):
                        if rule.condition.search(base) and base.startswith(rule.strip):
                            out.add(rule.add + base[len(rule.strip):])
        return out
