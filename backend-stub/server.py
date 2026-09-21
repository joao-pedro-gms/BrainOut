"""Stub FastAPI usado pelo CI para testes de integração do Android.

Não é o backend definitivo — apenas um contrato inicial (R6).
A persistência é em memória, reiniciada a cada boot.

======================================================================
Política de IDs (cliente-supplied UUID)
======================================================================
Este stub adota a política "cliente-supplied": o cliente (app Android) é
quem gera o UUID da entidade localmente e envia no corpo da requisição
ou no path da URL. O servidor apenas respeita o ID recebido.

Razões (alinhadas ao SyncWorker do E3.3):
1. Idempotência de replay — reenviar a mesma operação POST/PUT não
   duplica a entidade (mesmo `id`, mesmo payload).
2. Identidade estável entre clientes — o ID local é a verdade antes de
   qualquer sincronização; o servidor nunca substitui o ID do cliente
   por um próprio.
3. Foreign keys locais (project_id em tasks, project_id↔tag) já são
   UUIDs locais; o servidor precisa honrá-los para que as relações
   sobrevivam ao sync.

Consequências:
- `POST /v1/projects` e `POST /v1/tasks`: o `id` no corpo é OPCIONAL.
  Se ausente, o servidor gera um UUID. Com `id` válido, a inserção
  é idempotente: reenviar o mesmo payload não duplica.
- `PUT /v1/{resource}/{id}` é upsert idempotente: cria se ausente,
  substitui se presente. `created_at` é fixado na primeira inserção e
  preservado em updates.
- `DELETE /v1/{resource}/{id}` retorna 204; 404 se ausente? NÃO — o
  stub é intencionalmente idempotente e devolve 204 em ambos os casos
  (não trava o replay da fila offline).
- Tags: ID é gerado pelo servidor (são entidades de vocabulário
  controlado pelo usuário, sem identidade local forte antes do upload).
"""

from __future__ import annotations

import os
import re
import uuid
from typing import Optional

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field, field_validator

APP_ENV = os.environ.get("APP_ENV", "dev")

app = FastAPI(title="BrainOut Stub API", version="0.2.0")

_PROJECTS: dict[str, dict] = {}
_TASKS: dict[str, dict] = {}
_TAGS: dict[str, dict] = {}
_PROJECT_TAGS: dict[str, set[str]] = {}  # project_id -> {tag_id}

_UUID_RE = re.compile(
    r"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
)


def _is_uuid(value: str) -> bool:
    return bool(_UUID_RE.match(value))


def _normalize_id(value: str) -> str:
    """Aceita hex (32 chars, gerado por uuid4().hex) ou UUID canônico."""
    if _is_uuid(value):
        return value
    try:
        return str(uuid.UUID(value))
    except (ValueError, AttributeError) as exc:
        raise HTTPException(status_code=400, detail="id deve ser UUID") from exc


# --- Modelos (Pydantic) ------------------------------------------------------


class ProjectIn(BaseModel):
    name: str = Field(min_length=1, max_length=120)
    description: Optional[str] = Field(default=None, max_length=500)


class ProjectUpsert(BaseModel):
    """Body do POST/PUT de projeto — `id` opcional (política cliente-supplied)."""

    id: Optional[str] = None
    name: str = Field(min_length=1, max_length=120)
    description: Optional[str] = Field(default=None, max_length=500)


class Project(ProjectIn):
    id: str
    created_at: str


class TaskIn(BaseModel):
    project_id: str
    title: str = Field(min_length=1, max_length=200)
    priority: int = Field(default=0, ge=0, le=4)
    done: bool = False


class TaskUpsert(BaseModel):
    """Body do POST/PUT de tarefa — `id` opcional."""

    id: Optional[str] = None
    project_id: str
    title: str = Field(min_length=1, max_length=200)
    priority: int = Field(default=0, ge=0, le=4)
    done: bool = False


class Task(TaskIn):
    id: str
    created_at: str


class TagIn(BaseModel):
    name: str = Field(min_length=1, max_length=60)
    color: str = Field(default="#888888", pattern=r"^#[0-9a-fA-F]{6}$")


class Tag(TagIn):
    id: str
    created_at: str


class TagAssociation(BaseModel):
    tag_id: str

    @field_validator("tag_id")
    @classmethod
    def _check_uuid(cls, value: str) -> str:
        if not _is_uuid(value):
            raise ValueError("tag_id deve ser UUID")
        return value


# --- Helpers --------------------------------------------------------------


def _now_iso() -> str:
    import datetime as _dt

    return _dt.datetime.utcnow().isoformat(timespec="seconds") + "Z"


def _require_uuid(value: str, field: str) -> None:
    if not _is_uuid(value):
        raise HTTPException(status_code=400, detail=f"{field} deve ser UUID")


# --- Health ---------------------------------------------------------------


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "env": APP_ENV}


@app.get("/v1/ping")
def ping() -> dict:
    return {"pong": True}


# --- Projects -------------------------------------------------------------


@app.get("/v1/projects")
def list_projects() -> dict:
    return {"items": list(_PROJECTS.values())}


@app.post("/v1/projects", status_code=201)
def create_project(body: ProjectUpsert) -> dict:
    """Cria projeto com ID do cliente (idempotente) ou gerado pelo servidor.

    Política cliente-supplied: se `body.id` vier, é usado como chave;
    reenviar o mesmo payload retorna o registro já criado em vez de
    duplicar. Se `body.id` for None, o servidor gera um UUID.
    """
    if body.id is not None:
        pid = _normalize_id(body.id)
        if pid in _PROJECTS:
            # Idempotência: replay não duplica.
            return _PROJECTS[pid]
    else:
        pid = str(uuid.uuid4())
    project = {
        "id": pid,
        "created_at": _now_iso(),
        "name": body.name,
        "description": body.description,
    }
    _PROJECTS[pid] = project
    _PROJECT_TAGS.setdefault(pid, set())
    return project


@app.get("/v1/projects/{project_id}")
def get_project(project_id: str) -> dict:
    _require_uuid(project_id, "project_id")
    if project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    return _PROJECTS[project_id]


@app.put("/v1/projects/{project_id}")
def upsert_project(project_id: str, body: ProjectUpsert) -> dict:
    """Upsert idempotente: cria se ausente, atualiza se presente.

    O `id` do path manda. Se o body trouxer `id`, deve bater com o path;
    divergência é erro 400 (cliente mal formado) para evitar inconsistência
    silenciosa durante o sync.
    """
    _require_uuid(project_id, "project_id")
    if body.id is not None and body.id != project_id:
        raise HTTPException(
            status_code=400,
            detail="id do corpo difere do id do path",
        )
    existing = _PROJECTS.get(project_id)
    if existing is None:
        record = {
            "id": project_id,
            "created_at": _now_iso(),
            "name": body.name,
            "description": body.description,
        }
        _PROJECTS[project_id] = record
        _PROJECT_TAGS.setdefault(project_id, set())
    else:
        existing["name"] = body.name
        existing["description"] = body.description
        # `created_at` é preservado no upsert (regra do append-only).
        record = existing
    return record


@app.delete("/v1/projects/{project_id}", status_code=204)
def delete_project(project_id: str) -> Response:
    """Delete idempotente: 204 mesmo que já ausente.

    Garante também limpeza das tags do projeto e das tasks filhas
    (cascade — o backend definitivo vai tratar essa regra de negócio;
    aqui só refletimos no stub para que o SyncWorker consiga reproduzir
    um cenário real de remoção sem deixar órfãos no `list_tasks`).
    """
    _require_uuid(project_id, "project_id")
    _PROJECTS.pop(project_id, None)
    _PROJECT_TAGS.pop(project_id, None)
    for tid in [t for t, v in _TASKS.items() if v["project_id"] == project_id]:
        _TASKS.pop(tid, None)
    return Response(status_code=204)


@app.post("/v1/projects/{project_id}/tags", status_code=201)
def add_project_tag(project_id: str, body: TagAssociation) -> dict:
    """Associa uma tag existente a um projeto (idempotente)."""
    _require_uuid(project_id, "project_id")
    if project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    if body.tag_id not in _TAGS:
        raise HTTPException(status_code=404, detail="tag not found")
    _PROJECT_TAGS.setdefault(project_id, set()).add(body.tag_id)
    return {"project_id": project_id, "tag_id": body.tag_id}


@app.get("/v1/projects/{project_id}/tags")
def list_project_tags(project_id: str) -> dict:
    _require_uuid(project_id, "project_id")
    if project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    tag_ids = _PROJECT_TAGS.get(project_id, set())
    items = [_TAGS[t] for t in tag_ids if t in _TAGS]
    return {"items": items}


# --- Tasks ----------------------------------------------------------------


@app.get("/v1/tasks")
def list_tasks(project_id: Optional[str] = None) -> dict:
    items = list(_TASKS.values())
    if project_id is not None:
        items = [t for t in items if t["project_id"] == project_id]
    return {"items": items}


@app.post("/v1/tasks", status_code=201)
def create_task(body: TaskUpsert) -> dict:
    """Cria tarefa com ID do cliente (idempotente) ou gerado pelo servidor."""
    if body.project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    if body.id is not None:
        tid = _normalize_id(body.id)
        if tid in _TASKS:
            return _TASKS[tid]
    else:
        tid = str(uuid.uuid4())
    task = {
        "id": tid,
        "created_at": _now_iso(),
        "project_id": body.project_id,
        "title": body.title,
        "priority": body.priority,
        "done": body.done,
    }
    _TASKS[tid] = task
    return task


@app.get("/v1/tasks/{task_id}")
def get_task(task_id: str) -> dict:
    _require_uuid(task_id, "task_id")
    if task_id not in _TASKS:
        raise HTTPException(status_code=404, detail="task not found")
    return _TASKS[task_id]


@app.put("/v1/tasks/{task_id}")
def upsert_task(task_id: str, body: TaskUpsert) -> dict:
    """Upsert idempotente de tarefa."""
    _require_uuid(task_id, "task_id")
    if body.id is not None and body.id != task_id:
        raise HTTPException(
            status_code=400,
            detail="id do corpo difere do id do path",
        )
    if body.project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    existing = _TASKS.get(task_id)
    if existing is None:
        record = {
            "id": task_id,
            "created_at": _now_iso(),
            "project_id": body.project_id,
            "title": body.title,
            "priority": body.priority,
            "done": body.done,
        }
        _TASKS[task_id] = record
    else:
        existing["project_id"] = body.project_id
        existing["title"] = body.title
        existing["priority"] = body.priority
        existing["done"] = body.done
        record = existing
    return record


@app.delete("/v1/tasks/{task_id}", status_code=204)
def delete_task(task_id: str) -> Response:
    """Delete idempotente de tarefa."""
    _require_uuid(task_id, "task_id")
    _TASKS.pop(task_id, None)
    return Response(status_code=204)


# --- Tags -----------------------------------------------------------------


@app.get("/v1/tags")
def list_tags() -> dict:
    return {"items": list(_TAGS.values())}


@app.post("/v1/tags", status_code=201)
def create_tag(body: TagIn) -> dict:
    """Cria tag com ID gerado pelo servidor.

    Tags são vocabulário controlado pelo usuário — não há identidade
    local forte antes do upload (o app não precisa saber o ID antes de
    criar, ele descobre via GET /v1/tags e referencia o que já existe).
    """
    tid = str(uuid.uuid4())
    tag = {
        "id": tid,
        "created_at": _now_iso(),
        "name": body.name,
        "color": body.color,
    }
    _TAGS[tid] = tag
    return tag


@app.delete("/v1/tags/{tag_id}", status_code=204)
def delete_tag(tag_id: str) -> Response:
    """Delete idempotente de tag; remove de todas as associações."""
    _require_uuid(tag_id, "tag_id")
    _TAGS.pop(tag_id, None)
    for project_id in list(_PROJECT_TAGS.keys()):
        _PROJECT_TAGS[project_id].discard(tag_id)
    return Response(status_code=204)