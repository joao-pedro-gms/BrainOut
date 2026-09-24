"""Smoke test ponta-a-ponta via HTTP (curl) — validação do script da task."""
import json
import subprocess
import sys
import uuid


BASE = "http://127.0.0.1:8765"


def curl(method: str, path: str, body=None, want_status=None) -> tuple[int, dict | str]:
    args = ["curl", "-s", "-X", method, "-w", "\n%{http_code}", f"{BASE}{path}"]
    if body is not None:
        args += ["-d", json.dumps(body), "-H", "Content-Type: application/json"]
    out = subprocess.check_output(args, text=True)
    raw_body, _, raw_code = out.rpartition("\n")
    code = int(raw_code)
    if want_status is not None:
        return code, raw_body
    if not raw_body.strip():
        return code, ""
    try:
        return code, json.loads(raw_body)
    except json.JSONDecodeError:
        return code, raw_body


def step(label: str) -> None:
    print(f"\n=== {label} ===")


def main() -> int:
    pid = str(uuid.uuid4())

    step("POST /v1/projects com id do cliente")
    code, p1 = curl("POST", "/v1/projects", {"id": pid, "name": "x"})
    assert code == 200, p1
    assert p1["id"] == pid, p1
    print(f"  id={p1['id']} name={p1['name']}")

    step("POST /v1/projects replay (mesmo id) — idempotente")
    code, p2 = curl("POST", "/v1/projects", {"id": pid, "name": "x"})
    assert code == 200, p2
    assert p2["id"] == pid, p2
    assert p2["created_at"] == p1["created_at"], (p1["created_at"], p2["created_at"])
    print("  mesmo id, mesmo created_at — replay não duplica ✓")

    step("PUT /v1/projects/{id} (upsert)")
    code, p3 = curl("PUT", f"/v1/projects/{pid}", {"id": pid, "name": "y", "description": "atualizado"})
    assert code == 200, p3
    assert p3["name"] == "y", p3
    assert p3["created_at"] == p1["created_at"], "created_at deve ser preservado"
    print(f"  name atualizado, created_at preservado ✓")

    step("DELETE /v1/projects/{id} presente")
    code, _ = curl("DELETE", f"/v1/projects/{pid}", want_status=204)
    assert code == 204, code
    print(f"  code={code} ✓")

    step("DELETE /v1/projects/{id} ausente (idempotente)")
    bogus = str(uuid.uuid4())
    code, _ = curl("DELETE", f"/v1/projects/{bogus}", want_status=204)
    assert code == 204, code
    print(f"  code={code} ✓ (204 mesmo ausente)")

    step("POST /v1/tags + associação a projeto")
    p2 = str(uuid.uuid4())
    curl("POST", "/v1/projects", {"id": p2, "name": "p2"})
    code, tag = curl("POST", "/v1/tags", {"name": "urgente", "color": "#ff0000"})
    assert code == 200, tag
    print(f"  tag id={tag['id']}")
    code, _ = curl("POST", f"/v1/projects/{p2}/tags", {"tag_id": tag["id"]})
    assert code == 200
    code, listing = curl("GET", f"/v1/projects/{p2}/tags")
    assert any(t["id"] == tag["id"] for t in listing["items"]), listing
    print(f"  tags do projeto: {[t['name'] for t in listing['items']]} ✓")

    step("POST /v1/tasks com project_id do projeto criado")
    tid = str(uuid.uuid4())
    code, task = curl("POST", "/v1/tasks", {
        "id": tid,
        "project_id": p2,
        "title": "T1",
        "priority": 2,
    })
    assert code == 200, task
    assert task["id"] == tid
    print(f"  task id={task['id']} ✓")

    step("PUT /v1/tasks/{id} (upsert)")
    code, task2 = curl("PUT", f"/v1/tasks/{tid}", {
        "id": tid,
        "project_id": p2,
        "title": "T1 atualizada",
        "priority": 3,
        "done": True,
    })
    assert code == 200, task2
    assert task2["title"] == "T1 atualizada"
    print(f"  upsert preserva created_at: {task2['created_at']} ✓")

    step("DELETE /v1/projects/{id} cascateia tasks filhas")
    code, _ = curl("DELETE", f"/v1/projects/{p2}", want_status=204)
    assert code == 204
    code, tasks_after = curl("GET", "/v1/tasks")
    assert all(t["project_id"] != p2 for t in tasks_after["items"]), tasks_after
    print(f"  tasks órfãs removidas ✓")

    step("PUT com id do path != id do body → 400")
    a = str(uuid.uuid4())
    b = str(uuid.uuid4())
    args = ["curl", "-s", "-o", "/dev/null", "-w", "%{http_code}", "-X", "PUT",
            f"{BASE}/v1/projects/{a}", "-d", json.dumps({"id": b, "name": "x"}),
            "-H", "Content-Type: application/json"]
    code = int(subprocess.check_output(args, text=True))
    assert code == 400, code
    print(f"  code={code} ✓")

    print("\n=== TODOS OS PASSOS OK ✓ ===")
    return 0


if __name__ == "__main__":
    sys.exit(main())