"""Impede APK de testes sem runner ou sem os schemas históricos de migration."""

from pathlib import Path
import sys
from zipfile import ZipFile


def verify(apk: Path) -> None:
    schema_root = Path(__file__).resolve().parents[2] / "core/data/schemas"
    schemas = list(schema_root.rglob("*.json"))
    if not schemas:
        raise SystemExit("No exported Room schemas found")
    with ZipFile(apk) as archive:
        names = set(archive.namelist())
        for schema in schemas:
            expected = "assets/" + schema.relative_to(schema_root).as_posix()
            if expected not in names:
                raise SystemExit(f"Missing migration asset: {expected}")
        dex_files = (archive.read(name) for name in names if name.endswith(".dex"))
        if not any(b"Landroidx/test/runner/AndroidJUnitRunner;" in dex for dex in dex_files):
            raise SystemExit("AndroidJUnitRunner missing from test APK")
    print(f"OK: test runner and {len(schemas)} migration schemas packaged.")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
