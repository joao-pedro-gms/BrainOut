# BrainOut — Backend Stub

Stub mínimo do serviço de retaguarda usado pelo CI e como contrato inicial
para a integração do app (R6 — persistência remota / sincronização).

Este stub existe por dois motivos:

1. Permitir que os testes de integração do Android rodem no GitHub Actions
   sem depender de um backend externo provisionado.
2. Servir de contrato de API para a equipe começar a implementar o app e
   os serviços reais.

## Estrutura

- `server.py` — aplicação FastAPI com `/health`, `/v1/ping`,
  `/v1/projects`, `/v1/tasks`. Persistência em memória (redefinida a cada
  boot). **Não é o backend definitivo** — quando a equipe decidir a
  plataforma definitiva (Firebase/Supabase/Spring/etc.), este diretório
  será substituído e o workflow `ci.yml` atualizado.
- `requirements.txt` — dependências mínimas para o CI.
- `Dockerfile` — imagem usada pelo `docker build` no CI.

## Como rodar localmente

```bash
cd backend-stub
pip install -r requirements.txt
python server.py
# expõe em http://localhost:8000
```

## Endpoints

| Método | Caminho              | Função                                       |
|--------|---------------------|----------------------------------------------|
| GET    | `/health`           | Health check (esperado pelo CI)              |
| GET    | `/v1/ping`          | Smoke test                                   |
| GET    | `/v1/projects`      | Lista de projetos                            |
| POST   | `/v1/projects`      | Cria projeto                                 |
| GET    | `/v1/tasks`         | Lista tarefas (filtro por `project_id`)      |
| POST   | `/v1/tasks`         | Cria tarefa                                  |

A especificação completa está em `docs/ARQUITETURA.md` (a ser escrita no
Ciclo 1 da N1).