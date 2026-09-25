# BrainOut — backend-stub

## OVERVIEW

Stub FastAPI de dev + CI. Persistência em memória. Substituído pelo FastAPI hospedado (decisão E3.1, `docs/ARQUITETURA.md` §9). Único código não-Android do repo.

## STRUCTURE

- `server.py` — app FastAPI. Endpoints + política cliente-supplied UUID. Estado em dicts globais.
- `tests/test_contract.py` — 14 casos pytest (TestClient, sem servidor).
- `tests/smoke_e2e.py` — smoke via HTTP/curl. Requer servidor em `127.0.0.1:8765`.
- `requirements.txt` — runtime + testes (pytest/httpx juntos, p/ CI não instalar nada extra).
- `Dockerfile` — `python:3.12-slim`, copia server + tests, `EXPOSE 8000`.
- `README.md` — contrato detalhado + endpoints.

## CONTRACT

cliente-supplied UUID: app gera id local (Room), servidor respeita. Nunca substitui.

| Método | Caminho | Notas |
|--------|---------|-------|
| GET | `/health` | `{"status":"ok","env":APP_ENV}`; gate do CI |
| GET | `/v1/ping` | `{"pong":true}` |
| GET/POST | `/v1/projects` | POST: `id` opcional no corpo |
| GET/PUT/DELETE | `/v1/projects/{id}` | DELETE cascata em tasks filhas |
| POST/GET | `/v1/projects/{id}/tags` | associa tag existente (404 se tag/projeto ausente) |
| GET/POST | `/v1/tasks` | filtro `?project_id=`; POST exige projeto existente (404) |
| GET/PUT/DELETE | `/v1/tasks/{id}` | |
| GET/POST/DELETE | `/v1/tags` | id da tag **cliente-supplied** (R6 — paridade com project/task). POST sem id: servidor gera. POST com id existente: replay idempotente (devolve o registro). DELETE: 204 mesmo ausente. |

Regras (ver `server.py` docstring):

- POST com `id` já existente → retorna registro existente (replay não duplica).
- PUT = upsert idempotente: cria se ausente, substitui se presente. `created_at` fixado na 1ª inserção, preservado em updates.
- DELETE idempotente: 204 mesmo com id ausente (não trava replay da fila offline).
- id não-UUID → 400.
- PUT com `id` do corpo ≠ id do path → 400 (`"id do corpo difere do id do path"`).
- Tag `color` valida regex `^#[0-9a-fA-F]{6}$` (422 se inválido).
- Payloads `snake_case` — casam com `RemoteDtos.kt`.

## COMMANDS

```bash
cd backend-stub
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python -m uvicorn server:app --host 0.0.0.0 --port 8000
# check: curl http://127.0.0.1:8000/health → {"status":"ok","env":"dev"}

pytest -q    # 14 casos de contrato

# smoke e2e (servidor de pé antes):
python -m uvicorn server:app --host 127.0.0.1 --port 8765 &
python tests/smoke_e2e.py

# Docker:
docker build -t brainout-stub .
docker run -d --name brainout-stub -p 8000:8000 brainout-stub
```

## CONECTIVIDADE

- Emulador → host: `10.0.2.2:8000` (default do `BASE_URL` dev). Não `localhost`.
- Dispositivo físico → IP LAN do host + `--host 0.0.0.0`.
- CI `backend-integration`: `docker build` da imagem + `curl /health`.

## ANTI-PATTERNS

- Sem storage persistente. Reinício zera dados. De propósito.
- Sem auth. Stub não autentica.
- Nunca apontar flavor `prod` aqui. `prod` usa `https://TBD/`.
- Mudança de contrato exige atualizar juntos: `BrainOutApi` + `RemoteDtos` + este arquivo + `tests/test_contract.py`.
