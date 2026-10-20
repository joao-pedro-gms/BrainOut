#!/usr/bin/env python3
"""Verifica que as implementações de produção entram no denominador do Kover.

Issue #202 (DEF-64) — o gate de cobertura de 60% é real, mas ele só
mede o que o denominador permite. A exclusão `*_Impl` do Kover foi
escrita para pegar as implementações **geradas** pelo Room
(`UserDao_Impl`, `BrainOutDatabase_Impl`), e é fácil ler isso como "as
implementações de produção estão fora da cobertura". Não estão: o
padrão `*_Impl` exige um sublinhado, então `ProjectRepositoryImpl` e
`PasswordHasherImpl` continuam no denominador.

Este verificador fixa esse invariante para que uma exclusão ampla
introduzida no futuro não tornaria o gate verde sem medir nada:

1. Toda classe de produção de `:core:data` cujo nome termina em
   `RepositoryImpl`, `HasherImpl` ou `Store` precisa aparecer no
   relatório XML do Kover.
2. O padrão de exclusão `*_Impl` não pode voltar à lista de exclusões
   do `build.gradle.kts` da raiz — ele pegaria de volta as classes
   geradas do Room, mas também passaria a capturar qualquer
   implementação renomeada no formato `Algo_Impl`.

Uso:
    python3 .github/scripts/verify_coverage_scope.py \
        core/data/build/reports/kover/report.xml
"""

import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_REPORT = REPO_ROOT / "core/data/build/reports/kover/report.xml"
SOURCE_ROOT = REPO_ROOT / "core/data/src/main/kotlin"
ROOT_BUILD = REPO_ROOT / "build.gradle.kts"

# Sufixos das classes de produção que concentram a lógica testável do
# módulo: são elas que o gate de 60% precisa estar medindo.
TRACKED_SUFFIXES = ("RepositoryImpl", "HasherImpl", "Store")

# Padroes de exclusao amplos que esconderiam as classes acima.
# Procura uma entrada de lista (o padrao sozinho na linha), e nao
# qualquer mencao em comentario.
FORBIDDEN_EXCLUSION = re.compile(r'^\s*"\*_Impl"\,?\s*$', re.MULTILINE)


def fail(message: str) -> None:
    print(f"verify_coverage_scope: FALHA — {message}")
    sys.exit(1)


def collect_tracked_classes() -> set[str]:
    """Classes de produção que precisam estar no relatório de cobertura."""
    if not SOURCE_ROOT.is_dir():
        fail(f"diretório de fontes não encontrado: {SOURCE_ROOT}")
    # Declaração de classe no início da linha, com modificadores e
    # anotações opcionais antes do nome (`data class`, `internal class`,
    # `class X @Inject constructor(`).
    pattern = re.compile(
        r"(?m)^\s*(?:[A-Za-z]+\s+)*class\s+(\w+)",
    )
    tracked: set[str] = set()
    for path in SOURCE_ROOT.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        for name in pattern.findall(text):
            if name.endswith(TRACKED_SUFFIXES):
                tracked.add(name)
    if not tracked:
        fail("nenhuma classe de produção rastreada foi encontrada")
    return tracked


def measured_classes(report_path: Path) -> set[str]:
    """Nomes de classe presentes no relatório XML do Kover."""
    if not report_path.is_file():
        fail(
            "relatório do Kover não encontrado: "
            f"{report_path} (gere com ./gradlew :core:data:koverXmlReport)"
        )
    text = report_path.read_text(encoding="utf-8")
    # O Kover usa "/" como separador de pacote no nome do classe.
    return {
        name.rsplit("/", 1)[-1]
        for name in re.findall(r'<class\b[^>]*\bname="([^"]+)"', text)
    }


def check_exclusion_list() -> None:
    """O padrão amplo `*_Impl` não pode voltar às exclusões."""
    if not ROOT_BUILD.is_file():
        fail(f"build.gradle.kts da raiz não encontrado: {ROOT_BUILD}")
    text = ROOT_BUILD.read_text(encoding="utf-8")
    if FORBIDDEN_EXCLUSION.search(text):
        fail(
            "as exclusões do Kover voltaram a conter \"*_Impl\": esse padrão "
            "esconde qualquer implementação de produção renomeada no formato "
            "\"Algo_Impl\". Use os padrões do Room gerado (\"*Dao_Impl\", "
            "\"*Database_Impl\")."
        )


def main() -> int:
    report_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_REPORT
    tracked = collect_tracked_classes()
    measured = measured_classes(report_path)

    missing = sorted(name for name in tracked if name not in measured)
    if missing:
        fail(
            "classes de produção fora do denominador da cobertura: "
            + ", ".join(missing)
            + ". As exclusões do Kover estão escondendo a lógica que o gate "
            "deveria medir."
        )

    check_exclusion_list()

    print(
        f"verify_coverage_scope: OK — {len(tracked)} classes de produção "
        f"rastreadas presentes no relatório; exclusões sem padrão amplo."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
