# Plano e Relatório de Sessões de Usabilidade — BrainOut (Ciclo 4 / E4.2)

> Documento em duas partes: **(1)** Plano das sessões de teste de
> usabilidade com 5 colegas da turma ADS e **(2)** Relatório de
> achados a ser preenchido após a execução presencial.
>
> Este material atende ao item **N2 item 3** do documento norteador e é
> insumo direto para o **registro de defeitos do E4.3** e para o
> **relatório técnico da N2 (E5.1)**.
>
> Referências cruzadas:
>
> - Roteiro de casos funcionais a executar durante as sessões:
>   [`docs/ROTEIRO-TESTES.md`](./ROTEIRO-TESTES.md) (TF-01..TF-14).
> - Marcos e cronograma:
>   [`docs/ROADMAP.md`](./ROADMAP.md) — bloco do Ciclo 4.
> - Política de severidade de defeitos: E4.3 (Bloqueante / Crítico /
>   Menor), alinhada ao item 6.3 do documento norteador.

---

## Parte 1 — Plano das sessões

### 1.1 Objetivo

Identificar barreiras de uso, ambiguidades textuais e problemas de
fluxo no BrainOut, com foco em **operações frequentes** (criar projeto,
criar tarefa, concluir tarefa) executadas por usuários cujo perfil
assimila o público-alvo do app: estudantes universitários de cursos
tecnológicos que gerenciam trabalhos em grupo e prazos individuais.

### 1.2 Perfil dos participantes

| Sigla | Perfil resumido                                          | Dispositivo próprio   |
|-------|----------------------------------------------------------|-----------------------|
| P1    | Colega ADS — usa Android 13, smartphone intermediário    | Aparelho pessoal      |
| P2    | Colega ADS — usa Android 14, smartphone topo de linha    | Aparelho pessoal      |
| P3    | Colega ADS — usuária Android desde versão 10, não-técnica| Aparelho pessoal      |
| P4    | Colega ADS — uso intenso de apps de produtividade        | Aparelho pessoal      |
| P5    | Colega ADS — primeira experiência com apps de tarefas    | Aparelho pessoal      |

> **Sigilo e privacidade.** Os nomes reais dos participantes não são
> registrados neste documento nem no relatório subsequente; cada um é
> identificado apenas pela sigla `P1`–`P5`. Os dados coletados
> limitam-se a idade, nível de familiaridade com Android e modelo do
> dispositivo (sem número de série, IMEI, conta Google, e-mail ou
> qualquer dado pessoal). Nenhuma captura de tela exibirá conteúdo
> pessoal do participante; as evidências mostrarão apenas a tela do
> BrainOut.

### 1.3 Critérios de inclusão

- Estudante regularmente matriculado em ADS da PUC Goiás.
- Usuário de smartphone Android com **Android 10 ou superior**.
- Sem vínculo prévio com o projeto BrainOut (autor ou dupla).
- Assinatura de termo de consentimento livre e esclarecido (TCLE)
  autorizando observação presencial, registro de tela do BrainOut e
  uso anonimizado dos achados em entregas acadêmicas.

### 1.4 Cenários a executar

Os cenários derivam do [`docs/ROTEIRO-TESTES.md`](./ROTEIRO-TESTES.md).
Cada sessão cobre **três tarefas-chave**, executadas na ordem abaixo.
Os tempos são alvos — tempos muito curtos indicam fluência; tempos
muito longos ou pausas prolongadas viram achados.

| #   | Tarefa do participante                              | Caso(s) de referência | Tempo-alvo |
|-----|-----------------------------------------------------|-----------------------|------------|
| T1  | Criar um projeto **com uma tag** associada          | TF-05, TF-13          | 3 min      |
| T2  | Criar uma tarefa **com prazo** dentro do projeto    | TF-07                 | 3 min      |
| T3  | Concluir a tarefa criada                            | TF-12 (ação manual)   | 1 min      |

**Pré-condição geral para todas as sessões:**

- APK debug do BrainOut instalada no celular do participante (banco
  limpo: `adb shell pm clear pucgo.joaopedrogmsilva.brainout.debug`).
- Uma conta de demonstração já criada no dispositivo do participante,
  com nome fictício e tag `Estudo` previamente cadastrada nas
  configurações.
- Plano de internet do próprio participante (Wi-Fi ou 4G/5G).

> Os cenários completos do roteiro (TF-01..TF-14) podem ser cobertos
> pelos mesmos 5 participantes em janelas adicionais, mas as três
> tarefas acima são o **núcleo observável** desta avaliação de
> usabilidade.

### 1.5 Método

- **Observação presencial individual** (mediador + participante, um
  por vez; sem outros participantes no recinto durante a sessão).
- **Think-aloud** (verbalização simultânea): o participante fala em
  voz alta o que está tentando fazer, o que espera e o que observa na
  tela. O mediador não orienta; só pede "continue pensando em voz
  alta" se o participante ficar em silêncio por mais de 15 s.
- **Registro de tela do BrainOut** (via `adb screenrecord`) para
  reanálise posterior. O arquivo de vídeo fica sob custódia do autor
  e é apagado após o preenchimento da Parte 2.
- **Notas cronometradas** do mediador em formulário de observação
  (anexo a este documento): tempo de cada tarefa, número de erros,
  hesitações, pedidos de ajuda, comentários literais do
  participante.
- **Questionário pós-tarefa** (Seção 1.7) aplicado ao fim da sessão,
  com 10 perguntas Likert de 5 pontos (SUS — System Usability Scale,
  Brooke, 1996) + 3 perguntas abertas qualitativas.

### 1.6 Cronograma

| Atividade                                       | Data prevista          | Local                  |
|-------------------------------------------------|------------------------|------------------------|
| Sessão P1                                       | 17/11/2026, 14h        | Sala de reunião do bloco acadêmico |
| Sessão P2                                       | 17/11/2026, 15h        | Sala de reunião do bloco acadêmico |
| Sessão P3                                       | 18/11/2026, 14h        | Sala de reunião do bloco acadêmico |
| Sessão P4                                       | 18/11/2026, 15h        | Sala de reunião do bloco acadêmico |
| Sessão P5                                       | 19/11/2026, 14h        | Sala de reunião do bloco acadêmico |
| Consolidação de notas e preenchimento da Parte 2 | 20/11 a 22/11/2026     | Remoto                 |
| Revisão por João e abertura do PR de follow-up  | 23/11/2026             | Remoto                 |

> A janela "semana de 17/11" está alinhada ao congelamento de escopo
> (E4.8 em 27/11), permitindo ajustes antes do fim do Ciclo 4.

### 1.7 Roteiro da sessão

> O script abaixo é o **template** a ser seguido em cada uma das 5
> sessões. Marque o tempo no cronômetro a partir do início da T1.

#### 1.7.1 Abertura (3 min)

1. Receber o participante, oferecer água e deixar o celular em uma
   mesa próxima.
2. Ler em voz alta o termo de consentimento livre e esclarecido
   (TCLE) e colher assinatura (formulário físico ou digital, à
   escolha do participante).
3. Explicar o cenário em linguagem neutra:

   > "Você vai usar um aplicativo de gestão de projetos e tarefas
   > chamado BrainOut. Ele ainda está em desenvolvimento. Eu vou te
   > pedir três tarefas curtas; enquanto isso, peço que **fale em voz
   > alta** o que está tentando fazer e o que está achando. Se não
   > souber o que fazer, pode dizer também — isso é importante. Não
   > estou avaliando você, e sim o aplicativo. Se eu ficar em
   > silêncio, é proposital: só estou observando."

4. Confirmar que o celular do participante está em modo avião
   **desativado** e que a permissão `POST_NOTIFICATIONS` será
   concedida quando o app pedir (caso o participante autorize).
5. Iniciar o cronômetro e o `adb screenrecord`.

#### 1.7.2 Tarefa T1 — Criar um projeto com tag (3 min alvo)

**Comando:**

> "Abra o app. Imagine que você precisa organizar um trabalho da
> faculdade chamado **'Projeto Integrador'**. Crie esse projeto no
> app e associe a ele a tag **Estudo** que já existe nas
> configurações."

**Observar e anotar (sem interromper):**

- Onde o participante procura o botão "novo projeto".
- Se ele encontra a forma de associar tag ao projeto, ou se hesita.
- Quantas tentativas / erros até concluir.
- Se o nome "Projeto Integrador" foi aceito sem objeção.
- Comentários literais marcantes.

#### 1.7.3 Tarefa T2 — Criar uma tarefa com prazo (3 min alvo)

**Comando:**

> "Dentro do projeto Projeto Integrador, crie uma tarefa chamada
> 'Entregar slide da N2' com prazo para **a próxima quarta-feira**."

**Observar e anotar:**

- Como o participante escolhe a data (calendário nativo, picker,
  digitação).
- Se ele ajusta o **horário** do prazo (o app aceita só dia).
- Se ele compreende que pode ou não atribuir a tarefa a si mesmo.
- Dificuldades com seletor de prioridade, se houver.

#### 1.7.4 Tarefa T3 — Concluir a tarefa (1 min alvo)

**Comando:**

> "Marque a tarefa 'Entregar slide da N2' como concluída."

**Observar e anotar:**

- Ação por toque longo, swipe, menu, checkbox, botão dedicado, etc.
- Se o status final é evidente sem precisar abrir detalhes.

#### 1.7.5 Questionário pós-tarefa (5 min)

Aplicar as 10 perguntas do **System Usability Scale** (SUS, Brooke,
1996) em escala Likert de 5 pontos (1 = Discordo totalmente,
5 = Concordo totalmente). Itens em ordem original, mesclados entre
positivos e negativos para reduzir viés de aquiescência:

1. Eu acho que gostaria de usar este app com frequência.
2. Eu acho o app desnecessariamente complexo.
3. Eu acho o app fácil de usar.
4. Eu acho que precisaria de ajuda de alguém com conhecimentos
   técnicos para usar o app.
5. Eu acho que as funções do app estão bem integradas.
6. Eu acho que há muita inconsistência no app.
7. Eu imagino que a maioria das pessoas aprenderia a usar o app
   muito rapidamente.
8. Eu achei o app muito complicado de usar.
9. Eu me senti muito confiante ao usar o app.
10. Eu precisei aprender muitas coisas antes de conseguir usar o
    app.

> Cálculo da pontuação SUS: para itens ímpares (1, 3, 5, 7, 9)
> subtrair 1 da resposta; para itens pares (2, 4, 6, 8, 10) subtrair
> a resposta de 5. Somar os 10 valores e multiplicar por 2,5.
> Resultado em escala 0–100. Acima de 68 é considerado "acima da
> média"; abaixo de 50 indica problemas graves de usabilidade.

**Perguntas abertas qualitativas (opcional, 1 min):**

- O que mais te frustrou durante as tarefas?
- O que mais te agradou?
- O que mudaria para recomendar o app a um colega?

#### 1.7.6 Encerramento (1 min)

1. Parar o cronômetro e o `screenrecord`.
2. Agradecer e lembrar que o contato posterior será apenas para
   eventual esclarecimento, sem novas coletas de dados.
3. Apagar o arquivo de `screenrecord` do celular do participante.

### 1.8 Recursos materiais

- 1 celular com a APK debug instalada por participante (pode ser o
  próprio aparelho do participante, desde que Android 10+).
- 1 notebook do mediador para anotações cronometradas.
- 1 cronômetro digital.
- 1 roteador Wi-Fi disponível (apenas para garantir conectividade;
  o foco é UX, não performance).
- TCLE impresso ou em formulário digital (1 por participante).

### 1.9 Riscos e mitigações

| Risco                                                          | Mitigação                                                                 |
|----------------------------------------------------------------|---------------------------------------------------------------------------|
| Conflito de horário dos participantes                           | Janela de 3 dias úteis; reagendamento até 19/11 com aviso prévio de 24h   |
| Participante sem Android 10+                                   | Substituição por voluntário da lista de espera de ADS                     |
| Falha de conectividade durante a sessão                        | Cenários offline (E3.4) também estão no roteiro; aceita-se a perda       |
| Viés de mediação (mediador interfere na tarefa)                | Script fixo e lembrete ao mediador antes de cada sessão                  |

### 1.10 Critérios de pronto do plano (E4.2 — plano)

- [x] Perfil dos 5 participantes definido (sigilo `P1`–`P5`).
- [x] Três tarefas centrais mapeadas (T1 projeto+tag, T2
      tarefa+prazo, T3 concluir).
- [x] Método (observação presencial + think-aloud + screenrecord)
      especificado.
- [x] Cronograma com datas e local.
- [x] Roteiro da sessão de 15 min com script de boas-vindas,
      comandos exatos, itens de observação e questionário SUS.
- [x] Pontuação SUS e referência bibliográfica (Brooke, 1996).

---

## Parte 2 — Relatório de achados

> **Esta parte é preenchida após as sessões presenciais (semana de
> 17/11 a 19/11/2026).** Os campos abaixo ficam em **branco até a
> execução**, em conformidade com a regra da `body` do card de que o
> worker **não invente resultados de sessões que não aconteceram**.
>
> O preenchimento da Parte 2 deve acontecer em **PR de follow-up** a
> este, aberto após as cinco sessões e contendo os registros de
> `screenrecord`, as planilhas de observação e os cálculos SUS por
> participante.

### 2.1 Tabela-mestre de achados

> Preencher uma linha por achado identificado durante as sessões.
> Severidade segue a política do E4.3: **Bloqueante** (impede a
> tarefa), **Crítico** (degrada sem contorno), **Menor** (cosmético
> ou ambiguidade).

| ID    | Sessão(s) em que apareceu | Tarefa afetada (T1/T2/T3) | Descrição objetiva do achado                                                                                              | Evidência (referência a `screenrecord` ou nota do mediador) | Frequência (x/5) | Severidade    | Sugestão de correção                                                                                          | Prioridade |
|-------|---------------------------|--------------------------|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|------------------|---------------|---------------------------------------------------------------------------------------------------------------|------------|
| A-01  | _pendente_                | _pendente_               | _a preencher após sessão_                                                                                                | _a preencher_                                              | _/5_             | _Bloq/Crít/Men_| _a preencher_                                                                                                | _Alta/Média/Baixa_ |

### 2.2 Métricas agregadas

> Preencher após as 5 sessões.

- **Pontuação SUS por participante:**

  | Sigla | Pontuação SUS (0–100) | Faixa         |
  |-------|------------------------|---------------|
  | P1    | _a preencher_          | —             |
  | P2    | _a preencher_          | —             |
  | P3    | _a preencher_          | —             |
  | P4    | _a preencher_          | —             |
  | P5    | _a preencher_          | —             |
  | **Média** | _a preencher_      | _suspensa_    |

- **Tempo médio por tarefa (em segundos):**

  | Tarefa | Média (s) | Mínimo (s) | Máximo (s) | Observação                          |
  |--------|-----------|------------|------------|-------------------------------------|
  | T1     | _a preen._| _a preen._ | _a preen._ | —                                   |
  | T2     | _a preen._| _a preen._ | _a preen._ | —                                   |
  | T3     | _a preen._| _a preen._ | _a preen._ | —                                   |

- **Taxa de conclusão por tarefa (com sucesso sem mediação):**

  | Tarefa | Concluíram sem ajuda (x/5) | Precisaram de pista verbal | Não concluíram |
  |--------|-----------------------------|----------------------------|-----------------|
  | T1     | _a preen._                  | _a preen._                 | _a preen._      |
  | T2     | _a preen._                  | _a preen._                 | _a preen._      |
  | T3     | _a preen._                  | _a preen._                 | _a preen._      |

### 2.3 Achados qualitativos (perguntas abertas)

> Listar, agrupando por tema, os comentários literais mais
> representativos dos 5 participantes ao final da sessão. Citar entre
> aspas e indicar a sigla (`P1`..`P5`).

#### 2.3.1 O que mais frustrou

- _a preencher_

#### 2.3.2 O que mais agradou

- _a preencher_

#### 2.3.3 O que mudariam

- _a preencher_

### 2.4 Conclusão e recomendações

> Resumo executivo (≤ 200 palavras) cruzando achados com o E4.3
> (severidade, registro de defeitos) e propondo ordens de correção.
> _A preencher após as sessões._

### 2.5 Critérios de pronto do relatório (E4.2 — relatório)

- [ ] As 5 sessões foram executadas e os 5 TCLE arquivados.
- [ ] Os 5 arquivos de `screenrecord` foram revisados e apagados.
- [ ] A tabela 2.1 contém **um registro por achado** com severidade,
      evidência e prioridade.
- [ ] A pontuação SUS foi calculada por participante e a média
      registrada.
- [ ] Os comentários qualitativos foram agrupados por tema.
- [ ] Os defeitos classificados como **Bloqueante** ou **Crítico**
      foram importados para o registro oficial de defeitos (E4.3),
      com referência cruzada ao `ID` da tabela 2.1.
- [ ] PR de follow-up aberto atualizando esta Parte 2 e referenciando
      os artefatos de evidência.

---

## Apêndice A — Termo de Consentimento Livre e Esclarecido (modelo)

> Disponível em formato físico e digital. Conteúdo resumido abaixo
> para conferência no momento da sessão.

```
Você está sendo convidado(a) a participar de uma sessão de teste de
usabilidade do aplicativo BrainOut, desenvolvido como Projeto
Integrador do curso de Análise e Desenvolvimento de Sistemas da
PUC Goiás. A sessão será presencial, com duração aproximada de 15
minutos, conduzida pelo autor do projeto.

O QUE FAREMOS: você realizará três tarefas curtas dentro do
aplicativo enquanto fala em voz alta o que está pensando, e ao
final responderá a um questionário de 10 perguntas.

O QUE É COLETADO: idade, nível de familiaridade com Android e
modelo do seu celular, mais notas de observação do mediador
sobre as tarefas. Não coletamos nome, e-mail, telefone, fotos ou
conteúdo pessoal do seu celular.

COMO USAREMOS: apenas para fins acadêmicos deste projeto
integrador, em relatórios e apresentações da disciplina. Os
achados serão apresentados de forma agregada ou anonimizada
(sigla P1 a P5).

SEUS DIREITOS: você pode parar de participar a qualquer momento,
sem qualquer prejuízo. Ao final, o arquivo de gravação de tela
será apagado do seu celular.

CONTATO: João Pedro G M Silva — ADS / PUC Goiás.
```

---

## Apêndice B — Referências normativas e bibliográficas

- **BROOKE, J.** *SUS: A "quick and dirty" usability scale*. In:
  JORDAN, P. W.; THOMAS, B.; WEERDMEESTER, B. A.; McCLELLAND,
  I. L. (Ed.). Usability evaluation in industry. London: Taylor
  & Francis, 1996. p. 189–194. (SUS — System Usability Scale;
  base do questionário de 10 itens da Seção 1.7.5.)
- **Documento Norteador Projeto Integrador ADS 2026-2** — PUC Goiás
  (ver `Documentos/Documento Norteador Projeto Integrador ADS
  2026-2.pdf`, especialmente itens 6.3 — registro de defeitos — e
  N2 item 3 — plano e relatório de testes).
- **`docs/ROTEIRO-TESTES.md`** — fonte dos casos funcionais
  adaptados aos cenários T1/T2/T3 das sessões.
- **`docs/ROADMAP.md`** — marcos E4.2 e E4.3 do Ciclo 4.

---

*Documento elaborado por **João Pedro G M Silva** — ADS, PUC Goiás
(mat. 20251012000740), como entrega do marco **E4.2** do Ciclo 4 do
Projeto Integrador.*
