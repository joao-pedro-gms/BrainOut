# Especificação — Responsividade do BrainOut (Fases 0–3)

> **Autor:** architect · **Data:** 2026-10-09 · **Card:** `t_649c1ef8`
> **Escopo:** decide *o que* construir e *como*, para que um developer implemente sem
> reabrir decisões. Não é um plano de implementação linha a linha.
> **Alvo do documento:** `docs/plans/` (ver `2026-10-01-redesign-neobrutalista.md`, mesma convenção).

---

## 0. TL;DR para quem implementa

1. **Não há bump de dependência.** O `compose-bom:2024.10.01` **já gerencia**
   `material3-window-size-class`, `material3-adaptive-navigation-suite` e o grupo
   inteiro `material3.adaptive:*`. A Fase 0 do briefing original ("adicionar
   material3-adaptive + material3-window") está **concluída de fato** — falta
   declarar no catálogo e consumir. Única dependência genuinamente nova:
   `androidx.window:window` (para *nomear* `FoldingFeature` no código).
2. **Os tokens já existem e estão testados.** `NeoLayout` (`core/ui/.../theme/NeoTokens.kt:109-119`)
   define exatamente os breakpoints de `DESIGN.md:221-228`. O trabalho é **consumir**, não projetar.
3. **A Fase 1 (shell) é a de maior retorno e o único item que é correção de bug real**
   (teclado cobrindo campos em paisagem). Faça primeiro.
4. **Fases 2 e 3 são cheap e mecânicas** — trocar literais `16.dp`/`24.dp` por
   tokens e trocar `LazyColumn` por `LazyVerticalGrid` atrás de um `when`.
5. **Fase 4 (lista/detalhe) NÃO está nesta spec** e exige decisão do João (ver §7).

---

## 1. Decisões de arquitetura (as que eu fixo como orquestrador)

Cada decisão abaixo é **final** — quem implementar não deve reabrir.

### D-01 — foundation: `NeoWindowInfo` próprio em `:core:ui`, não `calculateWindowSizeClass(activity)` espalhado

**Decisão:** criar **um** objeto em `:core:ui` que expõe a largura da janela como
uma **enum própria do projeto** (`NeoWidth`), sem vazar os tipos do AndroidX para
o resto do app.

**Por quê:** o briefing pedia `calculateWindowSizeClass(activity)`. Isso é errado
para este app por três razões concretas, todas verificadas:

- `calculateWindowSizeClass(Activity)` **não é testável sem Activity** e não é
  composable-friendly para os testes Robolectric com `qualifiers` que o repositório
  já usa (`SettingsScreenTest.kt:32`, `DashboardScreenTest.kt:36` — `@Config(sdk=[34], qualifiers=...)`).
  Precisamos de um caminho **puro** `Dp -> NeoWidth` para AC-2.
- Os breakpoints do AndroidX (`Compact/Medium/Expanded` = 600/840) coincidem hoje
  com `NeoLayout`, mas `NeoLayout` é a **fonte de verdade do design** (`tokens.json:177`
  é o contrato verificado por `verify_tokens.py`). Duas fontes de verdade para o
  mesmo número é exatamente o tipo de coisa que o `NeoTokenParityTest` existe para
  impedir, e que a auditoria DEF-10 mostrou custar caro quando os valores divergem.
- `WindowWidthSizeClass` é uma value class experimental
  (`ExperimentalMaterial3WindowSizeClassApi`, confirmado no `current.txt` do AOSP).
  Não queremos opt-in experimental (`AnnotatedOptIn`) em cada tela que só precisa
    saber "sou largo?".

**Contrato (assinatura definitiva — o developer implementa isto):**

```kotlin
// core/ui/src/main/kotlin/.../theme/NeoLayout.kt  (NOVO arquivo, ao lado de NeoTokens.kt)

/** Largura da janela disponível, classificada pelos breakpoints de NeoLayout. */
enum class NeoWidth { COMPACT, MEDIUM, EXPANDED, LARGE }

/** Altura útil: relevante só para a regra "altura < 480dp → coluna única". */
enum class NeoHeight { COMPACT, MEDIUM, EXPANDED }

@Immutable
data class NeoWindowInfo(
    val width: NeoWidth,
    val height: NeoHeight,
    val widthDp: Dp,
    val heightDp: Dp,
) {
    /** Verdadeiro quando a largura alcanza NeoLayout.mediumMinWidth (600dp). */
    val isWide: Boolean get() = width != NeoWidth.COMPACT

    /** Margem horizontal do content segundo o width bucket. */
    val margin: Dp
        get() = when (width) {
            NeoWidth.COMPACT -> NeoLayout.compactMargin    // 16.dp
            NeoWidth.MEDIUM -> NeoLayout.mediumMargin     // 24.dp
            NeoWidth.EXPANDED, NeoWidth.LARGE -> NeoLayout.expandedMargin // 32.dp
        }

    /** Alvos < 480dp de altura forçam coluna única (DESIGN.md:228). */
    val forceSingleColumn: Boolean get() = heightDp < NeoLayout.compactHeightMaxExclusive
}

/** Conversão PURA — testável sem Activity, sem Compose, sem Android. */
fun NeoWindowInfo.Companion.fromSize(width: Dp, height: Dp): NeoWindowInfo = …

val LocalNeoWindowInfo: ProvidableCompositionLocal<NeoWindowInfo> = staticCompositionLocalOf { … }
```

> ⚠️ **O developer DEVE** manter `fromSize` como função pura top-level (não método de
> companion que o AndroidX injecte). É ela que AC-2 exercita. Os breakpoints
> **precisam** vir de `NeoLayout` — não escreva `600.dp` literal em lugar nenhum.

**Onde se injeta:** `MainActivity.setContent` (`MainActivity.kt:79-95`), involve o
`BrainOutTheme` existente com `CompositionLocalProvider(LocalNeoWindowInfo provides …)`.
O valor é calculado **fora** do Compose (`onConfigurationChanged` /
`resources.configuration.screenWidthDp`), para que girar o aparelho não force
recomposição de tudo por mudança de `Configuration`.

> **Nota de arquitetura:** `enableEdgeToEdge()` já está em `MainActivity.kt:78`, mas
> **zero** tratamento de insets existe no repo (grep de `WindowInsets|imePadding|
> safeDrawing|statusBars|navigationBars` em `app/src/main`, `feature/*/src/main`,
> `core/*/src/main` → **vazio**). As `Scaffold`s (Login:92, Register:89,
> Home:82, ProjectDetail:95, Settings:56 + Tasks/Dashboard desde o DEF-22) usam o
> `ScaffoldDefaults.contentWindowInsets` padrão (= `systemBarsForVisualComponents`),
> o que é **quase** correto — falta `ime`. Ver §3, Fase 1.

---

### D-02 — shell: `NavigationSuiteScaffold`, não `NavigationBar` + `NavigationRail` manuales

**Decisão:** trocar `HomeBottomBar` por `NavigationSuiteScaffold` +
`NavigationSuiteItem`, deixando o **AndroidX escolher** bar/rail/drawer.

**Por quê:** o briefing pedia "rail a partir de 600dp" com branch manual.
`NavigationSuiteScaffold` faz exatamente isso (e drawer em medium-width) e já está
disponível via BOM — sem código de decisão nosso para manter.

> ⚠️ **CORREÇÃO (verificada por `javap` no artefato do BOM, 09/10/2026).**
> A versão anterior desta decisão afirmava que `NavigationSuiteScaffold`
> "contém o próprio `Scaffold`". **Isso é falso.** A assinatura real em
> `material3-adaptive-navigation-suite` 1.3.1 é:
>
> ```
> NavigationSuiteScaffold(navigationSuiteItems, modifier, layoutType,
>                         navigationSuiteColors, containerColor, contentColor, content)
> ```
>
> **Não existe** `topBar`, `floatingActionButton`, `snackbarHost` **nem
> `contentWindowInsets`.** Por dentro é `Surface { NavigationSuiteScaffoldLayout { … } }`
> — um `Layout` próprio que posiciona a suíte (bar embaixo / rail à esquerda), e ele
> passa `noWindowInsets` **internamente** (há um `access$getNoWindowInsets$p()` no
> bytecode): o conteúdo do suite **não recebe** insets dele.
>
> **Consequência prática:** a composição correta é o `Scaffold` da Home **dentro** do
> suite — é a única forma de manter `HomeTopBar` + `HomeFloatingActionButton` +
> `innerPadding`, que são exatamente os três slots que o suite não oferece. Como o
> suite não aplica insets, é o `Scaffold` interno que precisa de
> `contentWindowInsets = WindowInsets.safeDrawing`.

**Restrição do projeto:** os 4 destinos de `HomeTab` disparam **navegação externa**
via callback (`HomeScreen.kt:88-97`) — "Projetos" é aba interna, as outras três
saem para `TasksRoutes.TASKS`, `TasksRoutes.DASHBOARD` e `BrainOutRoutes.Settings`.
`NavigationSuiteScaffold` não muda isso: os `NavigationSuiteItem`s continuam sendo
`onClick` lambdas. ✅ Nenhuma mudança de navegação na Fase 1.

**Cores:** `HomeBottomBar.kt:29-32` hoje fixa `containerColor = surface` +
`tonalElevation = 4.dp`. `NavigationSuiteScaffold` usa
`containerColor`/`contentColor`. **Manter o visual neobrutalista**: passar
`containerColor = MaterialTheme.colorScheme.surface`. Não adotar os defaults
Material (que são `surfaceContainer`, tonal).

---

### D-03 — largura: `contentMaxWidth` e `formMaxWidth` são **caps**, não larguras

**Decisão:** `widthIn(max = NeoLayout.formMaxWidth)` **centrado** nos formulários;
`widthIn(max = NeoLayout.contentMaxWidth)` nos containers de conteúdo.

**Por quê:** os dois tokens estão em `NeoTokens.kt:117-118` e `tokens.json:177`.
`formMaxWidth=480dp` casa exatamente com a linha de texto do `bodyLarge`
(`Public Sans 16/24`), o que é a razão de design do valor. `contentMaxWidth=1200dp`
implementa `DESIGN.md:226` ("mais espaço não implica cartões gigantes").

Detalhe que importa: o `Modifier` de `widthIn` **precisa vir antes** do
`fillMaxWidth` na cadeia, senão o `fillMaxWidth` ganha. Padronizar em
`.fillMaxWidth().widthIn(max = …).align(CenterHorizontally)` — o `widthIn` com
`max` menor que o disponível **encolhe** o elemento, e o `align` centraliza dentro
do pai.

---

### D-04 — listas: `LazyVerticalGrid` só ≥600dp, e `GridCells.Adaptive(260.dp)` com **guarda de token**

**Decisão:** `LazyColumn` abaixo de 600dp; `LazyVerticalGrid(columns = GridCells.Adaptive(260.dp))`
acima. O `260.dp` vem de `DESIGN.md:224` ("2 colunas se cada coluna tiver ≥260dp").

**Cuidado:** `260.dp` **não** é um token em `NeoTokens.kt`. Duas opções:
(a) adicionar `NeoLayout.gridMinColumnWidth = 260.dp` (e espelhar em `tokens.json`),
ou (b) usar `Adaptive(minSize = NeoLayout.mediumMargin * 11)`. **Escolho (a)** —
é explícito, testável, e `tokens.json` já tem uma chave `layout` para receber. **O
developer deve** adicionar o token e **atualizar `docs/design/tokens.json` e
`NeoTokenParityTest.kt` juntos** (é o contrato que `verify_tokens.py` checa).

---

### D-05 — `androidx.window:window` é dependência nova: justificativa e versão

**Decisão:** adicionar `androidx.window:window = 1.5.1` (a mais recente **estável**;
1.6.0-alpha* rejeitado por ser alpha) ao catálogo, **apenas** em `:core:ui`.

**Por quê é nova:** o BOM `2024.10.01` **não** gerencia o grupo `androidx.window`
(grep no POM → **0 matches**). O `adaptive:1.0.0` puxa `window`/`window-core 1.3.0`,
mas **em escopo `runtime`** — verifiquei o Gradle module metadata: os dois deps
aparecem em `releaseApiElements-published` sem `runtimeOnly`, ou seja entram na API
… mas o `window-core-android:1.3.0` AAR contém **apenas 7 classes**
(`WindowSizeClass`/`WindowWidthSizeClass`/`WindowHeightSizeClass` + `ExperimentalWindowCoreApi`)
e **nenhuma** `WindowLayoutInfo`/`FoldingFeature` — essas vivem em `androidx.window:window`
(confirmado: `window:1.5.1` tem 262 classes, incluindo `WindowLayoutInfo`,
`FoldingFeature`, `FoldingFeature$State`, `WindowInfoTracker`).

Portanto: se o código do projeto **nomear** `FoldingFeature` ou `WindowLayoutInfo`
(precisa, para AC-3), a dependência é obrigatória. `1.3.0` (o que o adaptive puxa)
também tem as classes e funcionaria — mas `1.5.1` é estável e `1.3.0` é de 2023.
**Declarar `1.5.1` é um pin explícito de 2 minors acima do mínimo, com a API
idêntica.**

⚠️ **Nota de escopo:** declarar `window` **transitivamente expõe a API AndroidX
de dobra**. Fique **confinado a `:core:ui`**, atrás de `NeoWindowInfo`.
Nenhum `feature/*` deve importar `androidx.window.*` diretamente — isso mantém o
blast radius de `:core:ui` (que todo mundo consome) controlado.

---

## 2. Matriz de conformidade: briefing vs. verificação

| Afirmação do briefing | Verificado? | Nota |
|---|---|---|
| Zero adaptatividade no repo | ✅ **confirmado** | grep de `WindowSizeClass`, `BoxWithConstraints`, `LazyVerticalGrid`, `NavigationRail`, `values-w600dp`, `dimens.xml` → tudo vazio |
| `NeoLayout` existe em `NeoTokens.kt:109-119` e não é usado em produção | ✅ **confirmado** | únicas refs: `NeoTokenParityTest.kt:137-145` e `tokens.json:177` |
| "material3-adaptive + material3-window" precisam ser adicionados | ❌ **CORRIGIDO** | o BOM já os gerencia (D-05) |
| `HomeBottomBar.kt:29` sempre `NavigationBar` | ✅ **confirmado** | e ignora `navMinHeight=80.dp` |
| Login/Register sem `widthIn(max=formMaxWidth)` | ✅ **confirmado** | `LoginScreen.kt:136-141`, `RegisterScreen.kt:167-172` — `padding(horizontal=24.dp)`, sem cap |
| Listas coluna única | ✅ **confirmado** | `HomeProjectsContent.kt:155`, `ProjectDetailBody.kt:87`, `TasksScreen.kt:~250` ⚠️ ver nota |
| Alturas fixas estouram em paisagem | ✅ **confirmado** | 360/280/240/160dp — lista completa em §3 |
| Zero insets apesar de `enableEdgeToEdge()` | ✅ **confirmado + agravante** | todas as `Scaffold`s usam o default (sem IME), **inclusive as novas do DEF-22** |
| `DashboardPriorityChart` canvas não cresce | ✅ **confirmado** | `DashboardPriorityChart.kt:112` `.height(160.dp)` |
| **Tasks/Dashboard não tinham `Scaffold`** | ⚠️ **corrigido em campo — VER NOTA ABAIXO** | VER NOTA ABAIXO |

---

> ### ⚠️ NOTA DE REVERIFICAÇÃO (2026-10-09, pós-escrita)
>
> Enquanto esta spec era escrita, um worker concorrente mergeou o **DEF-22** em
> `:feature:tasks`, que adicionou `Scaffold` + `TasksTopBar` a `TasksScreen`
> (`TasksScreen.kt:81-97`) e a `DashboardScreen` (`DashboardScreen.kt:63-79`).
>
> **O que muda nesta spec:**
> - O passo **Fase 1.2** ("Tasks/Dashboard ganham um `Scaffold`") está
>   **obsoleto — já feito**. Apagado abaixo.
> - **O defeito de insets NÃO foi corrigido pelo DEF-22**: o `Scaffold` novo não
>   define `contentWindowInsets` e `grep` de `WindowInsets|imePadding|safeDrawing`
>   em `feature/tasks/src/main/kotlin/` continua **vazio**. O teclado em Tasks/Dashboard
>   **ainda** cobre o conteúdo. O passo **1.1** (insets) continua válido e agora
>   alcança 7 telas, não 5.
> - **Números de linha citados para `:feature:tasks` nesta spec desatualizaram.**
>   Re-verifique antes de usar: `TasksScreen.kt:245` (era 208) e
>   `DashboardScreen.kt:110-112` (era 99-104) para os paddings.
>
> Os demais achados (`NeoLayout` sem uso, zero insets no repo, alturas fixas,
> `HomeBottomBar` sempre `NavigationBar`) **permanecem válidos** — foram
> re-verificados contra o working tree após o DEF-22.

---

## 3. Fases, critérios de aceitação e ordem

Cada fase é **independente e shippável**. Nenhuma depende de uma posterior.
Ordem é por (retorno funcional) ÷ (risco).

---

### FASE 0 — Fundação de janela (`:core:ui`)

**Entrega:** `NeoWindowInfo` (D-01) + `LocalNeoWindowInfo` + `androidx.window:window:1.5.1`.

**Arquivos tocados:** `core/ui/.../theme/NeoLayout.kt` (novo),
`gradle/libs.versions.toml`, `core/ui/build.gradle.kts`.

**Por que primeiro:** tudo Brancha nisso. Nada aqui é visual — **zero**
risco de regressão perceptual.

**AC:**
- **AC-0.1** `NeoWindowInfo.fromSize()` é função pura e tem teste de tabela em
  `:core:ui` cobrindo: 0dp→COMPACT, 599dp→COMPACT, 600dp→MEDIUM, 839dp→MEDIUM,
  840dp→EXPANDED, 1199dp→EXPANDED, 1200dp→LARGE. **Os valores esperados são
  derivados de `NeoLayout`, não hardcoded no teste.**
- **AC-0.2** `heightDp < 480.dp` ⇒ `forceSingleColumn == true`; `480.dp` ⇒ false
  (o token é `compactHeightMaxExclusive`, exclusivo).
- **AC-0.3** `LocalNeoWindowInfo` tem valor default seguro (COMPACT) para não
  quebrar preview/testes que não a fornecem.
- **AC-0.4** `./gradlew :core:ui:testDebugUnitTest` verde + `ktlintCheck` verde.

**Não fazer:** não introduzir `BoxWithConstraints` ainda. `fromSize` é o suficiente.

---

### FASE 1 — Shell adaptativo + insets ⭐ *fase de maior retorno*

**Entrega:** `HomeBottomBar` → `NavigationSuiteScaffold` (D-02); insets de IME
aplicados **uma vez** no shell; Tasks/Dashboard deixam de ser alvos órfãos (§2, bônus).

**Arquivos tocados:** `feature/projects/.../HomeBottomBar.kt`,
`feature/projects/.../HomeScreen.kt`, `feature/tasks/.../TasksScreen.kt`,
`feature/tasks/.../DashboardScreen.kt`, `app/.../MainActivity.kt`.

**Ordem interna (importante — são 2 passos porque `NavigationSuiteScaffold` aninha `Scaffold`):**

1. **1.1 — Insets primeiro, isolado.** Adicionar `imePadding()` às telas com campos
   de texto (`Login`, `Register`) **e** `contentWindowInsets = WindowInsets.safeDrawing`
   às `Scaffold`s. Isto sozinho corrige AC-4 (teclado em paisagem) e AC-5 (nada sob
   status bar/cutout) sem tocar em navegação. *Mergeável sozinho.*
   **Escopo: 7 telas** — Login, Register, Home, ProjectDetail, Settings **e** Tasks +
   Dashboard (os dois últimos passaram a ter `Scaffold` no DEF-22, sem
   `contentWindowInsets`). *Mergeável sozinho e é o passo de maior retorno.*
2. **1.2 — `HomeBottomBar` → `NavigationSuiteScaffold`.** `HomeScreen.kt:82` já tem
   um `Scaffold` com `topBar`/`bottomBar`/`floatingActionButton`.
   **O suite NÃO é um scaffold** (ver correção em D-02): ele não tem `topBar`,
   `floatingActionButton` nem `contentWindowInsets`. A composição correta é o
   `Scaffold` da Home **dentro** do `NavigationSuiteScaffold` — trocar a
   `bottomBar = { HomeBottomBar(...) }` por `NavigationSuiteScaffold(navigationSuiteItems = {…}) { Scaffold(...) { content } }`.
   **Atenção:** o suite passa `noWindowInsets` internamente, então o `Scaffold`
   interno é quem precisa de `contentWindowInsets = WindowInsets.safeDrawing`.

> ~~**Passo 1.3 (antigo):** Tasks/Dashboard ganham um `Scaffold`.~~ **CANCELADO** —
> o DEF-22 já os deu (`TasksScreen.kt:81-97`, `DashboardScreen.kt:63-79`). O que
> falta é o `contentWindowInsets`, coberto pelo passo 1.1 acima.

**AC:**
- **AC-1.1 (o bug real)** Em paisagem (<480dp de altura) com o teclado aberto, o
  campo de senha de Login e o botão de Register permanecem **totalmente visíveis**.
  Teste: `LoginScreenTest` com `@Config(qualifiers = "w800dp-h400dp")` + assert
  `onNodeWithTag(...).assertIsDisplayed()` no botão. *(O `feature/auth` já tem
  `androidTestImplementation(libs.androidx.compose.ui.test.junit4)` —

  > ⚠️ **CORREÇÃO (medida, 09/10/2026).** O teste acima **não prova o que o AC pede.**
  > No Robolectric **todo inset de janela é zero**: duas sondas em `:feature:auth`
  > (`@Config(sdk=[34], qualifiers="w800dp-h400dp")`) devolveram
  > `imeBottom=0 safeBottom=0 sysBottom=0`, e nem um
  > `view.rootView.dispatchApplyWindowInsets(...)` com `Type.ime()` de 240px chegou
  > ao `WindowInsets.ime` do Compose (`WindowInsetsHolder` lê
  > `View.getRootWindowInsets()`, que o shadow não popula).
  >
  > Ou seja: `imePadding()` é **no-op** em Robolectric — um teste de "campo acima do
  > teclado" passaria **antes e depois** da correção. Isso não é teste de regressão,
  > é ruído que dá falsa confiança.
  >
  > **O que continua verificável em JVM** (e deve ser o conteúdo do
  > `LoginScreenTest`): layout em `w800dp-h400dp` com `assertIsDisplayed()` no campo
  > de senha e no botão de submit, provando que o **layout** de paisagem comporta os
  > dois. O deslocamento causado pelo IME fica para **QA manual** em
  > `docs/ROTEIRO-TESTES.md` — mesmo tratamento que o AC-C já recebe (§5).
  >
  > Alternativa não recomendada: instrumentado no emulador do CI. Hoje
  > `connectedAndroidTest` **não roda** no pipeline, e o `LoginScreenTest` que existe
  > em `feature/auth/src/androidTest` é teste morto por esse motivo.
  `feature/auth/build.gradle.kts:116`.)*
- **AC-1.2** A 599dp de largura a navegação é `NavigationBar`; a 600dp é
  `NavigationRail`. Verificável por `qualifiers` sem device.
- **AC-1.3** Os **4 destinos** e o **FAB** continuam funcionando exatamente como
  antes (Projetos é interno; Tarefas/Painel/Configurações saem para fora).
  Regressão: os testes de navegação existentes em `:app` continuam verdes.
- **AC-1.4** **Os `testTag`s existentes são contrato** — `HomeTestTags`,
  `TasksTestTags`, `DashboardTestTags` não mudam de nome nem desaparecem.
  *(Os tags de `NavigationSuiteItem` precisam continuar encontráveis: os testes
  existentes fazem `onNodeWithTag(HomeTestTags.TAB_TASKS)`.)*
- **AC-1.5** Nada renderizado sob status bar, nav bar ou cutout — em
  `qualifiers = "w800dp-h480dp-notround"` o topo do `topBar` tem y ≥ topo do inset.

**Risco:** **ALTO** — toca 4 módulos. Mitigação: os 2 passos são separáveis;
fazer 1.1 (insets) em commit separado de 1.2 (navegação) — só 1.2 toca navegação.

---

### FASE 2 — Largura: consumir `NeoLayout` (D-03)

**Entrega:** caps de largura + margens por token no lugar de literais.

**Arquivos tocados:** `LoginScreen.kt`, `RegisterScreen.kt`, `SettingsScreen.kt`,
`ProjectDetailScreen.kt`, `HomeProjectsContent.kt`, `TasksScreen.kt`,
`DashboardScreen.kt`.

**Regra única:** nenhum `padding(horizontal = 16.dp)` / `24.dp` sobrevive onde
haja conteúdo de tela cheia. Substituir por `padding(horizontal = LocalNeoWindowInfo.current.margin)`.

**AC:**
- **AC-2.1** A 600dp de largura, um `OutlinedTextField` de Login mede **≤ 480dp**
  de largura (`assertWidthIsAtMost`), com o bloco **centralizado**.
- **AC-2.2** A 599dp o mesmo campo mede a largura disponível menos 2×16dp
  (margem compacta) — prova que o `margin` muda junto com o bucket.
- **AC-2.3** A 1400dp, nenhum container de conteúdo excede **1200dp**
  (`contentMaxWidth`).
- **AC-2.4** Nenhum literal `16.dp`/`24.dp`/`32.dp` de **margem horizontal de
  tela** permanece nos 7 arquivos. *(Padding interno de componente — ex.
  `Card` interno — está fora do escopo e pode ficar.)*
- **AC-2.5** Os `testTag`s de todos os 7 arquivos continuam presentes e
  alcançáveis.

**Risco:** **BAIXO** — puramente visual, nenhuma mudança de comportamento.

---

### FASE 3 — Listas em grade (D-04)

**Entrega:** `LazyVerticalGrid` ≥600dp nas 3 listas.

**Arquivos tocados:** `HomeProjectsContent.kt` (lista de projetos),
`ProjectDetailBody.kt` (tarefas do projeto), `TasksScreen.kt` (lista global).

**AC:**
- **AC-3.1** A 599dp: `LazyColumn` (1 coluna). A 840dp: **2 colunas** de projetos,
  cada uma com largura **≥260dp**. Verificável por `qualifiers` contando nós por
  posição horizontal, ou mais simples: contar `ProjectCard`s por linha.
- **AC-3.2** `gridMinColumnWidth = 260.dp` existe em `NeoLayout`,
  em `docs/design/tokens.json` e é **assertado** em `NeoTokenParityTest.kt`
  (os três, juntos — é o contrato que `verify_tokens.py` verifica).
- **AC-3.3** Alturas fixas `360.dp`/`280.dp`/`240.dp` **não** são usadas para
  dimensionar *cards de conteúdo* em grid; estados vazio/loading ocupam o espaço
  disponível (`fillMaxSize()` dentro de um pai com peso, ou `heightIn(min=…)`).
  Os `height(8.dp)` de `Spacer` e `height(20.dp)`/`height(48.dp)` de ícone/progresso
  **permanecem** — não são o defeito.
- **AC-3.4** O `LazyColumn` de `ProjectDetailBody.kt:87` mantém `.weight(1f)` e
  `contentPadding(bottom = 80.dp)` — remover o `contentPadding` quebraria o
  scroll sob o FAB.

**Risco:** **MÉDIO** — trocar `LazyColumn` por `LazyVerticalGrid` muda chaves de
paginamento virtual. Bug clássico: item "some" ao rolar. Mitigação: manter
`key = { it.project.id }` / `key = { it.task.id }` **idênticos**.

---

### FASE 4 — ❌ NÃO NESTA SPEC (decisão de produto, ver §7)

---

## 4. Contrato de teste (infraestrutura compartilhada)

**Constatação verificada:** `:feature:tasks` e `:feature:settings` **já têm**
`testImplementation(libs.androidx.compose.ui.test.junit4)` + `robolectric` +
`androidx.test.core` e usam `@Config(sdk = [34], qualifiers = "pt-rBR")`
(`feature/tasks/build.gradle.kts:30-34`, `feature/tasks/src/test/.../DashboardScreenTest.kt:36`).
**`:core:ui` NÃO tem** nenhuma dep de `compose.ui.test`
(`grep -c "compose.ui.test" core/ui/build.gradle.kts` → 0).

| Fase | Onde testar | Dep necessária |
|---|---|---|
| 0 | `:core:ui` (teste **puro**, sem Compose) | nenhuma — `truth` já existe |
| 1 | `:feature:auth` **androidTest** (já tem a dep, `:feature:auth/build.gradle.kts:116`) | nenhuma |
| 1.1 (Tasks/Dashboard) | `:feature:tasks` (tem a dep) | nenhuma |
| 2 | `:feature:auth` androidTest / `:feature:settings` | nenhuma |
| 3 | `:feature:projects` — **precisa adicionar** `compose.ui.test.junit4` + `robolectric` + `androidx.test.core` + `espresso.core`, espelhando `:feature:tasks:30-34` | **é a única infra nova** |

**Regra de qualifiers:** usar `qualifiers = "w<W>dp-h<H>dp"` (ex.: `w599dp-h800dp`,
`w600dp-h800dp`, `w840dp-h1200dp`). O `robolectric`/`androidx.test.core` do
repositório já resolve `dp` → pixels. `sdk = [34]` é a convenção do repo (ver
`AGENTS.md` e os 4 testes existentes) — **mantenha**.

**Contrato de corrimão:** `./gradlew test` não pode **perder** nenhum dos 662 testes
base (constraint herdada do card pai `t_054c5db6`).

---

## 5. Critérios de aceitação transversais (mantidos do briefing, validados)

| # | Critério | Verificável? |
|---|---|---|
| AC-A | resize/dobra sem reiniciar; estado em `rememberSaveable` | ⚠️ **parcial** — `Activity` não declara `configChanges`, então o sistema **recompõe**; ViewModels sobrevivem (Hilt). Verificar com teste, não assumir |
| AC-B | breakpoints com testes Compose por `qualifiers` | ✅ sim |
| AC-C | nenhum elemento cruza a dobradiça (`getBoundsInRoot`) | ⚠️ **precisa device/foldable** — em Robolectric não há `FoldingFeature` real. Testar a *lógica* (que a área útil é calculada), não o pixel |
| AC-D | teclado em paisagem mantém campo e botão visíveis | ⚠️ **só QA manual** — Robolectric devolve inset zero, então `imePadding()` é no-op em JVM (ver correção em AC-1.1). O que o teste prova é o *layout* de paisagem, não o deslocamento do IME |
| AC-E | nada sob status/nav bar/cutout | ✅ sim (AC-1.5) |
| AC-F | alvos ≥48dp, contraste ≥4.5:1 mantidos | ✅ não regredir (`:core:ui` tem testes de contraste) |
| AC-G | build+lint+testes verdes; `testTag`s existentes são contrato | ✅ sim |

**Advertência honesta sobre AC-C:** o critério original ("nada cruza a dobradiça",
verificável por `getBoundsInRoot`) **não é testável em Robolectric**. Ele exige um
emulador de foldable ou um dispositivo real. Deve ficar como **QA manual** em
`docs/ROTEIRO-TESTES.md`, não como AC automatizado. Dizer isso agora é mais barato
do que descobrir no meio da Fase 4.

---

## 6. Non-goals (recomendado **não** fazer)

| Não fazer | Por quê |
|---|---|
| **Migração para Navigation 3** | O app usa **Navigation Compose 2.8.4** com `NavHost` clássico (`BrainOutNavHost.kt:54`). `Navigation 3` é explicitamente fora do escopo. `NavigationSuiteScaffold` não exige mudança de navegação. |
| **ListDetailPaneScaffold / PaneScaffold** (Fase 4) | Requer decisão de produto (§7). |
| **Bump do `compose-bom`** | Não é necessário (D-05). Bump agora arrastaria `adaptive` para 1.1/1.2, que exigem Compose **1.8/1.9** contra os **1.7.5** do BOM atual — um salto de toolchain não justificado por esta frente. |
| **Bump de `compileSdk`/`targetSdk`** | Já 37. Sem necessidade. |
| **Novos recursos `values-w600dp` / `dimens.xml`** | Seria um segundo sistema de verdade competindo com `NeoLayout`. |
| **`BoxWithConstraints` como primitivo de layout** | Tentador, mas quebra a testabilidade por `qualifiers` e o "sempre a janela, nunca o aparelho" do `NeoLayout`. `fromSize` é melhor. |
| **Reescrever o tema / tokens** | `NeoLayout` está correto e testado. Consumir, não redesenhar. |

---

## 7. Fase 4 — a decisão que é do João, não minha

O briefing é honesto ao dizer que a Fase 4 "muda a forma de navegação". Meu
posicionamento técnico:

- `DESIGN.md:225` diz, para ≥840dp: *"lista/detalhe lado a lado como **evolução**,
  **sem inventar nova rota de tarefa**"*.
- O app **não tem** rota de tarefa (`TaskEntity` existe, mas `BrainOutRoutes` só tem
  Splash/Login/Register/Home/Settings/ProjectDetail + `TasksRoutes.TASKS/DASHBOARD`).
- `ListDetailPaneScaffold` está disponível (BOM, `adaptive-layout:1.0.0`), mas exige
  um **estado de pane selecionado** (`ListDetailPaneScaffoldValue`) que é estado de
  produto novo — não é layout, é navegação.

**Recomendação:** Fases 0–3 primeiro (não tocam navegação, são puras). Fase 4 vira
card próprio, com a pergunta explícita: *em tablet, `ProjectDetail` abre como pane
ao lado da lista de projetos, ou continua tela cheia com o tablet wasting space?*
Se a resposta for "continua tela cheia", a Fase 4 inteira é **não-objetivo** e a
Fase 3 já resolve 90% do sintoma.

---

## 8. Riscos e blast radius

| Fase | Módulos | Blast radius | Risco |
|---|---|---|---|
| 0 | `:core:ui` + catálogo | **todo app consome `:core:ui`** — mas só *adiciona*, não muda | 🟢 baixo |
| 1 | `:feature:projects`, `:feature:tasks`, `:app` | navegação + shell | 🔴 **alto** |
| 2 | 7 telas em 4 módulos | visual | 🟢 baixo |
| 3 | 3 listas | listas + navegação virtual | 🟡 médio |

**`:core:ui` é o módulo de maior raio** (todos consomem). A Fase 0 só **acrescenta**
um arquivo novo e uma dep — **não toca** `NeoTokens.kt`, `Color.kt`, `Type.kt` nem
`NeoTheme`. Isso é deliberado: um `NeoLayout` *alterado* quebraria o
`NeoTokenParityTest` e o `verify_tokens.py` de uma vez.

**Contenção de `androidx.window`:** por D-05, `FoldingFeature` só pode ser nomeado
dentro de `:core:ui`. Nenhum `feature/*` importa `androidx.window.*` — isso vira um
critério de revisão.

---

## 9. Gates do repo que o developer vai bater (verificados, não lembrados)

- `ktlint` e `detekt` **BLOQUEIAM** (`build.gradle.kts:148-153`,
  `ignoreFailures.set(false)`). ⚠️ `AGENTS.md:169` **diz o contrário** e está
  *stale* — é DEF-10 já fechado. Não confie na doc.
- **Temurin JDK 21** (JBR 25 quebra detekt 1.23.7). `gradle/gradle-daemon-jvm.properties`
  diz `toolchainVersion=25` — divergência a checar antes do primeiro build.
- Baseline: **662 testes**, nenhum pode ser perdido.
- Kover 60% em `:core:domain` e `:core:data` (não em `:core:ui`).
- Commits conventional: `feat(scope): …`.

---

## 10. Ordem de implementação recomendada

```
Fase 0  (fundação, isolada, sem risco visual)
  └─ Fase 1.1 (insets — CORRIGE O BUG DO TECLADO, mergeável sozinho)
       └─ Fase 1.2 (NavigationSuiteScaffold)   ← maior risco, fazer por último na fase
                 └─ Fase 2 (largura)
                      └─ Fase 3 (grade)
```

Cada nó é um PR. Nada abaixo depende de algo acima estar *completo*, apenas
*disponível*.