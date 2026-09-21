# BrainOut — Backend Stub

Stub mínimo do serviço de retaguarda usado pelo CI e como contrato inicial
para a integração do app (R6 — persistência remota / sincronização).

Este stub existe por dois motivos:

1. Permitir que os testes de integração do Android rodem no GitHub Actions
   sem depender de um backend externo provisionado.
2. Servir de contrato de API para a equipe começar a implementar o app e
   os serviços reais.

## Estrutura

- `server.py` — aplicação FastAPI (`/health`, `/v1/ping`,
  `/v1/projects`, `/v1/tasks`, `/v1/tags`). Persistência em memória
  (redefinida a cada boot). **Não é o backend definitivo** — quando a
  equipe decidir a plataforma definitiva (FastAPI próprio hospedado
  em algum provedor), este diretório será substituído e o workflow
  `ci.yml` atualizado.
- `tests/test_contract.py` — suíte pytest cobrindo o contrato
  (upsert idempotente, delete idempotente, roundtrip de tags,
  cascade de tasks, validação de UUID, divergência id body/path).
- `tests/smoke_e2e.py` — smoke ponta-a-ponta via HTTP (requer
  servidor de pé em `127.0.0.1:8765`).
- `requirements.txt` — dependências de runtime + testes.
- `Dockerfile` — imagem usada pelo `docker build` no CI.

## Política de IDs (cliente-supplied UUID)

O stub adota a política **cliente-supplied**: o app Android gera o
UUID da entidade localmente (Room) e envia no corpo (`POST`) ou no
caminho (`PUT`). O servidor apenas respeita o ID recebido — não
substitui por um próprio.

Razões (alinhadas ao SyncWorker do E3.3):

- **Idempotência de replay** — reenviar a mesma operação POST/PUT
  não duplica a entidade.
- **Identidade estável** — o ID local é a verdade antes de qualquer
  sincronização; o servidor nunca o sobrescreve.
- **Foreign keys locais** (`project_id` em tasks, `project_id ↔ tag`)
  já são UUIDs locais; o servidor precisa honrá-los para as relações
  sobreviverem ao sync.

Consequências observáveis:

- `POST /v1/{resource}` aceita `id` opcional no corpo. Se vier, é
  usado; se não, o servidor gera um UUID canônico.
- `PUT /v1/{resource}/{id}` é **upsert idempotente**: cria se ausente,
  substitui se presente. `created_at` é fixado na primeira inserção e
  preservado em updates.
- `DELETE /v1/{resource}/{id}` retorna **204** mesmo com id ausente
  (idempotente — não trava o replay da fila offline).
- IDs mal formados (não-UUID) retornam **400**.
- `POST` com `id` no corpo divergente do path retorna **400**.
- **Tags**: ID é gerado pelo servidor (vocabulário controlado pelo
  usuário, sem identidade local forte antes do upload).

## Como rodar localmente

```bash
cd backend-stub
pip install -r requirements.txt
python -m uvicorn server:app --host 0.0.0.0 --port 8000
# expõe em http://localhost:8000
```

## Como rodar os testes

```bash
cd backend-stub
pip install -r requirements.txt
pytest -q              # 14 casos verdes
```

Para o smoke ponta-a-ponta (servidor precisa estar de pé):

```bash
python -m uvicorn server:app --host 127.0.0.1 --port 8765 &
python tests/smoke_e2e.py
```

## Endpoints

| Método | Caminho                                | Função                                            |
|--------|----------------------------------------|---------------------------------------------------|
| GET    | `/health`                              | Health check (esperado pelo CI)                   |
| GET    | `/v1/ping`                             | Smoke test                                        |
| GET    | `/v1/projects`                         | Lista de projetos                                 |
| POST   | `/v1/projects`                         | Cria projeto (id do cliente opcional)             |
| GET    | `/v1/projects/{id}`                    | Busca projeto por id                              |
| PUT    | `/v1/projects/{id}`                    | Upsert idempotente de projeto                     |
| DELETE | `/v1/projects/{id}`                    | Remove projeto + cascade de tasks filhas          |
| POST   | `/v1/projects/{id}/tags`               | Associa tag existente ao projeto                  |
| GET    | `/v1/projects/{id}/tags`               | Lista tags do projeto                             |
| GET    | `/v1/tasks`                            | Lista tarefas (filtro `?project_id=`)             |
| POST   | `/v1/tasks`                            | Cria tarefa (id do cliente opcional)              |
| GET    | `/v1/tasks/{id}`                       | Busca tarefa por id                               |
| PUT    | `/v1/tasks/{id}`                       | Upsert idempotente de tarefa                      |
| DELETE | `/v1/tasks/{id}`                       | Remove tarefa                                     |
| GET    | `/v1/tags`                             | Lista tags                                        |
| POST   | `/v1/tags`                             | Cria tag (id gerado no servidor)                  |
| DELETE | `/v1/tags/{id}`                        | Remove tag + desassocia dos projetos              |

Todos os payloads mantêm `snake_case` para casar com os DTOs em
`core/data/src/main/kotlin/.../core/data/remote/RemoteDtos.kt`.

A especificação completa está em `docs/ARQUITETURA.md` (seção 4 e 5).