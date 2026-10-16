"""Executa o Bash real do workflow sem build, credenciais ou publicação."""

import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/release-apk-debug.yml"
GIT_BASH = Path("C:/Program Files/Git/bin/bash.exe")
BASH = str(GIT_BASH) if os.name == "nt" and GIT_BASH.is_file() else shutil.which("bash")


def release_script():
    """Extrai o bloco literal Bash do passo nomeado, sem dependência YAML."""
    text = WORKFLOW.read_text(encoding="utf-8")
    step = text.split("      - name: Resolve release tag\n", 1)[1]
    step = step.split("\n      - ", 1)[0]
    script = step.split("        run: |\n", 1)[1]
    return textwrap.dedent(script)


@unittest.skipUnless(BASH, "Bash necessário para executar o contrato do workflow")
class ReleaseTagTest(unittest.TestCase):
    def run_tag(self, value, event="workflow_dispatch", ref="main"):
        with tempfile.TemporaryDirectory() as directory:
            env_file = Path(directory) / "github-env"
            env_file.touch()
            env = os.environ.copy()
            env.update(
                GITHUB_EVENT_NAME=event,
                GITHUB_REF_NAME=ref,
                GITHUB_ENV=env_file.as_posix(),
                RELEASE_TAG_INPUT=value,
            )
            # Simula a interpolação do runner para detectar a regressão antiga.
            script = release_script().replace("${{ inputs.tag }}", value)
            result = subprocess.run(
                [BASH, "-c", script], cwd=directory, env=env,
                capture_output=True, text=True, timeout=10, check=False,
            )
            return result, env_file.read_text(), (Path(directory) / "injected").exists()

    def test_dispatch_aceita_tags_de_release(self):
        for tag in ("v0.1.0", "v1.2.3-rc.1", "v1.2.3+build_4"):
            with self.subTest(tag=tag):
                result, output, injected = self.run_tag(tag)
                self.assertEqual(result.returncode, 0, result.stderr)
                self.assertEqual(output, f"RELEASE_TAG={tag}\n")
                self.assertFalse(injected)

    def test_push_usa_ref_e_ignora_input_manual(self):
        result, output, injected = self.run_tag(
            "$(touch injected)", event="push", ref="v2.0.0",
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(output, "RELEASE_TAG=v2.0.0\n")
        self.assertFalse(injected)

    def test_tags_invalidas_nao_publicam_variavel(self):
        for tag in ("", "v", "1.0.0", "v1/branch", "v1 test", "v1\nOTHER=value"):
            with self.subTest(tag=tag):
                result, output, injected = self.run_tag(tag)
                self.assertNotEqual(result.returncode, 0)
                self.assertEqual(output, "")
                self.assertFalse(injected)

    def test_input_e_ref_nao_executam_comandos(self):
        payloads = (
            "v1$(touch injected)",
            "v1`touch injected`",
            'v1"; touch injected; #',
        )
        for tag in payloads:
            for event in ("workflow_dispatch", "push"):
                with self.subTest(tag=tag, event=event):
                    result, output, injected = self.run_tag(tag, event=event, ref=tag)
                    self.assertFalse(injected, "A tag foi executada como código shell")
                    self.assertNotEqual(result.returncode, 0)
                    self.assertEqual(output, "")

    def test_validacao_acontece_antes_de_checkout_e_build(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        validation = workflow.index("      - name: Resolve release tag")
        self.assertLess(validation, workflow.index("      - uses: actions/checkout"))
        self.assertNotRegex(release_script(), re.compile(r"\$\{\{"))
        self.assertIn("RELEASE_TAG_INPUT: ${{ inputs.tag }}", workflow)


if __name__ == "__main__":
    unittest.main()
