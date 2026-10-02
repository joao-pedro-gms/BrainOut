# Especificação — componentes, fluxos e migração

**Proposta de 01/10/2026**, complementar ao [DESIGN.md](../../DESIGN.md). Razão de existir: permitir que cada feature seja migrada com contratos claros, sem inventar funcionalidades para preencher o novo visual.

## 1. Contratos dos componentes

Nomes `BrainOut*` abaixo são **propostos**, não componentes já existentes. Tokens resolvem por tema; componentes recebem dados/callbacks e não acessam Room, rede ou Hilt. Toda altura é mínima, salvo geometria decorativa. Conteúdo cresce com font scale.

### Ações e seleção

| Componente | Anatomia/visual | Estados e comportamento |
|---|---|---|
| `BrainOutButton` primary | Ação amarelo/ink; raio 8dp; borda 2dp; padding horizontal 20dp; altura mínima 52dp; sombra medium | Default 4dp/4dp; press face 2dp/2dp + sombra relativa 2dp/2dp; foco 3dp; loading mantém label «Salvando…» e progresso; disabled sem sombra e com tokens próprios |
| Secondary / tertiary | Secondary = `surface`/`text.primary` com contorno, sombra small; tertiary = link sublinhado sem caixa | Ações menos proeminentes; ≥48dp; não usar o mesmo amarelo para Cancelar e Salvar |
| Destructive | `error`/`onError`, raio 8dp, borda contrastante à superfície externa, sombra small | Rótulo explícito «Excluir projeto»; confirmação inclui consequência; nunca só ícone vermelho |
| `BrainOutFab` | FAB estendido, mínimo 56dp, raio 8dp, ícone 24dp + label; sombra medium | Uma ação principal; manter visível sobre listas com padding inferior suficiente; não criar FAB global de tarefa sem projeto definido |
| `BrainOutFilterChip` | Visual min-height 40dp, slot/alvo ≥48dp; raio 4dp; label 14sp; borda 2dp | Selecionado amarelo/ink + check; sem seleção surface/text; selectable/role apropriado; wrap/scroll quando não couber |
| `BrainOutStatusBadge` / `PriorityBadge` | Informativo, mínimo visual 28dp, padding 8dp/4dp, raio 4dp, borda 1dp; cores da receita semântica | Sem onClick ou Role.Button; label explícito; não exige alvo 48dp se não interativo |
| Radio / switch / checkbox | Controle Material tematizado dentro de linha ≥64dp; indicadores e track com contraste validado | Linha `selectable`/`toggleable`; controle interno sem segundo callback; grupo de radio; anunciar selected/checked; estados não dependem só de cor |

Focus e press são independentes: pressionar não apaga foco; focus ring tem área reservada. Hitbox permanece fixa enquanto a face animada desloca. Uma ação indisponível por regra de negócio mostra motivo legível, não apenas opacidade baixa.

### Entrada, superfícies e navegação

| Componente | Especificação |
|---|---|
| `BrainOutTextField` | Base `OutlinedTextField` tematizada ou wrapper Foundation com semântica equivalente; fundo surface, texto 16sp/24; raio 4dp; contorno 2dp; label persistente; supporting text ≥14sp; min-height 56dp. Focus com anel/contorno 3dp; erro + mensagem e ícone redundante; readOnly distinto de disabled |
| Campo de busca | Ícone 24dp, placeholder secundário neutro, limpar com alvo 48dp; preservar query e seleção ao voltar. Debounce continua no ViewModel; não animar resultados a cada caractere |
| Campo de prazo | Rótulo, data formatada por locale, abrir DatePicker; «Sem prazo» distinto de vazio inválido; hints de final de semana/feriado conforme resultado real do use case; aviso não bloqueia por decisão estética |
| `BrainOutProjectCard` | Nome 18sp/26, descrição 14sp/22, tags com swatch; padding 16dp; raio 8dp; borda 2dp; sombra small quando clicável; sem min-height rígida arbitrária. Não mostrar métricas sem dados disponíveis |
| `BrainOutTaskRow` | Título 16sp/24 ou 18sp/26, duas receitas status/prioridade separadas, prazo se disponível, menu 48dp. Sem sombra por linha; contorno 1dp ou divisor; padding 16dp; metadados quebram linha |
| Top app bar | Base neutra, título Archivo 22sp/28, Back nativo 48dp e menu; separação 2dp. Nome longo de projeto tem resumo/truncamento seguro e texto completo na área de conteúdo |
| Navegação | **Quatro** destinos hoje (`HomeTab` em `:feature:projects`); **cinco** no alvo deste redesign (Q13 acrescenta Agenda). Ícone 24dp e label 14sp; selecionado amarelo/ink, check/indicador e selected semantics. Min-height proposta 80dp + inset inferior, cresce com fonte. No médio/expandido usar rail. Não repetir bottom bar no detalhe/subtelas |
| Menu | Superfície opaca, raio 8dp, contorno 2dp; linha ≥48dp e label ≥14sp. Separar ações destrutivas; retorno de foco ao acionador; respeitar Back/outside dismiss |
| Dialog | Superfície opaca, raio 16dp, contorno 2dp; largura máxima 480dp; padding 24dp; título 22sp/28; ações empilham quando não couberem. Sem sombras gigantes; scrim preto 56%; foco modal e scroll interno |
| Sheet de formulário | **Evolução proposta** para formulário longo em janela curta, não requisito de trocar todos os dialogs. Raio superior 16dp, contorno, handles não são única forma de fechar; IME e ações acessíveis. Usar componente modal nativo com Back |

Manter `MaterialTheme` em volta dos wrappers. Campos de senha preservam transformação, autofill/teclado, mostrar/ocultar, ordem IME e foco no primeiro campo inválido. Rótulos têm a mesma posição entre light/dark; temas não alteram layout.

### Feedback e conteúdo auxiliar

| Componente | Contrato |
|---|---|
| `BrainOutStatePanel` | Título ≥18sp, descrição ≥14sp, ilustração opcional ≤120dp e CTA quando houver ação real; altura adaptável; alinhado ao início em listas, não obrigatoriamente centralizado |
| `BrainOutErrorBanner` | `error.container/text`, contorno, explicação + «Tentar novamente» quando callback existir. Falha de leitura tem prioridade sobre «vazio»; erro de escrita não apaga dados carregados |
| `BrainOutSyncBanner` | Offline = warning; pendente = info; contagem exata de operações, não «tarefas», pois fila contém outros recursos. Persistente enquanto condição real; sem spinner que afirme transferência sem estado de worker |
| Skeleton | Formas estáticas de surface.alt com contorno discreto; sem gradiente obrigatório; uma descrição de «Carregando» para o conjunto, não vários focos. Quando já há dados, preservar conteúdo em atualização |
| Progresso | Loading indeterminado só durante operação real; nunca barra que cresce por tempo sem progresso mensurável. Pequeno indicador no botão mantém label; acesso sem animação continua claro |
| Snackbar | Confirmação transitória/ação existente; texto legível, surface inversa + texto inverso, contraste validado. Não prometer «Desfazer» sem suporte de restauração no domínio/VM |
| Gráfico de prioridades | Cinco barras nomeadas; canto reto, contorno 2dp, eixo inicia em zero, valores visíveis; categorias fixas LOW→CRITICAL. Rótulos e alternativa textual com mesmos valores. Zero não é ausência de categoria |
| Taxa de conclusão | Percentual 32sp/40 + período/rótulo ≥14sp; preservar denominador/regra atual. «Sem tarefas» deve ser distinto de 0% quando o estado permitir; não inventar streak ou ranking |

## 2. Fluxos e estados das telas reais

### Estados compartilhados

| Condição | Apresentação | Regra de verdade |
|---|---|---|
| Primeira leitura sem dados ainda | Skeleton/loader com label | Não emitir empty antes da carga terminar |
| Leitura falhou sem conteúdo | Painel de erro + retry | Não dizer «Você ainda não criou…» |
| Leitura falhou com conteúdo disponível | Preservar conteúdo + aviso de atualização | **Evolução proposta** se UiState passar a preservar snapshot; código atual de algumas listas substitui body por erro |
| Nenhum item | Empty específico e próxima ação autorizada | Lista vazia legítima, não falha ou filtro |
| Busca/filtro sem resultado | «Nenhum projeto encontrado» + limpar filtros | Não usar empty de primeiro uso |
| Offline, fila 0 | «Sem conexão. Você pode continuar usando os dados locais.» | Conectividade já observada em Projetos/detalhe; não implica erro de Room |
| Offline, fila >0 | «Sem conexão. N alterações aguardam sincronização.» | Singular/plural correto; salva localmente |
| Online, fila >0 | «N alterações aguardam sincronização.» | Online não prova worker rodando. «Sincronizando» só com telemetria real futura |
| Online, fila 0 | Sem banner persistente | Não celebrar «Tudo sincronizado» só pela contagem: 4xx pode descartar operações |
| Escrita falhou | Mensagem localizada; draft e contexto permanecem | Não fechar formulário por mero clique; fechar após confirmação local |

Offline/sync global em Tarefas/Painel/Settings seria **evolução de estado**, não um dado já conectado aos respectivos composables. Não fabricar conectividade a partir de lista vazia. Perfil é salvo localmente e não participa do sync de projetos/tarefas.

### Splash / apresentação (`:feature:auth`)

- **Observado:** apresentação manual com monograma, título, tagline e «Começar». Não é somente o splash de sistema Android.
- **Proposta:** monograma em bloco violeta com offset 6dp, título Archivo e CTA amarelo; arte própria pequena. Entrada opcional ≤240ms, nunca posterga o botão. Textos vêm das strings reais; sem novos claims comerciais.
- **Estados:** estável e interativo sem rede; ao voltar não repetir animação longa. Loading de resolução de sessão pertence ao app, não a um atraso artificial da apresentação.

### Login e cadastro (`:feature:auth`)

- Header contido, formulário em coluna, CTA ao alcance após campos, max-width 480dp. Cadastro preserva nome, e-mail, senha, confirmação e seleção Owner/Member.
- Campo inválido tem erro inline e foco correspondente; erro de credenciais/formulário em banner; loading bloqueia submissões duplicadas mantendo texto. Não criar OAuth, biometria ou recuperação de senha sem contrato existente.
- IME Next/Done funciona; toggle de senha tem rótulo localizado e estado; teclado não encobre erro nem ação. Label de papel explica capacidade funcional; não sugerir plano pago porque existe um diálogo chamado Upgrade.
- **Estados:** idle, campo inválido, credenciais inválidas, cadastro duplicado, loading, sucesso via evento, falha inesperada. Autenticação offline deve seguir implementação real; esta proposta não garante login remoto/offline novo.

### Projetos / Home (`:feature:projects`)

- Hierarquia: título/identificação → busca → estado ativo/concluído → tags/ordenar → sync → lista → FAB. Papel usa badge informativo e label, não botão sem ação.
- Cards clicáveis com espaço para nome/descrição/tags; manter filtros/query ao retornar e durante alternância de tema. Cores escolhidas nas tags aparecem como swatch, não texto ilegível.
- **Estados:** carga, erro com retry, vazio inicial, nenhum resultado, conteúdo, erro de criação/exclusão e aviso sync. Empty de Member não oferece criação executável; mantém a explicação de permissão do fluxo atual.
- Criar projeto: nome obrigatório, descrição, tags existentes e rascunhos de novas tags/cor. Confirmação só fecha após gravação local; **ajuste funcional necessário** porque Home hoje fecha o dialog no dispatch de create. Não atribuir essa correção só ao tema.
- Criar tag: nome, cor, validação, preview com texto em neutro, cancelamento mantém draft do projeto. Evitar modais empilhados inacessíveis; recuperar foco no campo/grupo de origem.

### Detalhe de projeto (`:feature:projects`)

- Top bar com voltar/nome/menu, cabeçalho de dados do projeto, sync, lista de tarefas, FAB «Nova tarefa». Editar/excluir projeto continuam descobríveis em menu; não usar interação escondida por swipe como único caminho.
- Nova tarefa preserva título, prioridade e prazo. Prioridade pode usar lista radio vertical ao invés de cinco chips espremidos. Prazo abre o DatePicker Material e mostra os hints já calculados pelo use case.
- Renomear tarefa e editar nome/descrição do projeto preservam valores e fecham depois do evento de sucesso como nos fluxos atuais. Edição de tags no projeto **é aceita pelo repositório** (`ProjectRepository.update(project, tagIds)`) mas **não é exposta** na tela de detalhe; além disso a desassociação não viaja no sync (o dispatcher só enfileira associações). Não desenhar a ação como entregue.
- Status continua no menu com transições válidas: `TODO→DOING`, `DOING→TODO/DONE`, `DONE→DOING`. **Não** oferecer checkbox binário que permita `TODO→DONE` direto. O ícone TODO vazado é informativo, não um checkbox funcional.
- Prioridade de DONE é somente leitura; oferecer texto de motivo e caminho real de reabertura. Opções persistem nomes/IDs do domínio, não novos estados visuais como «Pausada».
- Exclusão de projeto abre confirmação com consequência; exclusão de tarefa atual ocorre por menu. Acrescentar confirmação de tarefa/undo exige decisão comportamental explícita futura.
- **Estados:** carga, erro, projeto ausente/removido, lista vazia, conteúdo, validação de escrita, prioridade bloqueada, sync. Ao concluir, feedback ocorre depois da confirmação local; não presumir confirmação remota.

### Tarefas global (`:feature:tasks`)

- Preservar lista global com nome do projeto (ou fallback), título, status e prioridade; compartilhar receitas com detalhe.
- **Observado:** a tela não recebe callback de abrir/editar/concluir tarefa. Portanto fichas são informativas e sem sombra de botão. Ação contextual futura exige rota/callback explícito e verificação de regras, não só uma nova aparência.
- **Estados:** carga, erro/retry/dispensa, vazio orientando ao projeto e lista. Proposta pode incluir prazo quando houver dados disponíveis no modelo. Busca/filtros/kanban próprios são expansão funcional, não parte comprovada desta tela.

### Painel (`:feature:tasks`)

- Faixa com ativos/concluídos, gráfico de prioridades e blocos de conclusão semanal/global; dados vêm do UiState existente, sem criar novos cálculos no composable.
- **Estados:** carga, erro, ausência de dados, zeros válidos e atualização reativa. Uma animação curta acompanha mudança de valor, nunca contagem fictícia do zero em cada retorno à aba.
- Barras usam receitas das cinco prioridades e contorno; valores e labels sempre visíveis, com lista textual acessível abaixo/ao lado. Fonte grande reorganiza labels sem apertar categorias em cinco colunas ilegíveis.
- Vico só se novas séries, zoom/markers ou complexidade justificarem. O Canvas atual já basta ao escopo real.

### Configurações, perfil, tema e notificações (`:feature:settings`)

- Configurações: lista neutra com Perfil, Notificações, Tema e Sair, linhas ≥64dp. Hoje as três primeiras são apenas `SettingsActionType` despachados para o `NavHost`, que as ignora (placeholder) — não há tela de destino. Sair é a única ação real e distinto das opções de navegação, limpa sessão via fluxo existente. Não transformar perfil em vitrine de assinaturas.
- Perfil: **tela inexistente hoje** (`ProfileScreen` não existe). Alvo: avatar de iniciais, nome/edit, e-mail e papel. Edição do nome é local; draft permanece se inválido, loading do usuário respeitado. Não incluir upload de foto/capa sem funcionalidade nova.
- Tema: três opções Sistema/Claro/Escuro, radio group de linha inteira; preview estático de tokens claro/escuro. **Não existe hoje**: `BrainOutTheme` deriva `darkTheme` de `isSystemInDarkTheme()` e nenhuma preferência é persistida, então tela **e** persistência são trabalho novo (NB-06/NB-27). Não acrescentar toggle de paletas B/C.
- Notificações: estado da permissão Android separado de `remindersEnabled`; concedida/negada/bloqueada, e retorno dos Ajustes devem ser refletidos. Negar não bloqueia o gerenciador de tarefas. **Não existe hoje**: não há tela de notificações nem preferência de lembretes; só o canal e o `DeadlineWorker` em `:app`. Orientação textual e atalho para Ajustes são trabalho novo.
- **Estados:** usuário carregando/ausente, nome inválido, preferência sendo salva/erro se VM expuser, permissão negada/concedida, lembretes desligados. Não criar spinners de rede onde só há DataStore/Room local.

## 3. Motion e feedback

Assinatura: **encaixe curto**, com face que alcança uma sombra fixa. A animação comunica interação e resultado; nenhuma operação espera animação concluir para ser executada. Sem som obrigatório ou motion decorativo contínuo.

| Token / evento | Duração | Curva / amplitude | Implementação futura |
|---|---|---|---|
| `instant` — movimento removido | 0ms | Estado final estático | Acesso à política central |
| `press` | 80ms | Linear; face +2dp x/y, sombra relativa 4→2dp | `updateTransition`/`animate*AsState`, draw/layer; hitbox estável |
| `release` | 120ms | cubic(0.2,0,0,1); retorna sem overshoot | Mesmo interactionSource, cancel/foco tratados |
| `state` — label/badge/banner | 160ms | cubic(0.2,0,0,1); fade/pequena expansão | `AnimatedContent`/`AnimatedVisibility` |
| `navigation` — abrir detalhe | 220ms | cubic(0.2,0,0,1); deslocamento máximo 16dp | Transições de Navigation Compose; Back respeita direção/gesto |
| `modal` | 240ms | cubic(0.2,0,0,1); deslocamento máximo 24dp | APIs do componente modal, sem reimplementar janela |
| `completion` | 240ms | cubic(0.2,0,0,1); check aparece, sem confete | Depois de confirmação local; uma vez por evento |
| `illustration` opcional | ≤600ms | Arte vetorial de um ciclo | Lottie somente se aprovado; não autoplay em cada scroll |

Tabs não usam slide lateral de página que sugira ordem sequencial obrigatória; troca discreta de 160ms preserva estado/scroll. Alteração de filtro não encena uma entrada escalonada por item. Key estável em listas; não disparar LaunchedEffect(Unit) de celebração a cada recomposição/reentrada de item.

**Política de redução proposta:** defaults respeitam a escala de animação do sistema. Se «Remover animações»/escala zero estiver ativo, press muda contorno estático, todos os estados renderizam diretamente e ilustrações usam poster; nenhum loop. Se futuramente existir preferência «Reduzir movimento», ela só pode reduzir mais, nunca reativar movimento desabilitado no sistema. Esse ajuste de preferência **não existe** no app atual.

Centralizar uma política `BrainOutMotionPolicy`; conferir a API efetivamente disponível para duration scale na versão adotada. Testar configuração do sistema 0 e normal em dispositivo. Lottie tem `ignoreSystemAnimatorScale=false` documentado: manter false; ainda escolher poster no modo reduzido. Rive/custom Canvas requerem política explícita; não presumir que um engine externo segue Compose automaticamente.

Haptics: leves e opcionais para confirmação efetiva, via APIs Android/Compose compatíveis; respeitar preferências/dispositivo. Não vibrar a cada scroll, erro de tecla ou sincronização em background. Progresso real tem texto persistente mesmo quando não anima. Sem flashes repetitivos.

**Performance como meta de QA:** no aparelho de referência em 60Hz, comparar proporção de frames lentos e p95 das interações com baseline; orçamento por frame de referência 16.7ms (8.3ms em 120Hz). Não chamar a animação de «60fps» por inspeção do código. Cachear paths/painters, evitar alocação por frame e pausar engine fora do lifecycle visível. Medir release em hardware antes de aceitar dependência.

## 4. Mapeamento futuro para a arquitetura

| Local | Mudança planejada |
|---|---|
| `:core:ui/theme/Color.kt` | Primitivos e esquemas semânticos; exportar apenas papéis públicos |
| `:core:ui/theme/Type.kt`, `Shape.kt`, `Theme.kt` | 15 tipos explícitos, shapes, ColorScheme completo. `Theme.kt` já expõe o parâmetro `darkTheme` e mantém `dynamicColor = false` por padrão, mas **não** existe modo persistido (System/Light/Dark) — entra em NB-06/NB-27 |
| `:core:ui/theme/` (novos arquivos) | `BrainOutSpacing`, `BrainOutBorders`, `BrainOutShadows`, `BrainOutMotion`, status colors via tipos imutáveis/CompositionLocal |
| `:core:ui/components/` | Wrappers Button/Field/Badge/StatePanel/Banner e drawing de sombra; APIs com Modifier/callback/content; strings de componentes compartilhados no módulo apropriado e traduzidas |
| `:feature:*` | Screen conecta UiState/callbacks às novas peças; mantém regras/eventos no VM/use case; usa tokens, sem raw hex |
| `:app/navigation/` | Shell responsivo, barra/rail, transitions e insets; sem importar uma feature de outra |
| `gradle/libs.versions.toml` | Somente dependências aprovadas após spike; sem versões inline ou repositório extra |

Não colocar status de tarefa no domínio como cor Android: mapear enum para papéis na UI. `:core:ui` permanece sem Room/Hilt/DataStore. Fonte/ícones são assets locais com proveniência. Dialogs usam recursos das features para texto específico, não texto hardcoded em biblioteca visual.

Na base Compose 1.7.5, sombra rígida é uma forma atrás da face desenhada com Canvas/drawWithCache e offset explícito; na versão nova que disponibilize `dropShadow`, o mesmo contrato pode usar radius/spread zero. A implementação permanece em um único modifier/componente; não copiar código de sombra em quatro módulos. O JSON é especificação, não parser em runtime: gerar/transcrever valores Kotlin e testar paridade, sem carregar arquivo de design a cada frame.

## 5. Migração incremental

| Fase | Entrega | Gate de saída |
|---|---|---|
| 0 — baseline e piloto | Capturas de telas atuais; protótipos de Projetos/detalhe/login em A, light/dark | Validar aparência, hipótese de público, fontes e legibilidade; registrar decisões. B/C só se A falhar na avaliação |
| 1 — fundações | Tokens, fontes/vetores, galeria em previews, wrappers pequenos | JSON→Kotlin coerente, contraste, font scale, touch/foco/TalkBack; sem feature migrada parcialmente por cor apenas |
| 2 — shell + Projetos | Navegação, search, filtros, cards, estado/sync, criação/tags | Preservar rota/tab/scroll, papel Owner/Member e dados locais; formulário fecha por sucesso |
| 3 — detalhe e Tarefas | Ficha, badge, menus, dialogs, prazo e lista global | Paridade de status/prioridade, matriz de transições, DONE bloqueia prioridade, sem ação fictícia na lista global |
| 4 — auth + Settings | Splash, forms, perfil, tema, notificações | IME/validação, session/back, preferências reais, negativa de permissão e retorno dos Ajustes |
| 5 — Painel e motion | Gráfico/percentuais, feedback, ilustração opcional | Sem números inventados; alternativa textual; redução de movimento e medições release |
| 6 — consolidação | Retirar cores/raios/componentes antigos duplicados; atualizar docs/testes | Matriz visual completa e checks do repo verdes; sem duas receitas de badge em produção |

As fases são recortes de entrega, não previsão em dias. Não depender de atualizar todas as bibliotecas para começar. Fixes comportamentais identificados (ex.: fechar criação antes da gravação) devem ter regressão própria e descrição separada da migração visual.

## 6. Critérios de aceite verificáveis

### Matriz de cenários

Validar pelo menos 320/360/412dp compacto, 600/840dp limites e 1024dp tablet; alturas curtas em paisagem; claro/escuro; font scale 1.0/1.3/2.0; pt-BR/EN; escala de animação normal/zero. Não exigir o produto cartesiano de todo cenário: capturar pares críticos e ampliar onde ocorrer falha.

- Campos/ações visíveis com IME; dialogs podem rolar; FAB não cobre último item; offsets não cortam no scroll/clip.
- Texto completo onde corrige erro; títulos longos com caminho para leitura integral; chips/menus crescem; sem fontScale clamp.
- Criar projeto Owner, restrição Member, draft tag e cancelamento; query/filtro sem resultados preserva contexto.
- Criar tarefa, renomear, editar projeto, alterar prioridade, transições permitidas e proibidas; falha local mantém formulário e texto.
- Offline cria/edita localmente; retorno de conexão mantém contagem coerente. Simular 4xx descarta fila e verificar que UI não declara sincronização confirmada indevidamente.
- Perfil não promete sync; tema restaura após reiniciar; permissão negada não esconde tarefas; sair limpa sessão pelo contrato existente.
- TalkBack completa os fluxos sem nós «botão» vazios, lê selecionado/checked, recebe foco no erro/modal e acessa dados dos gráficos.
- Teclado/Switch Access acessam menus e seleção; foco visível atende ≥3:1 em fundos adjacentes.
- Motion removido renderiza estado final; engine para fora da tela/background; sem replay decorativo em scroll.
- Labels/valores numéricos do painel coincidem com UiState; prioridade e status usam a mesma receita nas duas features.

### Evidências e comandos da futura implementação

Registrar capturas em emulador/device Android (não browser), configurações, build SHA e hardware. Usar a infraestrutura de testes Compose já existente e novos testes de comportamento onde necessário; screenshot tooling adicional é decisão posterior.

| Verificação | Comando/ação |
|---|---|
| Documento/tokens, agora | `python3 docs/design/verify_tokens.py` |
| Unit/regressão, após código | `./gradlew testDevDebugUnitTest` |
| Estática, após código | `./gradlew ktlintCheck detekt` (detekt com JDK compatível conforme AGENTS) |
| Tradução/lint | `./gradlew :app:lintDevDebug` e lints dos módulos com gate |
| Coverage onde código core mudar | `./gradlew :core:domain:koverVerify :core:data:koverVerify` |
| Build de referência | `./gradlew :app:assembleDevDebug`; para performance usar variante release adequada |
| TalkBack/gestos/performance | Sessão real no dispositivo com log de resultados e comparação baseline |

**Done desta especificação:** valores/aliases resolvem, pares de contraste passam, referências importantes têm origem/data e todas as telas reais possuem plano. **Done do redesign futuro:** evidência visual, funcional e assistiva dessa matriz, não só build verde. Nenhum dos checks Android ou ensaios em dispositivo foi executado por esta tarefa de documentação.
