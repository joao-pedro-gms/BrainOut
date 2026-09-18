"""Stub FastAPI usado pelo CI para testes de integração do Android.

Não é o backend definitivo — apenas um contrato inicial (R6).
A persistência é em memória, reiniciada a cada boot.
"""

from __future__ import annotations

import os
import uuid
from typing import Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

APP_ENV = os.environ.get("APP_ENV", "dev")

app = FastAPI(title="BrainOut Stub API", version="0.1.0")

_PROJECTS: dict[str, dict] = {}
_TASKS: dict[str, dict] = {}


class ProjectIn(BaseModel):
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


class Task(TaskIn):
    id: str
    created_at: str


def _now_iso() -> str:
    import datetime as _dt

    return _dt.datetime.utcnow().isoformat(timespec="seconds") + "Z"


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "env": APP_ENV}


@app.get("/v1/ping")
def ping() -> dict:
    return {"pong": True}


@app.get("/v1/projects")
def list_projects() -> dict:
    return {"items": list(_PROJECTS.values())}


@app.post("/v1/projects", status_code=201)
def create_project(body: ProjectIn) -> dict:
    pid = uuid.uuid4().hex
    project = {"id": pid, "created_at": _now_iso(), **body.model_dump()}
    _PROJECTS[pid] = project
    return project


@app.get("/v1/projects/{project_id}")
def get_project(project_id: str) -> dict:
    if project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    return _PROJECTS[project_id]


@app.get("/v1/tasks")
def list_tasks(project_id: Optional[str] = None) -> dict:
    items = list(_TASKS.values())
    if project_id is not None:
        items = [t for t in items if t["project_id"] == project_id]
    return {"items": items}


@app.post("/v1/tasks", status_code=201)
def create_task(body: TaskIn) -> dict:
    if body.project_id not in _PROJECTS:
        raise HTTPException(status_code=404, detail="project not found")
    tid = uuid.uuid4().hex
    task = {"id": tid, "created_at": _now_iso(), **body.model_dump()}
    _TASKS[tid] = task
    return task