# Pesquisa — neobrutalismo e ecossistema Android

**Consulta: 01/10/2026.** Este é o caderno de evidências da [proposta BrainOut](../../DESIGN.md). Razão de existir: justificar escolhas visuais e dependências antes de investir na implementação.

## 1. Método e limites

Pesquisa por leitura direta de fontes web, documentação oficial, código upstream e API de releases GitHub via `gh`. Context7 foi usado para Compose, Lottie Android e Rive Android, e as recomendações foram cruzadas com fontes upstream. Não houve ferramenta de busca geral/ranking, navegador visual ou subagentes disponíveis nesta sessão; a cobertura é curadoria aprofundada, não censo de toda a web.

Sites extraídos como texto não provam aparência renderizada. Onde necessário, o código CSS/TSX verificou a mecânica visual. Nenhum desempenho, tamanho de APK ou conformidade integral foi medido. Licenças citadas são as declaradas nos repositórios/fontes consultados; licença de runtime não cobre automaticamente assets, editor comercial ou marca.

Datas de release abaixo vieram de `/repos/{owner}/{repo}/releases/latest`, com `prerelease=false`. A release «latest» definida no GitHub é um snapshot upstream, não garantia de publicação Maven ou compatibilidade com BrainOut. Não fixar versões no catálogo sem verificar POM, dependências resolvidas, minSdk e build release.

## 2. Referências de estilo e tradução para mobile

### R01 — Nielsen Norman Group: Brutalism and Antidesign

Kate Moran, publicado em 05/11/2017. [Artigo](https://www.nngroup.com/articles/brutalism-antidesign/). **Leitura integral acessível.**

O texto distingue brutalismo cru/austero de antidesign disorientador e recomenda restringir a ruptura estética ao visual, preservando hierarquia, navegação e interação. É referência de UX/história do brutalismo digital, **não** uma definição de neobrutalismo de 2026 nem pesquisa com usuários BrainOut.

**Aplicar:** affordances fortes, conteúdo reconhecível, menus e ações convencionais. **Evitar:** retirar hierarquia, dificultar execução ou usar animação desnecessária para parecer distinto. A relevância para um gerenciador de tarefas é maior que a de landing pages de entretenimento.

### R02 — Michał Malewicz: Neubrutalism is taking over the web

[Artigo Hype4/SquarePlanet](https://hype4.academy/articles/design/neubrutalism-is-taking-over-web). **Título, autor, introdução e imagem acessíveis; restante sob paywall.** Não foi verificada a data original. **Reverificação 02/10/2026:** a URL respondeu 404 na primeira tentativa e 200 na segunda, no mesmo dia — o link é instável, não morto; se voltar a falhar, tratar como indisponível sem descartar a referência já registrada.

A introdução descreve combinação de visuais caóticos e boa tipografia, contraposta a arredondamentos/sombras suaves/gradientes. A frase sobre pessoas se cansarem após 6–7 anos é opinião do autor; não usar como justificativa empírica de redesign. A pesquisa não afirma ter lido a seção paga.

**Aplicar:** contraste de massas e tipografia vigorosa. **Adaptar:** retirar caos de listas e formulários. **Não copiar:** a extrapolação de que uma tendência melhora retenção ou produtividade.

### R03 — Samuel Brežnjak / ekmas: Neobrutalism components

[Introdução](https://www.neobrutalism.dev/docs), [styling](https://www.neobrutalism.dev/styling), [repositório](https://github.com/ekmas/neobrutalism-components), [CSS](https://raw.githubusercontent.com/ekmas/neobrutalism-components/main/src/styling/globals.css), [botão](https://raw.githubusercontent.com/ekmas/neobrutalism-components/main/src/components/ui/button.tsx). **Conteúdo e código acessíveis.** MIT declarado na documentação/README.

É uma coleção **web**, React/Tailwind, baseada em shadcn/ui/Base UI; não uma biblioteca Android. O CSS consultado define raio 5px, offsets 4px/4px e sombra com blur/spread zero. O botão combina borda 2px, sombra rígida e translação que remove a sombra no hover. A documentação descreve uma mistura de brutalismo e padrões contemporâneos de tipografia/ilustração/animação.

**Aplicar:** gramática coerente para button, badge, input, card, skeleton e dialog. **Traduzir:** hover para press/foco, px para dimensões dp concebidas para mobile, feedback sem mover a hitbox. Não portar alturas web 32/40px como alvos Android. Não importar npm, Radix, shadcn, Tailwind ou WebView no app.

### R04 — Gumroad: referência de produto em produção

[Home](https://gumroad.com/). **Página e links de assets acessíveis.** O site combina conteúdo orientado a ação com ilustrações próprias e mensagem direta. Esta consulta textual confirmou estrutura, CTAs e assets, não todas as cores/sombras atuais. Não houve captura visual em browser.

**Aprendizado:** personalidade pode conviver com uma próxima ação simples. A dimensão promocional de uma home de vendas não deve dominar uma lista de tarefas usada diariamente. Não copiar a identidade Gumroad; números comerciais e depoimentos do site são irrelevantes para BrainOut e não entram na proposta.

### R05 — Android Developers: Add shadows in Compose

[Documentação oficial](https://developer.android.com/develop/ui/compose/graphics/draw/shadows). **HTTP 200, conteúdo lido com fallback IPv4.** Possui seção explícita «Create neobrutalist shadows»: contornos espessos, cores vivas, sombra sem blur e offset distinto.

`shadow()` representa elevação e não expõe blur/offset como necessários. A documentação atual mostra `dropShadow(Shadow(radius=0.dp, spread=0.dp, offset=...))`. É uma evidência técnica de viabilidade nativa, não uma API existente na BOM antiga do BrainOut. Na base atual, desenhar a forma deslocada atrás do conteúdo com `drawWithCache`/Canvas e reservar espaço; migração para `dropShadow` depende de atualizar Compose de forma coordenada.

### Síntese crítica

| Característica recorrente | Benefício pretendido | Risco mobile | Decisão BrainOut |
|---|---|---|---|
| Contorno escuro espesso | Delimitar interação | Peso excessivo/ruído | 2dp para controle, 1dp para metadado |
| Sombra rígida deslocada | Feedback tátil visual | Corte/overdraw em listas | Somente ações e cartões clicáveis destacados |
| Campos saturados | Reconhecimento de marca | Fadiga e conflito semântico | Acentos por papel, neutros na leitura |
| Tipo grande/pesado | Hierarquia | Quebra de títulos/formulários | Archivo nos títulos, corpo Public Sans |
| Ilustração gráfica | Identidade | Asset genérico, custo de motion | Poucos vetores originais locais |
| Irregularidade | Expressividade | Desorientação | Só arte decorativa; grid de conteúdo estável |

«Bloco de ação» aproveita a expressividade do estilo sem tratar antidesign como requisito. Não há evidência consultada de que esse estilo, sozinho, aumente produtividade ou conversão.

## 3. Matriz de bibliotecas

Custos abaixo são avaliações qualitativas da integração, não medições: baixo = reaproveita stack/asset local; médio = nova dependência e QA; alto = runtime/pipeline e adaptação importantes.

| Recurso / licença verificada | Manutenção observada | Adequação e custo | Decisão |
|---|---|---|---|
| **Compose Foundation + Animation / Material 3** — Apache-2.0 no POM da BOM | Linha AndroidX oficial; versão local mapeada no POM | Nativo; baixo. `AnimatedVisibility`, `AnimatedContent`, `Animatable`, `updateTransition`, Canvas e Foundation para interação | **Adotar como base**, com tokens/wrappers próprios |
| **Material 3 Adaptive** — família AndroidX; POM da BOM declara Apache-2.0 | BOM local associa Adaptive 1.0.0; docs atuais evoluíram | Nativo; médio. Rail, classes de janela e layouts responsivos | **Avaliar para migração adaptativa**; não copiar API de documentação nova sem checar versão |
| **Lottie Android / lottie-compose** — Apache-2.0 | `v6.7.1`, 31/10/2025; README declara manutenção noturna/fins de semana e patrocínio | Android/Compose; médio. After Effects/Bodymovin JSON; integrar um asset local, cache e fallback | **Avaliar primeiro** para ilustração animada; sem necessidade para botões |
| **Rive Android** — MIT | `11.13.0`, 01/10/2026; runtime oficial e release com fixes/performance | Android; alto. `.riv`, state machines, renderer/native ABIs, pipeline de autoria | **Avaliar só se interação gráfica justificar**; não é base dos formulários |
| **Compottie** — MIT | `2.3.2`, 27/09/2026; README traz tabela CMP e engine Kotlin própria | Compose Multiplatform; médio. Outro renderer Lottie, suporte de features deve ser confrontado com o asset | **Alternativa se KMP virar requisito**; não combinar com Lottie Android sem motivo |
| **Compose Unstyled** — MIT | `2.10.0`, 18/09/2026; upstream modular e documentação própria | Compose Foundation/renderless; médio. Promete lógica/semântica sem impor aparência | **Avaliar em spike de um componente** se Material impedir o desenho; «fully accessible» é alegação upstream, não aceite automático |
| **Vico** — Apache-2.0 | `v3.3.1`, 28/08/2026; release corrige crash de Canvas em Android 10 | Compose Multiplatform; médio. Gráficos extensíveis, eventual série temporal/zoom | **Avaliar se métricas crescerem**; cinco barras atuais continuam Canvas |
| **Kizitonwose Calendar** — MIT | `2.10.1`, 28/03/2026; tabela Compose/release e exemplos | Android Compose; médio. Células/semana/mês customizáveis; usa `java.time` | **Avaliar para agenda real**; DatePicker existente basta para escolher prazo |
| **Coil 3** — Apache-2.0 | `3.6.3`, 18/09/2026; README e API de release concordam | Android/CMP; médio. Imagens com cache, módulo de rede separado | **Avaliar quando houver fotos/imagens**; perfil atual só iniciais, vetores locais não exigem Coil |
| **Compose Shimmer** — Apache-2.0 | `v1.5.0`, 09/07/2026; tabela exige Compose CMP 1.11 | Compose; médio. Gradiente animado de carregamento | **Evitar na primeira migração**: skeleton estático combina melhor e evita salto de versão |
| **Material Symbols (assets)** — Apache-2.0 | README oficial distingue Symbols atual de Icons legado não atualizado | SVG/vetor estático local; baixo. Importar seleção, não um runtime | **Adotar como família recomendada** |
| **Lucide (assets)** — ISC declarado | Repositório/docs e seleção SVG disponíveis; release não auditada aqui | SVG local convertido; baixo/médio. Não foi verificado pacote Android oficial | **Alternativa visual**; não tratar `lucide-react-native` como Kotlin/Compose |
| **Accompanist** — Apache-2.0 | README confirma Navigation-Animation/System UI Controller deprecados e removidos | Android Compose; médio se duplicar funcionalidade existente | **Evitar esses módulos**; usar Navigation Compose e insets oficiais |
| **Neobrutalism components** — MIT | Docs/código disponíveis; release/cadência não auditadas | React/Tailwind/Base UI; incompatível diretamente com Kotlin UI | **Referência de desenho**, não dependência Android |

Não se recomenda acrescentar todos os candidatos: a identidade nasce de tokens, tipografia, composição e consistência. Um kit com dezenas de componentes não elimina adaptação a regras específicas do BrainOut.

### Snapshot de versões e URLs de release

Todas consultadas em 01/10/2026, **sem instalação nesta tarefa**:

- [Lottie v6.7.1](https://github.com/airbnb/lottie-android/releases/tag/v6.7.1).
- [Rive 11.13.0](https://github.com/rive-app/rive-android/releases/tag/11.13.0).
- [Compottie 2.3.2](https://github.com/alexzhirkevich/compottie/releases/tag/2.3.2).
- [Compose Unstyled 2.10.0](https://github.com/composablehorizons/compose-unstyled/releases/tag/2.10.0).
- [Vico v3.3.1](https://github.com/patrykandpatrick/vico/releases/tag/v3.3.1).
- [Calendar 2.10.1](https://github.com/kizitonwose/Calendar/releases/tag/2.10.1).
- [Coil 3.6.3](https://github.com/coil-kt/coil/releases/tag/3.6.3).
- [Shimmer v1.5.0](https://github.com/valentinilk/compose-shimmer/releases/tag/v1.5.0).

### Compatibilidade: o que realmente sabemos

- [POM oficial BOM 2024.10.01](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2024.10.01/compose-bom-2024.10.01.pom): UI/Foundation/Animation **1.7.5**, Material 3 **1.3.1**, Adaptive **1.0.0**. É mapeamento publicado, não relatório de dependências efetivamente resolvidas pelo Gradle local.
- [Calendar README](https://github.com/kizitonwose/Calendar): Compose UI 1.7.x corresponde a Calendar 2.6.x; linha 2.10.x acompanha Compose 1.10/1.11. A versão newest pode elevar Compose transitivamente. Não instalar 2.10.1 como drop-in na base atual.
- [Shimmer README](https://github.com/valentinilk/compose-shimmer): 1.3.2 acompanha Compose 1.7.3; 1.5.0 acompanha CMP 1.11. Não confundir versão CMP com BOM Android. Requer avaliação da árvore resolvida.
- [Compottie README](https://github.com/alexzhirkevich/compottie): linha atual acompanha CMP 1.12; `dot`/`network` exigem desugaring abaixo de API 26. BrainOut tem desugaring no app; isso não dispensa teste API 24/25.
- [Lottie Compose build v6.7.1](https://raw.githubusercontent.com/airbnb/lottie-android/v6.7.1/lottie-compose/build.gradle): minSdk 21, compile/target 36; Android minSdk 24 do app atende o piso declarado. [versions.properties](https://raw.githubusercontent.com/airbnb/lottie-android/v6.7.1/versions.properties) referencia BOM 2024.02.01. Isso reduz uma suspeita de salto obrigatório, mas não prova compatibilidade binária/release.
- [Rive README](https://github.com/rive-app/rive-android) declara minSdk 21/target 35; é informação do README, não auditoria do manifest publicado de 11.13.0. O runtime inclui libs nativas e docs de ABIs/memória. Conferir empacotamento e alinhamento de páginas nativas quando adotado.
- Context7 de Rive indexou APIs modernas Compose e variantes baseadas em `Result`, incluindo exemplos em `master`. Não fixar assinatura a partir de snippet sem confirmar a tag adotada. O custo de lifecycle/rendering/input deve entrar no spike.
- APIs de shadows/adaptive atuais e Material Expressive não são automaticamente parte da BOM 2024.10.01. Não misturar upgrade de tooling com mudança visual sem validar separadamente.

### Orçamento e gates propostos de adoção

1. Implementar press, navegação, banners e confirmação com APIs Compose já presentes.
2. Se ilustração animada resolver um objetivo real, comparar **um** asset em Lottie com equivalente estático. Confirmar features suportadas, license do asset, fallback, lifecycle e redução de movimento.
3. Medir APK/AAB por ABI, startup, CPU/GPU e memória em build release antes/depois. Sem valor baseline não declarar overhead «insignificante».
4. Meta inicial: pacote total de ilustrações/animações ≤1MB comprimido; sem fonte/asset baixado como pré-condição. É orçamento proposto, não medida atual.
5. Rive só avança se state machine entregar valor não obtido com Compose simples e custo for aceito. Não manter dois engines ativos para a mesma necessidade.
6. Dependências futuras entram em `gradle/libs.versions.toml`; escolher versão publicada compatível, registrar resolução transitiva e manter repositórios permitidos. Atualização da BOM é tarefa verificável própria.

## 4. Fontes, ícones e documentação técnica

Todas as referências desta seção foram consultadas em **01/10/2026**; o comentário registra o que foi efetivamente verificado.

| ID / título | URL e uso |
|---|---|
| R06 — Archivo metadata e licença | [METADATA](https://raw.githubusercontent.com/google/fonts/main/ofl/archivo/METADATA.pb), [OFL](https://raw.githubusercontent.com/google/fonts/main/ofl/archivo/OFL.txt). Latin/latin-ext, eixos weight 100–900 e width 62–125; licença OFL 1.1 lida. **Reverificado 02/10/2026:** eixos `wdth 62–125` e `wght 100–900`, subsets latin/latin-ext/menu/vietnamese. O diretório `ofl/archivo` contém **só** `Archivo[wdth,wght].ttf` e a itálica — não há estáticos por peso, logo pesos fixos exigem instanciação local |
| R07 — Public Sans metadata e licença | [METADATA](https://raw.githubusercontent.com/google/fonts/main/ofl/publicsans/METADATA.pb), [OFL](https://raw.githubusercontent.com/google/fonts/main/ofl/publicsans/OFL.txt). Latin/latin-ext, weight 100–900; licença OFL 1.1 lida. **Reverificado 02/10/2026:** `wght 100–900`, subsets latin/latin-ext; diretório publica **só** `PublicSans[wght].ttf` e a itálica |
| R08 — Material Symbols / Material Icons | [Upstream Google](https://github.com/google/material-design-icons). Eixos, sets, distinção Icons/Symbols e Apache-2.0 declarados |
| R09 — Lucide | [Upstream](https://github.com/lucide-icons/lucide). SVG, pacotes oficiais enumerados, licença ISC declarada; sem validação de wrapper Compose |
| R10 — Quick guide to Animations in Compose | [Android Developers](https://developer.android.com/develop/ui/compose/animation/quick-guide). APIs, risco de alpha manter semântica, reinício de LaunchedEffect em lazy layouts e navegação; HTTP 200 via IPv4 + Context7 |
| R11 — Custom design systems in Compose | [Android Developers](https://developer.android.com/develop/ui/compose/designsystems/custom). Material wrappers e novos sistemas via CompositionLocal; HTTP 200 via IPv4 |
| R12 — API defaults / accessibility | [Android Developers](https://developer.android.com/develop/ui/compose/accessibility/api-defaults). 48dp, seleção/semântica e expansão de hit targets; HTTP 200 via IPv4 |
| R13 — Use window size classes | [Android Developers](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes). Janela dinâmica, 600/840/1200/1600dp e altura; HTTP 200 via IPv4 |
| R14 — WCAG 2.2 Contrast (Minimum) | [W3C](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html). Limiar 4.5:1, fórmula sRGB, aprovação sem arredondamento |
| R15 — WCAG 2.2 Non-text Contrast | [W3C](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html). 3:1, controles/estados, bordas, gráficos e adjacência |
| R16 — Lottie Android / Compose | [Upstream](https://github.com/airbnb/lottie-android). Runtime, license e autoria; Context7 confirma `rememberLottieComposition`, `animateLottieCompositionAsState`, progress lambda e escala de animação do sistema |
| R17 — Rive Android | [Upstream](https://github.com/rive-app/rive-android). Runtime, MIT, ABIs, lifecycle/native e docs oficiais vinculados |
| R18 — Compose Unstyled | [Upstream](https://github.com/composablehorizons/compose-unstyled). Renderless, módulos/semântica alegados e MIT |
| R19 — Vico | [Upstream](https://github.com/patrykandpatrick/vico). Compose Multiplatform, Apache-2.0 e release corretiva |
| R20 — Coil | [README](https://raw.githubusercontent.com/coil-kt/coil/main/README.md). `coil-compose`, módulo network separado, caches e Apache-2.0 |
| R21 — Accompanist | [Upstream](https://github.com/google/accompanist). Lista de módulos removidos/deprecados e Apache-2.0 |

## 5. Fontes indisponíveis e confiança

Tentativas descartadas como evidência: caminhos Figma `resource-library/neubrutalism/` e `brutalist-design/` retornaram 404; artigo Design Shack `articles/trends/neobrutalism/` retornou 403; caminhos tentados de Creative Bloq e LogRocket retornaram 404. Não foram citados como conteúdos lidos. O primeiro caminho tentado para CSS de ekmas retornou 404; a árvore do repo localizou `src/styling/globals.css`, que foi então lido.

Android Developers inicialmente teve timeout na ferramenta de fetch; nova consulta HTTPS via IPv4/extração de `article` confirmou páginas relevantes com HTTP 200. Não inferir indisponibilidade da documentação a partir da falha de rede local.

**Confiança alta:** valores tokens propostos calculados, recursos observados no código, licenças de fontes, mecanismo de sombras, releases retornadas pela API. **Confiança média:** comparação qualitativa de libs e direção estética. **Não validado:** satisfação do público, esforço em horas, desempenho/compatibilidade do app com candidatos e aparência de sites no browser.

## 6. Reverificação — 02/10/2026

Segunda passagem sobre **toda** a evidência externa deste documento, exigida pela auditoria doc×código registrada na **seção 14** do [plano de implementação](../plans/2026-10-01-redesign-neobrutalista.md). Reproduzível com `gh api repos/{owner}/{repo}/releases/latest` e as URLs citadas.

| Item reverificado | Resultado em 02/10/2026 |
|---|---|
| Releases dos 8 candidatos | **8/8 conferem** tag **e** data: Lottie `v6.7.1` (31/10/2025), Rive `11.13.0` (01/10/2026), Compottie `2.3.2` (27/09/2026), Compose Unstyled `2.10.0` (18/09/2026), Vico `v3.3.1` (28/08/2026), Calendar `2.10.1` (28/03/2026), Coil `3.6.3` (18/09/2026), Shimmer `v1.5.0` (09/07/2026); `prerelease=false` em todas |
| POM da BOM `2024.10.01` (dl.google.com) | UI/Foundation/Animation `1.7.5`, Material 3 `1.3.1`, `material3-window-size-class 1.3.1` e `androidx.compose.material3.adaptive:adaptive 1.0.0` — confere com o declarado |
| Licenças (SPDX via API) | Compose Unstyled MIT, Lottie Apache-2.0, Rive MIT, Compottie MIT, Vico Apache-2.0, Calendar MIT, Coil Apache-2.0, Shimmer Apache-2.0, neobrutalism-components MIT, Lucide ISC, material-design-icons Apache-2.0 — todas conferem. `ekmas/neobrutalism-components` **não publica releases** no GitHub: a ausência de cadência auditada segue correta |
| Compatibilidade por README | Calendar `1.7.x ↔ 2.6.x` e `1.10.x/1.11.x ↔ 2.10.x`; Shimmer `1.5.0 ↔ CMP 1.11` e `1.3.2 ↔ 1.7.3`; Compottie linha `1.12` com `dot`/`network` exigindo desugaring abaixo de API 26; Rive `minSdk 21 / target 35` com 4 ABIs; Lottie `minSdk 21`, `compile/target 36` e `version.androidx.compose=2024.02.01` — todas conferem. Os READMEs vivem em `master` (não `main`) em Shimmer e Rive |
| CSS do ekmas | `--border-radius: 5px`, `--box-shadow-x/y: 4px`, `--shadow: … 0px 0px` (blur e spread zero) — confere com o citado |
| Vico v3.3.1 | Corpo da release confirma o crash de `LineCartesianLayer` no Android 10 (`IllegalStateException` em `Canvas.restore`) |
| Accompanist | README lista Navigation-Animation, Navigation-Material e System UI Controller como «Deprecated & Removed»; release mais recente `v0.37.3` (28/04/2025) |
| Documentação Android e W3C | `shadows`, `accessibility/api-defaults`, `layouts/adaptive/use-window-size-classes`, `animation/quick-guide`, `designsystems/custom`, WCAG 2.2 *Contrast (Minimum)* e *Non-text Contrast*: **HTTP 200** nas sete. A página de shadows traz seção «Create neobrutalist shadows» e 36 ocorrências de `dropShadow` |
| NN/g (R01) | Data de publicação confirmada: 05/11/2017 |
| Contraste da paleta A | 27 pares declarados no [DESIGN.md](../../DESIGN.md#6-acessibilidade-e-contraste-calculado) recalculados por implementação independente da fórmula sRGB: **27/27 batem** com o declarado (diferença máxima 0,0034), inclusive o par reprovado `#51368F`/`#B5A1F5` = 4,13:1 |

**Não reverificado:** aparência renderizada dos sites de referência, empacotamento/ABI real dos runtimes, overhead de dependências e comportamento do app — todos exigem spike ou medição, conforme a seção 3 acima. Nenhuma release nova apareceu entre 01/10 e 02/10/2026, então nenhuma linha da matriz de bibliotecas muda de decisão por esse motivo.

**verify:** `python3 docs/design/verify_tokens.py` (raiz). Para renovar o snapshot upstream, consultar `gh api repos/airbnb/lottie-android/releases/latest --jq '{tag: .tag_name, published: .published_at, prerelease: .prerelease}'` e repetir para os candidatos adotados. Atualizar data/evidência antes de fixar dependência.
