# LOCAL-02 — interface do aplicativo local (#213)

Base: `7f02bd5`. Os banners, contadores e fontes de estado remoto já foram
retirados pela #212. Esta etapa alinha os textos restantes, sem mudar
autenticação, permissões de Owner/Member, navegação ou lembretes.

## Mudanças

- Abertura: projetos e tarefas neste aparelho, sem promessa de equipe remota.
- Login e cadastro: deixam explícito que a conta pertence a este aparelho.
- Cadastro Owner: gerenciamento de projetos locais. O diálogo de restrição
  de Member orienta entrar em uma conta local Owner, sem prometer um upgrade.
- Configurações: conta local e preferências do aplicativo.
- Prazo: falha no cálculo de feriados informa o problema da data, sem pedir
  conexão. O aviso não bloqueia a escolha ou a limpeza do prazo.
- Textos alinhados em pt/en, inclusive as chaves duplicadas em `:app`, que
  prevalecem no APK mesclado. Tags e comentários dos banners removidos
  também foram retirados das telas de projetos.

## Regressão automatizada

`DeadlineFieldTest` verifica a falha do cálculo local, a recuperação ao
selecionar outra data e a limpeza do prazo; também exercita a falha em inglês.
Os testes existentes de ViewModel e banners continuam sendo a referência
para erros/retry de leitura e escrita, sessão e operações locais. Não há
substituição desses erros por estado de conectividade.

Em 19/10/2026, Windows/JDK 21: **524 testes JVM, zero falhas/erros/ignorados**
nos oito módulos. Passaram `ktlintCheck`, `detekt`, `testDebugUnitTest`,
`:core:domain:koverVerify`, `:core:data:koverVerify`, `lintDebug` e
`:app:assembleDebug`. A asserção do subtítulo de Configurações foi atualizada
após o primeiro gate detectar a expectativa antiga. A paridade de chaves
pt/en e as sobrescritas dos textos entre app/features foram conferidas.

## Conferência em emulador

AVD `Medium_Phone_API_37.0`, Android 37, 1080×2400, modo avião
(`airplane_mode_on=1`), usando uma sessão de leitura sem salvar snapshot:

- Abertura em pt/en: texto completo, CTA visível e navegação para login.
- Login/cadastro em português: subtítulos locais e ajuda do papel Owner.
- Configurações em pt/en: subtítulo local; sair retorna ao login. Reiniciar
  após sair apresenta a abertura, sem restaurar a sessão encerrada.
- Teclado de software no campo de confirmação: o formulário pode ser rolado
  até exibir os papéis e o botão Cadastrar acima do teclado. Barra superior
  e área de gestos estão visíveis neste perfil de tela.

O primeiro processo do emulador encerrou durante a tentativa de abrir o
teclado, com erro `virtio: zero sized buffers are not allowed`. A conferência
foi repetida com `-gpu software`; não houve crash do app no buffer de crash
consultado. Isso não é uma validação de todos os tamanhos de tela ou IMEs.
O emulador foi encerrado ao final, preservando os dados originais do AVD.

| Captura | Evidência |
|---|---|
| [Abertura pt](local02/abertura-pt.png) | Apresentação local |
| [Abertura en](local02/abertura-en.png) | Tradução no APK |
| [Login pt](local02/login-pt.png) | Conta neste aparelho |
| [Cadastro pt](local02/cadastro-pt.png) | Conta e papel locais |
| [Cadastro com teclado](local02/cadastro-teclado-pt.png) | CTA acessível por rolagem |
| [Configurações pt](local02/settings-pt.png) | Conta local |

## Limites de QA e pendências

TalkBack não foi executado; a inspeção de nós pelo UI Automator não substitui
a navegação pelo leitor de tela. Nenhuma validação em aparelho físico
é atribuída a esta etapa. A conferência de teclado/insets acima é limitada
ao perfil do emulador; os achados de interface #182–#191 permanecem
nas issues originais; esta entrega não os encerra por consequência da
mudança de texto. O mesmo vale para os insets globais (#180).

As opções de configurações ainda sem implementação (#182), o seletor de
prioridade para TalkBack (#186) e as demais pendências mantêm seu escopo.
Backup portátil continua pendente na #214; estes textos não prometem
restauração ou transferência entre aparelhos.
