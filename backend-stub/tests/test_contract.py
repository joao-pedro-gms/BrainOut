# João Pedro G M Silva - PUC Goiás ADS - 20251012000740
"""Testes do contrato do backend-stub (E3.3 — backend-stub-contract).

Cobrem:
- Upsert idempotente (PUT em id ausente cria; PUT em id existente
  substitui sem duplicar; `created_at` preservado em updates).
- POST com ID do cliente é idempotente (replay não duplica).
- POST sem ID gera UUID no servidor (compatibilidade com clientes
  antigos / testes).
- DELETE idempotente (204 mesmo com id ausente).
- Validação de UUID em path/body (400 quando mal formado).
- Roundtrip de tags (criar → associar a projeto → listar → remover).
- Cascade: DELETE de projeto remove tasks filhas.
- Divergência id-do-corpo vs id-do-path retorna 400.

Para rodar:
    cd backend-stub
    pip install -r requirements.txt
    pip install pytest httpx
    pytest -q
"""

from __future__ import annotations

import os
import sys
import uuid

import pytest
from fastapi.testclient import TestClient

# Permite `import server` quando rodado de qualquer cwd.
_HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(_HERE))  # backend-stub/

import server  # noqa: E402  (path manipulation acima é intencional)


@pytest.fixture
def client() -> TestClient:
    # Limpa o estado in-memory antes de cada teste (o stub é singleton).
    server._PROJECTS.clear()
    server._TASKS.clear()
    server._TAGS.clear()
    server._PROJECT_TAGS.clear()
    return TestClient(server.app)


def _uuid_str() -> str:
    return str(uuid.uuid4())


# --- Projects: POST com/sem ID do cliente ---------------------------------


def test_post_project_with_client_id_is_idempotent(client: TestClient) -> None:
    pid = _uuid_str()
    payload = {"id": pid, "name": "Estudos", "description": "pós-graduação"}
    first = client.post("/v1/projects", json=payload)
    assert first.status_code == 201, first.text
    assert first.json()["id"] == pid
    created_at_first = first.json()["created_at"]

    # Replay do mesmo payload — não pode criar novo registro.
    replay = client.post("/v1/projects", json=payload)
    assert replay.status_code == 201, replay.text
    assert replay.json()["id"] == pid
    assert replay.json()["created_at"] == created_at_first

    listing = client.get("/v1/projects").json()["items"]
    assert len(listing) == 1
    assert listing[0]["id"] == pid


def test_post_project_without_id_generates_uuid(client: TestClient) -> None:
    resp = client.post("/v1/projects", json={"name": "Sem id"})
    assert resp.status_code == 201
    body = resp.json()
    assert server._is_uuid(body["id"])
    assert body["name"] == "Sem id"


# --- Projects: PUT upsert --------------------------------------------------


def test_put_project_creates_when_absent(client: TestClient) -> None:
    pid = _uuid_str()
    resp = client.put(
        f"/v1/projects/{pid}",
        json={"name": "novo", "description": None},
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["id"] == pid
    assert resp.json()["name"] == "novo"

    # Aparece na listagem.
    items = client.get("/v1/projects").json()["items"]
    assert any(item["id"] == pid for item in items)


def test_put_project_updates_and_preserves_created_at(client: TestClient) -> None:
    pid = _uuid_str()
    first = client.put(f"/v1/projects/{pid}", json={"name": "v1", "description": "d"})
    created_at = first.json()["created_at"]

    # Replay com name diferente.
    second = client.put(f"/v1/projects/{pid}", json={"name": "v2", "description": "d2"})
    assert second.status_code == 200
    assert second.json()["name"] == "v2"
    assert second.json()["description"] == "d2"
    # `created_at` é preservado (regra append-only).
    assert second.json()["created_at"] == created_at

    # Garantia: não duplicou.
    items = client.get("/v1/projects").json()["items"]
    matches = [i for i in items if i["id"] == pid]
    assert len(matches) == 1


def test_put_project_rejects_id_mismatch(client: TestClient) -> None:
    pid = _uuid_str()
    other = _uuid_str()
    resp = client.put(
        f"/v1/projects/{pid}",
        json={"id": other, "name": "x"},
    )
    assert resp.status_code == 400
    assert "difere" in resp.json()["detail"]


def test_put_project_rejects_non_uuid_path(client: TestClient) -> None:
    resp = client.put("/v1/projects/not-a-uuid", json={"name": "x"})
    assert resp.status_code == 400


# --- Projects: DELETE idempotente -----------------------------------------


def test_delete_project_returns_204_even_when_absent(client: TestClient) -> None:
    pid = _uuid_str()
    resp = client.delete(f"/v1/projects/{pid}")
    assert resp.status_code == 204
    assert resp.content == b""


def test_delete_project_cascades_to_tasks(client: TestClient) -> None:
    pid = _uuid_str()
    client.put(f"/v1/projects/{pid}", json={"name": "p"})
    tid = _uuid_str()
    client.put(f"/v1/tasks/{tid}", json={"project_id": pid, "title": "t"})

    assert len(client.get("/v1/tasks", params={"project_id": pid}).json()["items"]) == 1

    delete = client.delete(f"/v1/projects/{pid}")
    assert delete.status_code == 204

    after = client.get("/v1/tasks", params={"project_id": pid}).json()["items"]
    assert after == []


# --- Tasks: PUT/DELETE -----------------------------------------------------


def test_task_put_requires_existing_project(client: TestClient) -> None:
    tid = _uuid_str()
    bogus_project = _uuid_str()
    resp = client.put(
        f"/v1/tasks/{tid}",
        json={"project_id": bogus_project, "title": "t"},
    )
    assert resp.status_code == 404


def test_task_delete_idempotent(client: TestClient) -> None:
    tid = _uuid_str()
    resp = client.delete(f"/v1/tasks/{tid}")
    assert resp.status_code == 204


# --- Tags: criar, associar, listar, remover --------------------------------


def test_tag_roundtrip(client: TestClient) -> None:
    # 1) cria projeto
    pid = _uuid_str()
    client.put(f"/v1/projects/{pid}", json={"name": "p"})

    # 2) cria tag
    create = client.post("/v1/tags", json={"name": "urgente", "color": "#ff0000"})
    assert create.status_code == 201, create.text
    tag = create.json()
    assert tag["name"] == "urgente"
    assert tag["color"] == "#ff0000"
    tag_id = tag["id"]
    assert server._is_uuid(tag_id)

    # 3) lista
    listed = client.get("/v1/tags").json()["items"]
    assert any(t["id"] == tag_id for t in listed)

    # 4) associa tag → projeto
    assoc = client.post(
        f"/v1/projects/{pid}/tags",
        json={"tag_id": tag_id},
    )
    assert assoc.status_code == 201

    # 5) lista tags do projeto
    tags_of_project = client.get(f"/v1/projects/{pid}/tags").json()["items"]
    assert any(t["id"] == tag_id for t in tags_of_project)

    # 6) associar de novo é idempotente
    again = client.post(
        f"/v1/projects/{pid}/tags",
        json={"tag_id": tag_id},
    )
    assert again.status_code == 201
    tags_after = client.get(f"/v1/projects/{pid}/tags").json()["items"]
    assert len(tags_after) == 1

    # 7) associar tag inexistente → 404
    bogus = client.post(
        f"/v1/projects/{pid}/tags",
        json={"tag_id": _uuid_str()},
    )
    assert bogus.status_code == 404

    # 8) remove tag → some do projeto
    delete_tag = client.delete(f"/v1/tags/{tag_id}")
    assert delete_tag.status_code == 204
    assert client.get(f"/v1/projects/{pid}/tags").json()["items"] == []


def test_tag_color_must_match_hex_pattern(client: TestClient) -> None:
    resp = client.post("/v1/tags", json={"name": "x", "color": "red"})
    assert resp.status_code == 422  # validação do Pydantic


def test_tag_association_rejects_non_uuid(client: TestClient) -> None:
    pid = _uuid_str()
    client.put(f"/v1/projects/{pid}", json={"name": "p"})
    resp = client.post(
        f"/v1/projects/{pid}/tags",
        json={"tag_id": "not-a-uuid"},
    )
    assert resp.status_code == 422


# --- Healthcheck / ping preservados ----------------------------------------


def test_health_and_ping(client: TestClient) -> None:
    health = client.get("/health")
    assert health.status_code == 200
    body = health.json()
    assert body["status"] == "ok"
    # APP_ENV é injetado pelo container/CI; validamos só o tipo.
    assert isinstance(body["env"], str) and body["env"]
    assert client.get("/v1/ping").json() == {"pong": True}