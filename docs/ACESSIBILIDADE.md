# BrainOut — Acessibilidade (marco E4.4)

> Documento de referência para o requisito R11 (Acessibilidade) do
> documento norteador. Cobre os ajustes entregues no marco E4.4 do
> `ROADMAP.md` (contraste AA, áreas de toque de 48dp,
> `contentDescription` em ícones funcionais, suporte a TalkBack) e
> os itens pendentes de validação manual no dispositivo físico.

---

## 1. Método

Adotamos o padrão **WCAG 2.1 nível AA** como referência quantitativa
para os elementos visuais do app. Os critérios aplicados:

| Critério WCAG                              | Limiar                       | Onde se aplica                                      |
|--------------------------------------------|------------------------------|-----------------------------------------------------|
| 1.4.3 *Contrast (Minimum)*                 | Texto normal: ≥ 4.5:1        | Pares `on*`/fundo do `ColorScheme` Material 3       |
| 1.4.11 *Non-text Contrast*                 | Componente gráfico: ≥ 3.0:1  | `outline`, divisores, ícones funcionais              |
| 2.5.5 *Target Size (Minimum)* — AAA        | Botão clicável: ≥ 48×48dp    | `AssistChip`, `FilterChip`, controles interativos   |
| 1.1.1 *Non-text Content*                   | `contentDescription` ou `null` explícito | Todo `Icon(` e `Image(`                  |

Tokens do tema cobertos pelo `ContrastRatioTest`:

- `primary` / `onPrimary` / `primaryContainer` / `onPrimaryContainer`
- `secondary` / `onSecondary` / `secondaryContainer` / `onSecondaryContainer`
- `tertiary` / `onTertiary` / `tertiaryContainer` / `onTertiaryContainer`
- `error` / `onError` / `errorContainer` / `onErrorContainer`
- `background` / `onBackground`
- `surface` / `onSurface` / `surfaceVariant` / `onSurfaceVariant`
- `outline` (gráfico, limiar 3.0:1)

Total: **22 pares de texto (≥ 4.5:1) + 2 pares de gráfico (≥ 3.0:1) =
24 testes** em `core/ui/src/test/kotlin/.../core/ui/theme/ContrastRatioTest.kt`.

---

## 2. Status por tela

> Status atualizado pelo marco E4.4 — itens ⏳ aguardam validação manual
> do autor no dispositivo físico (S20 FE, Android 13).

### 2.1 Login / Cadastro (`feature/auth`)

| Verificação                                                | Status |
|------------------------------------------------------------|--------|
| Contraste AA nos campos `OutlinedTextField`                | ✅     |
| Ícone "olho" do toggle de senha com `contentDescription`   | ✅     |
| Botão "Entrar" / "Criar conta" — área de toque ≥ 48dp      | ✅ (botão M3 padrão = 48dp) |
| Foco volta automaticamente para o campo inválido           | ✅ (já implementado em E1.6) |
| Ordem de foco e navegação por TalkBack                      | ⏳ validar manualmente |

### 2.2 Home (`feature/projects`)

| Verificação                                                | Status |
|------------------------------------------------------------|--------|
| Bottom bar com 3 destinos e `NavigationBarItem` rotulado   | ✅     |
| `AssistChip` do badge de papel com 48dp mínimo             | ✅     |
| `AssistChip` de tag com 48dp mínimo                        | ✅     |
| `FilterChip` na seleção de tags com 48dp mínimo            | ✅     |
| FAB com `contentDescription` próprio (`Criar novo projeto`)| ✅     |
| Ordem de foco e navegação por TalkBack                      | ⏳ validar manualmente |

### 2.3 Detalhe do projeto / lista de tarefas (`feature/projects`)

| Verificação                                                | Status |
|------------------------------------------------------------|--------|
| `IconButton` de "Voltar" com `contentDescription`           | ✅ (`common_back`) |
| `IconButton` "Excluir projeto" com `contentDescription`    | ✅ (`project_detail_delete_project`) |
| `IconButton` "Mais opções" da tarefa com `contentDescription` | ✅ (`project_detail_task_menu_more`) |
| `AssistChip` de status e prioridade com 48dp mínimo        | ✅     |
| Snackbar de erro com auto-dismiss em 5s                    | ✅ (já implementado em RN01) |
| Ordem de foco e navegação por TalkBack                      | ⏳ validar manualmente |

### 2.4 Tarefas (`feature/tasks`)

| Verificação                                                | Status |
|------------------------------------------------------------|--------|
| `AssistChip` de prioridade e status com 48dp mínimo        | ✅     |
| Contraste AA em todas as combinações de chip               | ✅ (pares WCAG cobertos no teste) |
| Ordem de foco e navegação por TalkBack                      | ⏳ validar manualmente |

### 2.5 Configurações (`feature/settings`)

| Verificação                                                | Status |
|------------------------------------------------------------|--------|
| Linha clicável com 16dp de padding vertical (≥ 48dp total) | ✅     |
| Ícones decorativos (`Logout`, `ChevronRight`) ao lado de texto | ✅ (`contentDescription = null` explícito) |
| Ordem de foco e navegação por TalkBack                      | ⏳ validar manualmente |

---

## 3. Áreas de toque (48dp)

A varredura do código identificou que os componentes
`AssistChip` e `FilterChip` do Material 3 têm **~32dp de altura por
padrão**, abaixo do mínimo recomendado para alvos de toque
(WCAG 2.5.5). Os chips interativos foram ajustados com
`Modifier.heightIn(min = 48.dp)`:

- `feature/projects/.../HomeScreen.kt`:
  - `AssistChip` do badge de papel
  - `AssistChip` de tag (`TagChipView`)
  - `FilterChip` da seleção de tags (`CreateProjectTagsFlow`)
- `feature/projects/.../ProjectDetailScreen.kt`:
  - `AssistChip` de status (`StatusChip`)
  - `AssistChip` de prioridade (`PriorityChip`)
- `feature/tasks/.../TasksScreen.kt`:
  - `AssistChip` de prioridade
  - `AssistChip` de status

`IconButton` do Material 3 já garante 48dp via
`minimumInteractiveComponentSize` — não requer ajuste.

---

## 4. `contentDescription`

Varredura dos composables que usam `Icon(`, `Image(` e `IconButton(`
nos módulos `:feature:*` e `:app`:

- **Ícones funcionais** (com ação): recebem `stringResource` com a
  ação descrita — ex.: `Voltar`, `Excluir projeto`, `Mais opções`,
  `Criar novo projeto`. Já estavam cobertos; o único ícone funcional
  sem descrição era o `MoreVert` da `ProjectDetailScreen`, agora
  anotado como `R.string.project_detail_task_menu_more`.
- **Ícones decorativos** (acompanham rótulo visível): recebem
  `contentDescription = null` explícito. Exemplos: ícones da
  `NavigationBarItem` (rótulo embaixo é lido pelo TalkBack),
  ícones `Logout`/`ChevronRight` da `SettingsScreen`,
  ícone `Add` dentro do `ExtendedFloatingActionButton` (rótulo
  textual do FAB é lido pelo TalkBack).

---

## 5. TalkBack — ordem de foco

⏳ **Validação manual pendente** (João — S20 FE, Android 13).

Sequência sugerida para teste manual de cada fluxo:

1. **Login**: TalkBack deve ler "Campo de edição, e-mail" →
   "Campo de edição, senha" → "Botão, mostrar/ocultar senha" →
   "Entrar, botão" → "Criar conta, botão".
2. **Home (após login)**: saudação com nome → badge de papel
   (lê "Owner"/"Member") → lista de projetos (cada card lê nome
   + descrição + tags) → FAB "Criar novo projeto" → bottom bar
   (Projetos / Tarefas / Configurações).
3. **Detalhe do projeto**: "Voltar" → título → "Excluir projeto" →
   lista de tarefas (cada tarefa lê título + status + prioridade) →
   botão "Mais opções" por linha → FAB "Nova tarefa".
4. **Tarefas**: cards com título + projeto + chips (status +
   prioridade são lidos como "A fazer, chip" etc.).

A ordem deve seguir a sequência visual (top → bottom). Anotar
quaisquer desvios (ex.: foco pulando um botão, ou TalkBack
anunciando um botão decorativo como se fosse interativo) na
seção "Achados manuais" abaixo.

### Achados manuais

⏳ *Preencher após rodar TalkBack no S20 FE.*

---

## 6. Accessibility Scanner (varredura)

⏳ **Validação manual pendente** (João — S20 FE, Android 13).

Passo a passo para o João:

1. Instalar o app **Accessibility Scanner** pela Play Store.
2. Compilar e instalar o app `devDebug` no S20 FE
   (`./gradlew :app:installDevDebug`).
3. Abrir o Accessibility Scanner e conceder a permissão de
   sobreposição.
4. Para cada tela abaixo, tocar em "Scan" e capturar o
   resultado (print + tabela abaixo):

| Tela                    | # Alertas | Tipos                | Status |
|-------------------------|-----------|----------------------|--------|
| Login                   | ⏳        | —                    | ⏳     |
| Cadastro                | ⏳        | —                    | ⏳     |
| Home (lista vazia)      | ⏳        | —                    | ⏳     |
| Home (com projetos)     | ⏳        | —                    | ⏳     |
| Detalhe do projeto      | ⏳        | —                    | ⏳     |
| Tarefas                 | ⏳        | —                    | ⏳     |
| Configurações           | ⏳        | —                    | ⏳     |

**Tipos comuns de alerta esperados:**

- *ContentDescription* — deveria zerar após o E4.4.
- *Touch target size* — zerado após o ajuste dos chips.
- *Text contrast* — zerado (coberto por `ContrastRatioTest`).
- *Image contrast* — N/A (sem imagens raster no app até E5.x).

---

## 7. Testes automatizados

Arquivo: `core/ui/src/test/kotlin/pucgo/joaopedrogmsilva/brainout/core/ui/theme/ContrastRatioTest.kt`

Cobertura: 24 testes WCAG 2.1 (22 texto ≥ 4.5:1, 2 gráfico ≥ 3.0:1)
para todos os pares `on*`/superfície do `ColorScheme` Material 3,
em ambos os temas (claro/escuro). Algoritmo sRGB conforme
[W3C WCAG 2.1 §1.4.3](https://www.w3.org/TR/WCAG21/#dfn-contrast-ratio).

Execução: `./gradlew :core:ui:testDebugUnitTest` — roda em JVM
puro (sem Robolectric), sem dependência do ciclo de vida Android.

---

## 8. Referências

- [WCAG 2.1](https://www.w3.org/TR/WCAG21/) — W3C Recommendation.
- [Material 3 Accessibility](https://m3.material.io/foundations/accessible-design/accessibility-basics).
- `docs/ROADMAP.md` (marco E4.4).
- `core/ui/src/test/kotlin/.../core/ui/theme/ContrastRatioTest.kt`.
