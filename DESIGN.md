# BrainOut — sistema de design neobrutalista

**Status: proposta para o futuro redesign, não identidade já implementada.** Pesquisa e especificação: **01/10/2026**. Base inspecionada: `4dcfa0a`. Autor do projeto: João Pedro G M Silva.

**Razão de existir:** transformar referências visuais em decisões implementáveis para reformular toda a UI Android, preservando legibilidade, regras de negócio e comportamento offline-first.

## Como usar esta especificação

| Artefato | Responsabilidade |
|---|---|
| Este documento | Identidade, paletas, tokens semânticos, tipografia, geometria e acessibilidade |
| [Pesquisa e bibliotecas](docs/design/PESQUISA.md) | Evidências web, referências comentadas, manutenção, licenças, compatibilidade e decisões |
| [Componentes, telas e migração](docs/design/ESPECIFICACAO.md) | Contratos de UI, fluxos reais, estados, motion, implementação futura e QA |
| [Tokens estruturados](docs/design/tokens.json) | Valores canônicos da proposta; formato próprio do projeto, sem promessa de importação DTCG/Figma |
| [Verificador](docs/design/verify_tokens.py) | Validação de referências, paridade light/dark e contraste dos pares especificados |

**Observado** significa confirmado no código ou fonte consultada. **Proposto** significa decisão de design recomendada. **Hipótese** exige validação com usuários ou protótipo. Os valores deste documento são propostos. A pesquisa não adiciona dependências nem altera Kotlin.

## 1. Produto e ponto de partida

BrainOut organiza projetos pessoais e tarefas em Android nativo. Possui autenticação, papéis Owner/Member, tags, busca e ordenação de projetos, prazos, três estados de tarefa, cinco prioridades, painel de métricas, perfil, tema e lembretes. A gravação local e a fila de sincronização são parte central da experiência.

**Hipótese de uso:** consultas rápidas entre atividades acadêmicas/profissionais, com interrupções e conexão variável. Isso orienta o design, mas não constitui pesquisa com o público do app. O projeto acadêmico não prova que todos os usuários são estudantes.

### Inventário observado

Os caminhos Kotlin abaixo são relativos a `src/main/kotlin/pucgo/joaopedrogmsilva/brainout/` de cada módulo.

| Evidência local | Situação atual e impacto |
|---|---|
| `core/ui/.../core/ui/theme/{Color,Theme,Shape,Type}.kt` | Paleta Material roxa `#6750A4`, tema claro/escuro estático, fonte do sistema; raios 4/8/12/16/28dp. Boa centralização inicial |
| `app/.../navigation/BrainOutNavHost.kt` | Shell com quatro destinos: Projetos, Tarefas, Painel e Configurações; estado das abas preservado |
| `feature/projects/.../feature/projects/ui/home/HomeScreen.kt` | Busca, tags, ordenação, ativos/concluídos, criação de projeto e limitação por papel |
| `feature/projects/.../feature/projects/ui/projectdetail/ProjectDetailScreen.kt` | Criação/edição, prioridade, status, exclusão, prazo e erros; alguns formulários fecham por evento de sucesso |
| `feature/projects/.../feature/projects/ui/common/OfflineBanner.kt` | Offline e contagem de operações pendentes; atualmente offline usa cor de erro |
| `feature/tasks/.../feature/tasks/ui/TasksScreen.kt` | Lista global informativa; chips em `Surface`, sem ação fictícia |
| `feature/projects/.../feature/projects/ui/projectdetail/TaskRow.kt:219–259` | Chips informativos ainda são `AssistChip` com callback vazio; cores de status diferem da lista global |
| `feature/tasks/.../feature/tasks/ui/DashboardScreen.kt` | Contagens de projetos, cinco barras de prioridade em Canvas, conclusão semanal/global |
| `feature/settings/.../feature/settings/ui/{Profile,Theme,Notifications}Screen.kt` | Perfil com edição local, tema Sistema/Claro/Escuro, lembretes e permissão Android real |
| `gradle/libs.versions.toml` | BOM Compose `2024.10.01`; POM oficial associa UI/Foundation/Animation `1.7.5`, Material 3 `1.3.1` |

Existem raios, espaçamentos e transparências locais nas features; trocar somente `ColorScheme` não cria uma linguagem consistente. Alguns comentários/AGENTS e [acessibilidade atual](docs/ACESSIBILIDADE.md) estão defasados: código confirma quatro abas e SettingsViewModel. Esta proposta usa o código como evidência funcional.

**Limite da inspeção:** não houve execução do app nem captura de telas. Não foram encontrados goldens/screenshot fixtures na busca por imagens, apenas ícones de launcher. A avaliação visual atual é inferida dos composables; não é uma auditoria visual em dispositivo.

## 2. Fundamentos e direção exclusiva

Neobrutalismo digital é uma linguagem contemporânea, sem norma única: contraste estrutural, massas de cor, bordas visíveis, sombras rígidas e tipografia enfática. Brutalismo web pode enfatizar material cru/HTML e austeridade; antidesign busca ruptura/disorientação. A síntese das fontes e a tradução Android estão em [Pesquisa](docs/design/PESQUISA.md).

### Direção recomendada: «Bloco de ação»

**Tese:** dar forma concreta ao que precisa sair da cabeça e virar ação. Tarefas se comportam como fichas de trabalho: título claro, identificação de estado, prioridade explícita e controle acessível. A assinatura é o **encaixe de uma ficha no próprio carimbo/sombra** ao pressionar ou confirmar uma ação.

Não atribuir exclusividade histórica à estética: a combinação é uma proposta autoral para BrainOut, não prova de originalidade absoluta. Não transformar o nome em uma mascote cerebral onipresente.

1. **Estrutura forte, conteúdo tranquilo.** Bordas 2dp e alinhamento estável; corpo de texto convencional e sem rotações.
2. **Cor tem trabalho.** Amarelo identifica ação principal; violeta identifica marca/seleção; verde comunica conclusão; erro é reservado a falha ou destruição.
3. **Volume indica interação.** Sombra rígida em ações e cartões de projeto clicáveis; listas densas usam contorno/divisor sem sombra por item.
4. **Estado é visível e verdadeiro.** «Salvo neste dispositivo» não equivale a «Sincronizado». Fila vazia não prova ausência de rejeições remotas.
5. **Android continua reconhecível.** Back, teclado, menus, permissões, seleção e leitura por TalkBack seguem convenções nativas.
6. **Expressão é finita.** Uma ação principal por superfície; sem marquises, loop decorativo, confete repetitivo ou penalização por atraso.

### Composição proposta — primeiro viewport de Projetos

Janela de 360dp: margens 16dp, título «Projetos» em Archivo 28/36, nome/papel em metadados secundários; busca abaixo, filtros de estado e tags em blocos distintos. Aviso de conectividade aparece antes da lista quando necessário. Cartões de projeto ocupam a largura útil, com nome, descrição e tags. FAB estendido «Novo projeto» fica acima da navegação com quatro rótulos. A sombra ocupa espaço reservado, nunca cobre o próximo item. Alturas crescem com texto; não cabe tudo obrigatoriamente no primeiro viewport.

O Painel usa uma faixa de contagens e gráfico com valores rotulados; não uma coleção de caixas coloridas indistinguíveis. Configurações usa linhas de leitura e apenas um preview de marca/tema.

## 3. Paletas e papéis

### A — Bloco de ação (recomendada)

| Primitivo | Hex | Papel |
|---|---|---|
| `ink` | `#181818` | Texto e contorno no claro; texto sobre acentos claros nos dois modos |
| `paper` | `#F4F4F0` | Fundo claro e texto principal escuro |
| `white` | `#FFFFFF` | Superfície clara elevada visualmente por contorno |
| `yellow` | `#F9D94A` | Ação principal e seleção forte |
| `violet` | `#B5A1F5` | Identidade e acento secundário |
| `mint` | `#A8E6CF` | Acento de conclusão/ilustração |
| `blue` | `#A5D8FF` | Acento informativo/ilustração |
| `orange` | `#FFBA8C` | Ilustrações e prioridade alta em material exploratório; usar warning semântico na UI |
| `night` / `charcoal` / `graphite` | `#191919` / `#242424` / `#303030` | Fundo/superfícies escuras |

**Distribuição proposta:** 75–85% da área de telas operacionais em neutros; até 15–25% em acentos, com no máximo dois grandes campos saturados no viewport. É uma regra de composição, não uma exigência matemática de pixels. Splash pode ser mais expressivo; erro persistente pode superar essa distribuição.

### Alternativas para comparação, não temas adicionais prontos

| Direção | Fundo / tinta / ação / identidade / apoio | Trade-off |
|---|---|---|
| B — Oficina azul | `#EFF4FF` / `#13213A` / `#9CC7FF` / `#FFB89E` / `#D6C7FF` | Mais serena; reduz distinção entre ação e informação. Boa hipótese se amarelo parecer fatigante |
| C — Cartaz violeta | `#F6F1FF` / `#241A32` / `#C6AFF7` / `#F9D94A` / `#BCE9D4` | Mantém continuidade com o roxo atual; risco de marca e prioridade urgente disputarem o mesmo papel |

B/C possuem somente cinco primitivos exploratórios. Não são esquemas Material completos, não têm dark homologado e não herdam os resultados AA da paleta A. Escolher uma delas exige regenerar semânticos, estados e matriz de contraste. Não oferecer três paletas ao usuário final neste escopo.

### Tokens semânticos da paleta A

Hex são sRGB opacos, exceto `scrim` definido separadamente. `text.muted` é permitido somente em fundos neutros testados. Todo acento claro recebe `ink`, inclusive no modo escuro.

| Token | Light | Dark | Uso |
|---|---|---|---|
| `background` | `#F4F4F0` | `#191919` | Fundo da janela |
| `surface` | `#FFFFFF` | `#242424` | Cartão/campo/menu |
| `surface.alt` | `#E5E5DE` | `#303030` | Agrupamento, disabled e TODO |
| `text.primary` | `#181818` | `#F4F4F0` | Leitura principal |
| `text.muted` | `#54544C` | `#C6C6BE` | Descrição e metadados |
| `border.default` / `focus` | `#181818` | `#F4F4F0` | Contorno de superfície neutra e foco |
| `border.onAccent` | `#181818` | `#181818` | Contorno sobre amarelo/violeta/mint/blue |
| `shadow` | `#181818` | `#B7B7AE` | Impressão deslocada, sem brilho/blur |
| `action.background` / `action.text` | `#F9D94A` / `#181818` | `#F9D94A` / `#181818` | Botão principal, FAB e tab selecionada |
| `brand.background` / `brand.text` | `#B5A1F5` / `#181818` | `#B5A1F5` / `#181818` | Selo de marca, não texto violeta no branco |
| `link` | `#544500` | `#F9D94A` | Link sublinhado em fundo neutro |
| `secondary` / `onSecondary` | `#51368F` / `#FFFFFF` | `#C8B5FF` / `#181818` | Controle Material secundário |
| `secondary.container` / `secondary.text` | `#B5A1F5` / `#181818` | `#38294F` / `#E7DAFF` | Urgente e seleção secundária |
| `success` | `#165C47` | `#A8E6CF` | Texto/ícone de sucesso em neutro |
| `success.container` / `success.text` | `#DCF4E8` / `#165C47` | `#203F34` / `#BBF1DA` | DONE e confirmação |
| `info` | `#174F7B` | `#A5D8FF` | Texto/ícone informativo em neutro |
| `info.container` / `info.text` | `#DCEEFF` / `#174F7B` | `#20374A` / `#DCEEFF` | DOING e fila pendente |
| `warning` | `#665000` | `#FFE681` | Texto/ícone de atenção em neutro |
| `warning.container` / `warning.text` | `#FFF0B8` / `#665000` | `#453B1E` / `#FFE681` | Offline, prazo e prioridade alta |
| `error` / `onError` | `#A51D2D` / `#FFFFFF` | `#FFB4B4` / `#181818` | Falha e ação destrutiva |
| `error.container` / `error.text` | `#FFE0E0` / `#741C1C` | `#4A2424` / `#FFB4B4` | Erro localizado, crítica |
| `disabled.background` / `disabled.text` | `#E5E5DE` / `#54544C` | `#303030` / `#C6C6BE` | Desabilitado com texto legível, sem sombra |

**Atenção ao Material:** `primary = link`, `onPrimary = white` no claro e `ink` no escuro. `primaryContainer = action.background`, `onPrimaryContainer = action.text`. O botão BrainOut principal usa explicitamente os tokens de ação. Usar amarelo como `primary` no claro faria labels/links Material perderem contraste. O mapeamento dos demais papéis e níveis de superfície está em `materialMapping` no JSON.

Não aceitar defaults roxos residuais: mapear explicitamente `surfaceTint`, inversos, containers de superfície e `outlineVariant`; elevação tonal de cartões e botões BrainOut é 0dp. Popups/sheets mantêm modalização nativa, com superfície opaca e scrim preto a 56%. Contraste de conteúdo é calculado dentro do popup, não contra o scrim.

### Status, prioridades, prazo e tags

| Valor real | Container / texto | Indicador redundante |
|---|---|---|
| TODO — A fazer | `surface.alt` / `text.primary` | Círculo vazado + rótulo |
| DOING — Em andamento | `info.container` / `info.text` | Indicador de progresso estático + rótulo |
| DONE — Concluída | `success.container` / `success.text` | Check + rótulo; manter título legível |
| LOW — Baixa | `surface.alt` / `text.primary` | «Baixa» + 1 marca |
| MEDIUM — Média | `info.container` / `info.text` | «Média» + 2 marcas |
| HIGH — Alta | `warning.container` / `warning.text` | «Alta» + 3 marcas |
| URGENT — Urgente | `secondary.container` / `secondary.text` | «Urgente» + 4 marcas |
| CRITICAL — Crítica | `error.container` / `error.text` | «Crítica» + 5 marcas |

Marcas são decoração redundante; TalkBack anuncia o rótulo uma vez. Status descreve andamento; prioridade descreve importância; atraso é um terceiro sinal textual e não muda automaticamente a prioridade. Não pintar o cartão inteiro de vermelho por atraso.

Tags têm cor escolhida pelo usuário no modelo atual: exibi-la como swatch com contorno, mantendo nome em `text.primary` sobre `surface`. Não colocar texto diretamente em cor arbitrária sem calcular contraste. Cor inválida recebe swatch neutro. Não alterar o hex persistido para adaptar o dark.

## 4. Tipografia

**Proposta:** Archivo para títulos/controles enfáticos e Public Sans para leitura. Ambas têm Latin/Latin-ext e licença SIL OFL 1.1 verificada no repositório Google Fonts. Archivo sustenta títulos compactos e robustos; Public Sans evita que a interface inteira tenha voz de cartaz. São escolhas de design, não garantias empíricas de maior usabilidade.

Empacotar fontes locais em `core/ui/src/main/res/font/`, com OFL e origem registradas. O app deve funcionar offline na primeira abertura. Preferir arquivos estáticos oficiais dos pesos necessários; se usar instâncias geradas a partir dos eixos variáveis, documentar origem/geração e validar API 24/25. Não depender de suporte a fontes variáveis presente somente em Android mais novo. Fallback: `FontFamily.Default`; não usar negrito sintético como resultado final.

| Papel Material | Família | Tamanho / linha (sp) | Peso | Tracking (sp) |
|---|---|---|---|---|
| displayLarge / Medium / Small | Archivo | 40/48 · 36/44 · 32/40 | 800 | -0.5 |
| headlineLarge / Medium / Small | Archivo | 32/40 · 28/36 · 24/32 | 700 | 0 |
| titleLarge / Medium / Small | Archivo | 22/28 · 18/26 · 16/24 | 700 · 600 · 600 | 0 |
| bodyLarge / Medium / Small | Public Sans | 16/24 · 14/22 · 13/20 | 400 | 0 |
| labelLarge / Medium / Small | Public Sans | 15/20 · 14/20 · 12/16 | 600 | 0.1 |

Usar `titleMedium` nas fichas, `bodyLarge` nos inputs e `labelMedium` em chips. `labelSmall` somente em metadados auxiliares; prazos, erros e ações nunca abaixo de 14sp. Todos os 15 papéis recebem definição explícita para evitar fonte/tamanho Material residual. Números do painel usam dígitos tabulares se o arquivo escolhido suportar a feature; fallback não pode quebrar alinhamento.

Não usar caixa alta em frases, fonte monoespaçada em descrição ou condensação em campos. Títulos podem truncar após duas linhas somente onde houver acesso ao texto completo no detalhe; na lista global informativa, o título deve crescer porque não há ação de abertura. Formulários, erros, menus e instruções crescem e quebram linha. Validar «Ação», «Conclusão», «João», «ç», números, e-mail longo e texto EN com as fontes reais.

## 5. Ritmo, formas, sombras e ícones

| Grupo | Tokens propostos |
|---|---|
| Espaçamento (dp) | 0, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64; 4 = unidade de ritmo |
| Aplicação | 8 ícone/rótulo; 12 entre controles; 16 padding de ficha; 24 entre seções; 32 entre grandes grupos |
| Raio (dp) | 0 barras/gráficos; 4 chips/campos; 8 botões/cards/menus; 16 dialogs/sheets; círculo só avatar/radio |
| Shapes Material | extraSmall 4; small 4; medium 8; large 8; extraLarge 16 |
| Bordas (dp) | 1 divisores/chips informativos; 2 contornos; 3 foco/seleção; não mudar tamanho externo no foco |
| Sombra (x,y,blur,spread em dp) | none 0/0/0/0; small 2/2/0/0; medium 4/4/0/0; display 6/6/0/0 |
| Controles | Toque mínimo 48×48dp; botão min-height 52dp; campo min-height 56dp; FAB 56dp; linhas min-height 64dp |
| Geometria de ícone | 24dp padrão; 20dp metadado; 32dp empty state; alvo continua 48dp quando acionável |

Sombra significa impressão deslocada, não iluminação. No dark, é um offset neutro claro discreto, sem halo. Reservar até 6dp embaixo/no final da caixa para o desenho. Não aplicar `clip` a um pai que corta a sombra; aplicar recorte de conteúdo depois do desenho do volume. Pressão desloca somente a face, preservando layout e hitbox.

**Ícones recomendados:** Material Symbols Outlined, peso visual 500, óptico 24; exportar vetores estáticos locais da seleção necessária. Material Icons do catálogo atual não é o mesmo conjunto atualizado. Não carregar a fonte completa de símbolos para desenhar meia dúzia de glifos. Setas direcionais acompanham RTL; símbolos de estado não são espelhados indiscriminadamente. Lucide é alternativa coerente, desde que todo o conjunto seja convertido/testado; não misturar três famílias.

**Ilustração:** traço uniforme, 2–3 cores, formas preenchidas e offset único. Sem texto rasterizado, sem imagens remotas obrigatórias. Criar no máximo uma ilustração original para splash e três empty states; autoria/licença devem acompanhar o asset. Não reutilizar logos, personagens ou imagens de Gumroad.

### Layout adaptativo

Projetar pela janela disponível, não pelo modelo do aparelho. [Fonte oficial](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes).

| Largura disponível | Composição |
|---|---|
| <600dp | 1 coluna; margens 16dp (24dp em auth se houver espaço); barra inferior com quatro destinos |
| 600–839dp | Rail; margens 24dp; cards de projeto em 2 colunas se cada coluna tiver ≥260dp; formulários até 480dp |
| ≥840dp | Rail; margens 32dp; lista/detalhe lado a lado como evolução, sem inventar nova rota de tarefa |
| ≥1200dp | Conteúdo de trabalho limitado a 1200dp; mais espaço não implica cartões gigantes |

Grid visual de 4 colunas no compacto, 8 no médio, 12 no expandido; gutter 16dp/24dp. A unidade de 4dp organiza blocos, sem forçar baseline textual a múltiplos de dp: texto está em sp e escala independentemente. Com altura <480dp, priorizar rolagem e coluna única em formulários. Em dobra/separador, panes ficam em áreas utilizáveis; campos nunca atravessam a dobradiça. Aplicar insets de status/nav/cutout/IME uma vez por shell, e testar Back preditivo.

## 6. Acessibilidade e contraste calculado

WCAG 2.2 é usada como referência quantitativa, não certificação do app nativo. Texto ≥4.5:1 em todos os tamanhos desta proposta; limites/ícones essenciais ≥3:1. 48dp vem da orientação Android, não de equivalência direta entre dp e pixels CSS de WCAG 2.5.8.

Fórmula sRGB: normalizar canal /255; linearizar com divisor 12.92 se ≤0.04045, senão `((c+0.055)/1.055)^2.4`; luminância `0.2126R + 0.7152G + 0.0722B`; razão `(Lmax+0.05)/(Lmin+0.05)`. Critérios usam valor completo, sem arredondamento para aprovação. Tabela arredondada somente para leitura:

| Par essencial | Razão |
|---|---|
| Ink / paper · Ink / white | 16.10:1 · 17.76:1 |
| Ink / yellow · Ink / violet | 12.73:1 · 7.93:1 |
| Ink / mint · Ink / blue | 12.59:1 · 11.72:1 |
| Muted / paper · Muted / white | 6.93:1 · 7.64:1 |
| Paper / night · Paper / charcoal · Paper / graphite | 15.94:1 · 14.08:1 · 11.97:1 |
| Muted dark / charcoal · Muted dark / graphite | 9.03:1 · 7.68:1 |
| Link light / paper | 8.55:1 |
| White / error light | 7.46:1 |
| Error text / container — light · dark | 8.82:1 · 7.93:1 |
| Warning text / container — light · dark | 6.79:1 · 8.90:1 |
| Success text / container — light · dark | 6.83:1 · 9.17:1 |
| Info text / container — light · dark | 7.26:1 · 10.38:1 |
| Secondary container text — light · dark | 7.93:1 · 9.93:1 |
| Shadow dark / charcoal (decorativo) | 7.69:1 |

Pares fora das receitas precisam de cálculo próprio. Exemplo rejeitado na exploração: `#51368F` sobre `#B5A1F5` = **4.13:1**, insuficiente para texto normal; container violeta usa `#181818`. Não reduzir opacidade global para disabled/erro, nem aplicar `text.muted` sobre acentos. As barras pastel do gráfico precisam de contorno contrastante e valores textuais.

### Requisitos não negociáveis

- Cada ação tem alvo real ≥48×48dp e separação mínima proposta de 8dp; expansão automática invisível não pode gerar hitboxes sobrepostas.
- Chips informativos não são botões. Seletores usam `selectable`/`toggleable`, grupos e `stateDescription`; não callbacks vazios.
- TalkBack lê título, estado, prioridade e prazo sem duplicar descrição de ícone/label. Menus «Mais opções» identificam a tarefa. Gráficos têm equivalente textual e ordem de leitura.
- Foco visível: anel 3dp com afastamento 2dp; em superfícies neutras usar `focus`; perto de acento claro usar contorno `ink` e separador de superfície. Validar todos os fundos adjacentes, não apenas a cor do botão.
- Font scale 1.0/1.3/2.0; labels, erros, diálogos e barra de navegação não recortam. A 2.0, chips quebram em linhas e linhas crescem. Não limitar o fontScale para «consertar» o layout.
- Remover animações deve manter todo estado e feedback textual. Política de redução em [Motion](docs/design/ESPECIFICACAO.md#3-motion-e-feedback).
- Erros corrigíveis permanecem perto do campo; snackbar não é o único lugar onde uma validação aparece. Respeitar tempo recomendado de acessibilidade em mensagens efêmeras.
- Testar pt-BR/EN, teclado externo, Switch Access, paisagem, IME e Back. Subscrição/permissão não é disfarçada como obrigatória.

## 7. Decisões e próximos passos

**Recomendação de stack:** Compose Foundation/Animation + wrappers Material 3 em `:core:ui`, vetores locais e fontes locais. Avaliar Lottie Compose para uma ilustração original; Rive/Compose Unstyled somente se um protótipo demonstrar necessidade. Vico/Calendar/Coil têm gatilhos funcionais, não entram só para «deixar bonito».

**Pendente para a fase de implementação:** validar visualmente paleta A e fontes em três telas piloto; confirmar hipótese de público; definir criação de assets e suporte desejado a panes em tablet. A recomendação é suficientemente especificada para prototipar, mas não representa aprovação estética do usuário nem testes de usabilidade realizados.

**Primeiro recorte:** galeria de componentes + Projetos, detalhe de projeto e login em light/dark; depois demais fluxos. Seguir [migração e critérios de aceite](docs/design/ESPECIFICACAO.md#5-migração-incremental).

**verify:** `python3 docs/design/verify_tokens.py` — executável sem dependências, calcula a matriz da proposta. Não substitui testes Compose, verificação de pixels ou TalkBack no app futuro.
