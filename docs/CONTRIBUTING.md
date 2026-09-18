# Contribuindo — BrainOut

> Diretrizes para o desenvolvimento do projeto. Alinhadas ao item 6.2
> (Versionamento) do documento norteador.

## Fluxo básico

1. **Crie ou pegue uma issue.** Sem issue, sem branch. Cada item do
   `ROADMAP.md` vira uma issue (template `User story`).
2. **Crie uma branch a partir de `main`.**
   ```bash
   git checkout main && git pull
   git checkout -b feat/<id-issue>-<slug-curto>
   # exemplos:
   #   feat/12-tela-login
   #   fix/43-crash-rotacao
   #   chore/r2-leak-na-roupdate
   ```
3. **Faça commits pequenos e descritivos.** Mensagens seguem
   [Conventional Commits](https://www.conventionalcommits.org/pt-br/)
   (exemplos abaixo). Nada de `wip`, `fix`, `ajustes`.
4. **Mantenha a branch atualizada** com `main` enquanto trabalha:
   ```bash
   git fetch origin
   git rebase origin/main
   ```
   (evita merges de merge, mantém histórico linear).
5. **Abra o PR** com o template preenchido. Marque qual requisito (R1–R14)
   o PR atende.
6. **Aguarde o CI verde.** O `CODEOWNERS` está configurado para exigir
   aprovação do próprio autor antes do merge. Itens de mais de 200
   linhas podem pedir uma segunda revisão.
7. **Merge só após CI verde.** Sem exceção.

## Convenção de mensagens

```
<tipo>(<escopo opcional>): <descrição curta no imperativo>

<corpo opcional explicando o "por quê">

Refs: R5, E2.6
```

Tipos:

| Tipo       | Quando usar                                       |
|------------|---------------------------------------------------|
| `feat`     | Nova funcionalidade visível ao usuário            |
| `fix`      | Correção de bug                                    |
| `chore`    | Configuração, dependências, scripts (sem impacto direto) |
| `refactor` | Mudança interna sem alterar comportamento         |
| `docs`     | Apenas documentação                               |
| `test`     | Apenas testes                                     |
| `perf`     | Melhoria de desempenho                            |
| `ci`       | Mudança em pipeline/workflow                      |

Exemplos válidos:

```
feat(auth): tela de login com validação inline
fix(tasks): bloqueio de prioridade em task concluída
chore(gradle): atualizar Kotlin para 2.0.21
docs(roadmap): marcar E1.6 como concluído
ci(workflows): adicionar cache para ~/.gradle/caches
```

## Estilo de código

- **Kotlin:** estilo oficial JetBrains; `ktlintCheck` é a fonte da verdade.
- **Compose:** `@Preview` em todos os componentes reutilizáveis.
- **Nomes:** `PascalCase` para tipos, `camelCase` para funções/varáveis,
  `snake_case` apenas em arquivos Gradle.
- **Strings:** 100% em `res/values/strings.xml` (e variantes localizadas).
- **IDs de view (XML, quando usado):** prefixos `et`, `til`, `btn`, `tv`,
  `iv`, `seek` para manter a tradição didática do curso.
- **Sem código comentado.** Remova antes de comitar.

## Commits atômicos

Cada commit deve ser uma unidade coerente. Evite commits "pacotão".
Uma boa heurística: o commit deve poder ser revertido sem quebrar a
build por mais de 5 minutos.

## Não versione

- `local.properties`
- `.keystore`, `.jks`, `.p12`
- `google-services.json` (a menos que combinado por conta própria)
- `.env`, `secrets/`
- saídas de build (`*.apk`, `*.aab`, `build/`)

Já estão cobertos pelo `.gitignore`, mas vale reforçar.

## Responsabilidade individual (FPI)

O item 8.4 do documento norteador define o **Fator de Participação
Individual**. Por se tratar de projeto executado individualmente, o FPI
mantém-se em 1,00 desde que a contribuição técnica seja contínua,
comprovada pelo histórico de PRs, issues e commits. Sinal de risco:
longos intervalos sem contribuição, commits concentrados perto das
entregas (vedado pelo item 6.2).

## Comunicação

- **Quadro de tarefas:** GitHub Projects (link a adicionar).
- **Checkpoints:** conforme cronograma da Seção 7 do documento
  norteador; relatório via Apêndice B preenchido.
- **Bloqueios:** abrir issue com label `impediment` assim que
  identificados, não na véspera da entrega.