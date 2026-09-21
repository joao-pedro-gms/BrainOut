# Ata — Checkpoint 2

**Projeto:** BrainOut — Aplicativo de gestão de projetos e tarefas
**Data:** 21/09/2026
**Participante:** João Pedro G M Silva (participação individual)

## Deliberação — E3.1: plataforma do serviço de retaguarda

Foi deliberada a escolha da plataforma definitiva do serviço de
retaguarda (R6). As alternativas avaliadas — Firebase, Supabase e
backend próprio — foram comparadas quanto a custo, controle de dados,
esforço de integração, curva de aprendizado e modelo de deploy
(`docs/ARQUITETURA.md`, Seção 9).

**Decisão:** adotar **backend próprio com FastAPI**, evoluindo o stub
existente em `backend-stub/` (persistência em memória → PostgreSQL),
mantendo o contrato REST `/v1/projects` e `/v1/tasks` e a implantação em
VM própria acessível pela rede Tailscale.

Justificativa: controle integral dos dados de autenticação e perfis
(R2), custo zero de operação, contrato REST já implementado e exercitado
no pipeline de integração contínua, e alinhamento com o modelo relacional
já consolidado na persistência local (Room).

Decisão validada para apresentação no Checkpoint 2 (06/11/2026).

---

João Pedro G M Silva - PUC Goiás ADS - 20251012000740
