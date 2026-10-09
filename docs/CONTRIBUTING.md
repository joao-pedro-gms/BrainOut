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
chore(gradle): atualizar Kotlin para 2.3.20
docs(roadmap): marcar E1.6 como concluído
ci(workflows): adicionar cache para ~/.gradle/caches
```

O exemplo de `chore(gradle)` cita a versão que o catálogo realmente usa:
`gradle/libs.versions.toml` fixa `kotlin = "2.3.20"` (KGP), com AGP
9.4.1 e Gradle 9.7.1. Ao escrever um commit de bump, copie a versão do
catálogo, não uma de memória.

## Estilo de código

- **Kotlin:** estilo oficial JetBrains; `ktlintCheck` é a fonte da
  verdade. Ele está aplicado aos **9 subprojetos**, mas corre hoje com
  `ignoreFailures = true` — a engine 1.x do ktlint 14 sinaliza ~180
  violações cosméticas pré-existentes e a reformatação dedicada está
  **pendente**. Isso é pendência em aberto, não licença para ignorar:
  adote o formato do arquivo que você está editando.
- **Compose:** `@Preview` em todos os componentes reutilizáveis.
- **Nomes:** `PascalCase` para tipos, `camelCase` para funções/variáveis,
  `snake_case` apenas em arquivos Gradle.
- **Strings:** 100% em `res/values/strings.xml` (e variantes localizadas).
- **IDs de view (XML, quando usado):** prefixos `et`, `til`, `btn`, `tv`,
  `iv`, `seek` para manter a tradição didática do curso.
- **Sem código comentado.** Remova antes de comitar.

## O que validar antes de abrir o PR

O comando agregado do CI é `testDevDebugUnitTest`, mas ele **não**
alcança os módulos sem flavor. Rodar só ele deixa passar uma suíte de
`:feature/*` ou `:core:ui` quebrada. Reproduza localmente o que o CI
faz:

```bash
./gradlew testDevDebugUnitTest \
          :feature:projects:testDebugUnitTest :feature:tasks:testDebugUnitTest \
          :feature:auth:testDebugUnitTest :feature:settings:testDebugUnitTest \
          :core:ui:testDebugUnitTest

./gradlew :core:domain:koverVerify :core:data:koverVerify
./gradlew :app:lintDevDebug
```

Dois limites honestos do ambiente local:

- **`detekt` não roda com o JBR 25** do Android Studio (rejeita o
  `--jvm-target` derivado do JVM do Gradle). Ele roda no CI em Temurin
  21 — uma falha de detekt local não é um reprovável seu.
- **Testes instrumentados exigem emulador** (`connectedDevDebugAndroidTest`).
  Eles compilam, mas o CI **não** os executa (não há runner com emulador
  no plano gratuito). Se você mexer em migrações Room, rode o emulador
  você mesmo — nada vai rodar isso por você.

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