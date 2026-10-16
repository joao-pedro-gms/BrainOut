"""Verifica o manifesto final mesclado, incluindo permissões das dependências."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET


def verify(path: Path) -> None:
    root = ET.parse(path).getroot()
    android = "{http://schemas.android.com/apk/res/android}"
    permissions = {
        element.get(f"{android}name")
        for element in root
        if element.tag.startswith("uses-permission")
    }
    forbidden = permissions & {
        "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
    }
    if forbidden:
        raise SystemExit(f"Network permissions in local app: {sorted(forbidden)}")
    application = root.find("application")
    if application is None or application.get(f"{android}usesCleartextTraffic") != "false":
        raise SystemExit("Local app must explicitly disable cleartext traffic")
    if "android.permission.POST_NOTIFICATIONS" not in permissions:
        raise SystemExit("Deadline notification permission must be preserved")
    print("OK: final manifest has no network permissions; local notifications preserved.")


if __name__ == "__main__":
    verify(Path(sys.argv[1]))
