#!/usr/bin/env python3
# João Pedro G M Silva - PUC Goiás ADS - 20251012000740
"""Valida a consistência do roadmap docs/plans/2026-10-01-redesign-neobrutalista.md.

Verifica, sem dependências externas:
- links locais e âncoras do documento;
- IDs de tarefas NB-xx únicos, ordenados e com dependências anteriores/acyclic;
- fases com estimativas e total coerente;
- aliases de arquivo declarados;
- whitespace/final de linha.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / "docs" / "plans" / "2026-10-01-redesign-neobrutalista.md"

ERRORS: list[str] = []
CHECKS = 0


def check(ok: bool, message: str) -> None:
    global CHECKS
    CHECKS += 1
    if not ok:
        ERRORS.append(message)


def main() -> int:
    text = PLAN.read_text(encoding="utf-8")
    lines = text.splitlines()

    # Whitespace / EOF newline.
    check(text.endswith("\n"), "arquivo sem newline final")
    for i, line in enumerate(lines, start=1):
        check(line == line.rstrip(), f"linha {i}: whitespace no fim")
        check("\t" not in line, f"linha {i}: tabulação")

    # Local links: [label](target) with relative target.
    for match in re.finditer(r"\[([^\]]+)\]\(([^)]+)\)", text):
        target = match.group(2)
        if target.startswith(("http://", "https://", "#")):
            continue
        path_part, _, anchor = target.partition("#")
        resolved = (PLAN.parent / path_part).resolve()
        check(resolved.exists(), f"link local quebrado: {target}")
        if anchor and resolved.exists():
            check(anchor in resolved.read_text(encoding="utf-8"), f"âncora ausente em {target}")

    # Task IDs unique and strictly increasing.
    task_ids = re.findall(r"^#### (NB-\d+) ", text, flags=re.MULTILINE)
    check(len(task_ids) >= 30, f"poucas tarefas NB: {len(task_ids)}")
    check(len(task_ids) == len(set(task_ids)), "IDs NB duplicados")
    numbers = [int(t.split("-")[1]) for t in task_ids]
    check(numbers == sorted(numbers), "IDs NB fora de ordem")
    check(numbers == list(range(numbers[0], numbers[0] + len(numbers))), "IDs NB não contíguos")

    # Dependencies: each "Depende de" references earlier tasks or phase/F aliases.
    for block in re.split(r"^#### ", text, flags=re.MULTILINE)[1:]:
        header = block.splitlines()[0]
        task = header.split()[0]
        dep_line = re.search(r"\*\*Depende de:\*\* ([^\n]+)", block)
        check(dep_line is not None, f"{task}: sem linha 'Depende de'")
        if not dep_line:
            continue
        deps = dep_line.group(1)
        for dep in re.findall(r"NB-(\d+)", deps):
            check(int(dep) < int(task.split("-")[1]), f"{task}: depende de NB-{dep} (futura ou igual)")
        check("NB-" not in deps or re.search(r"NB-\d+", deps) is not None, f"{task}: dependência malformada")
        check("Refs:" in block, f"{task}: sem Refs acadêmicas")

    # Phases with estimates and coherent total.
    phase_rows = re.findall(r"^\| (F\d) \|[^|]+\|[^|]+\|[^|]+\| ([\d]+–[\d]+ h) \|", text, flags=re.MULTILINE)
    check(len(phase_rows) == 9, f"fases com estimativa: {len(phase_rows)} (esperado 9: F0–F8)")
    lows = highs = 0
    for _, effort in phase_rows:
        low, high = effort.replace(" h", "").split("–")
        lows += int(low)
        highs += int(high)
    total_row = re.search(r"\*\*Total\*\* \| \*\*(\d+) tarefas\*\* \|[^|]+\|[^|]+\| \*\*(\d+)–(\d+) h\*\*", text)
    check(total_row is not None, "linha de total ausente")
    if total_row:
        check(int(total_row.group(2)) == lows, f"total mínimo {total_row.group(2)} != soma {lows}")
        check(int(total_row.group(3)) == highs, f"total máximo {total_row.group(3)} != soma {highs}")
        check(int(total_row.group(1)) == len(task_ids), "total de tarefas != contagem NB")

    # Aliases declared.
    for alias in ("$UI", "$UI_TEST", "$APP", "$APP_TEST", "$PROJECTS", "$PROJECTS_TEST",
                  "$AUTH", "$AUTH_TEST", "$AUTH_ANDROID_TEST", "$TASKS", "$TASKS_TEST",
                  "$SETTINGS", "$SETTINGS_TEST"):
        check(f"| `{alias}` |" in text, f"alias ausente: {alias}")

    # Confirmed decisions Q1-Q30 present.
    for q in range(1, 31):
        check(f"| Q{q} |" in text, f"decisão Q{q} ausente")

    print(f"verify_roadmap: {CHECKS} verificações")
    if ERRORS:
        for err in ERRORS:
            print(f"FALHA: {err}")
        print(f"verify_roadmap: {len(ERRORS)} falha(s)")
        return 1
    print("verify_roadmap: OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
