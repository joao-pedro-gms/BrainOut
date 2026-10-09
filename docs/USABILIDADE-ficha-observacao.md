# Ficha de Observação Cronometrada — Sessões de Usabilidade BrainOut

> **Instrumento do anexo citado em `USABILIDADE.md:98-101`.**
>
> - Preencher **uma ficha por participante**, em tempo real, durante a
>   sessão presencial — não depois, de memória.
> - Identificação do participante: **apenas a sigla `P1`–`P5`**.
>   Não anote nome, e-mail, telefone, matrícula nem número de série.
> - Tarefas observadas: **T1**, **T2**, **T3** conforme
>   `USABILIDADE.md:67-71`; comandos exatos em `USABILIDADE.md:148-189`.
> - Cronometragem: use o **cronômetro digital** listado em
>   `USABILIDADE.md:236`, não o relógio do notebook. Os tempos
>   registrados aqui são transcritos das marcas do cronômetro.
> - Documento-mãe: [`docs/USABILIDADE.md`](./USABILIDADE.md).
> - Casos funcionais de referência: [`docs/ROTEIRO-TESTES.md`](./ROTEIRO-TESTES.md)
>   (TF-05, TF-13, TF-07, TF-12).
> - Achado registrado aqui vira linha na tabela 2.1 de
>   `USABILIDADE.md` e, conforme severidade, entrada em
>   [`docs/DEFEITOS.md`](./DEFEITOS.md).

---

## 1. Identificação da sessão

Preencher **antes** de iniciar as tarefas.

| Campo | P1 | P2 | P3 | P4 | P5 |
|-------|----|----|----|----|----|
| Sigla da sessão | P1 | P2 | P3 | P4 | P5 |
| **ID da sessão** (ex.: `S-P1-2026-11-17`) | __________ | __________ | __________ | __________ | __________ |
| **Data** da sessão | ____/____/______ | ____/____/______ | ____/____/______ | ____/____/______ | ____/____/______ |
| **Hora de início** do cronômetro (T1) | ____h:____ | ____h:____ | ____h:____ | ____h:____ | ____h:____ |
| **Hora de término** do cronômetro (T3) | ____h:____ | ____h:____ | ____h:____ | ____h:____ | ____h:____ |
| **Versão do Android** do aparelho | __________ | __________ | __________ | __________ | __________ |
| Modelo do aparelho (sem nº de série) | __________ | __________ | __________ | __________ | __________ |
| `POST_NOTIFICATIONS` concedida? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| `adb screenrecord` iniciado? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| Conta de demonstração usada ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| Mediador leu o TCLE e colheu assinatura? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| `screenrecord` **apagado** do aparelho ao fim? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |

> Preencher **apenas** as colunas da sigla da sessão. As demais ficam
> em branco.
>
> O TCLE deve estar assinado **antes** da linha
> `adb screenrecord iniciado`. Se o TCLE não foi assinado, a sessão
> **não** é válida para o E4.2 — anote aqui e suspenda as tarefas.

---

## 2. T1 — Criar um projeto com uma tag associada

> **Comando lido** (`USABILIDADE.md:150-155`):
> "Abra o app. Imagine que você precisa organizar um trabalho da
> faculdade chamado **'Projeto Integrador'**. Crie esse projeto no app
> e associe a ele a tag **Estudo** que já existe nas configurações."
>
> **Alvo de tempo: 3 min** (`USABILIDADE.md:69`).
> **Casos de referência: TF-05** (criar projeto) + **TF-13**
> (associar tags).
> **Tempo medido a partir da leitura do comando.**

| Campo de observação | P1 | P2 | P3 | P4 | P5 |
|---------------------|----|----|----|----|----|
| **Tempo até conclusão** (mm:ss) — alvo 3:00 | ________ | ________ | ________ | ________ | ________ |
| Concluída sem ajuda do mediador? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| **Nº de erros** (tentativa que falha e exige refazer) | ______ | ______ | ______ | ______ | ______ |
| **Nº de pedidos de ajuda** (o participante pede pista) | ______ | ______ | ______ | ______ | ______ |
| Onde procurou o botão "novo projeto"? | ________________ | ________________ | ________________ | ________________ | ________________ |
| Encontrou a forma de associar a tag? ( ) S ( ) N — se N, descrever | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ |
| O nome "Projeto Integrador" foi aceito sem objeção? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| **Hesitações** (onde parou > 15 s; o que estava procurando) | ________________ | ________________ | ________________ | ________________ | ________________ |
| **Comentários literais** (entre aspas; transcrever exato) | ________________ | ________________ | ________________ | ________________ | ________________ |

> **Hesitação** = pausa superior a 15 s, sem fala. O mediador só
> intervene com "continue pensando em voz alta" após 15 s de silêncio
> (`USABILIDADE.md:92-93`); a intervenção **não** conta como pista.
>
> **Comentário literal** = fala do participante transcrita entre
> aspas, sem paráfrase e sem correção de gramática. É citável no
> relatório (`USABILIDADE.md:2.3`).

---

## 3. T2 — Criar uma tarefa com prazo dentro do projeto

> **Comando lido** (`USABILIDADE.md:167-170`):
> "Dentro do projeto Projeto Integrador, crie uma tarefa chamada
> 'Entregar slide da N2' com prazo para **a próxima quarta-feira**."
>
> **Alvo de tempo: 3 min** (`USABILIDADE.md:70`).
> **Caso de referência: TF-07** (criar tarefa válida, com prazo).
> Tempo medido a partir da leitura do comando.

| Campo de observação | P1 | P2 | P3 | P4 | P5 |
|---------------------|----|----|----|----|----|
| **Tempo até conclusão** (mm:ss) — alvo 3:00 | ________ | ________ | ________ | ________ | ________ |
| Concluída sem ajuda do mediador? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| **Nº de erros** | ______ | ______ | ______ | ______ | ______ |
| **Nº de pedidos de ajuda** | ______ | ______ | ______ | ______ | ______ |
| Como escolheu a data? ( ) Calendário nativo ( ) Digitação ( ) Outro: ______ | ____________ | ____________ | ____________ | ____________ | ____________ |
| Tentou ajustar o **horário** do prazo? ( ) S ( ) N — o app aceita só dia | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| Compreendeu que pode ou não atribuir a tarefa a si mesmo? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| Dificuldade com seletor de prioridade? ( ) S ( ) N — descrever | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ | ( ) S ( ) N<br>______ |
| **Hesitações** | ________________ | ________________ | ________________ | ________________ | ________________ |
| **Comentários literais** (entre aspas) | ________________ | ________________ | ________________ | ________________ | ________________ |

---

## 4. T3 — Concluir a tarefa criada

> **Comando lido** (`USABILIDADE.md:183-184`):
> "Marque a tarefa 'Entregar slide da N2' como concluída."
>
> **Alvo de tempo: 1 min** (`USABILIDADE.md:71`).
> **Caso de referência: TF-12** (ação de concluir / `DONE`).
> Tempo medido a partir da leitura do comando.

| Campo de observação | P1 | P2 | P3 | P4 | P5 |
|---------------------|----|----|----|----|----|
| **Tempo até conclusão** (mm:ss) — alvo 1:00 | ________ | ________ | ________ | ________ | ________ |
| Concluída sem ajuda do mediador? ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N | ( ) S ( ) N |
| **Nº de erros** | ______ | ______ | ______ | ______ | ______ |
| **Nº de pedidos de ajuda** | ______ | ______ | ______ | ______ | ______ |
| **Ação usada** (ver seção 5) | ____________ | ____________ | ____________ | ____________ | ____________ |
| **Hesitações** | ________________ | ________________ | ________________ | ________________ | ________________ |
| **Comentários literais** (entre aspas) | ________________ | ________________ | ________________ | ________________ | ________________ |

### 4.1 Pergunta de fechamento da T3

> Transcrever **a resposta literal** da participante
> (`USABILIDADE.md:188-189`).

**Pergunta:** "O status final é evidente sem precisar abrir
detalhes?"

| Sigla | Resposta literal ( ) Sim ( ) Não | Anotação do mediador |
|-------|-----------------------------------|-----------------------|
| P1 | ( ) Sim ( ) Não | ________________________________________ |
| P2 | ( ) Sim ( ) Não | ________________________________________ |
| P3 | ( ) Sim ( ) Não | ________________________________________ |
| P4 | ( ) Sim ( ) Não | ________________________________________ |
| P5 | ( ) Sim ( ) Não | ________________________________________ |

---

## 5. Afordância de exibição de status (achado da T3)

> **Marcar com ( ) a ação efetivamente usada** pelo participante para
> concluir a tarefa. Se mais de uma foi tentada, marcar todas na ordem
> e descrever na coluna de observação. Se nenhuma das cinco opções
> servir, marcar "outra" e **descrever o que o participante fez** —
> uma afordância não catalogada aqui é achado, não erro de
> preenchimento.

| Afordância usada para concluir | P1 | P2 | P3 | P4 | P5 |
|--------------------------------|----|----|----|----|----|
| Toque longo (long-press) | ( ) | ( ) | ( ) | ( ) | ( ) |
| Deslizar (swipe) | ( ) | ( ) | ( ) | ( ) | ( ) |
| Menu (⋯ / opções) | ( ) | ( ) | ( ) | ( ) | ( ) |
| Checkbox | ( ) | ( ) | ( ) | ( ) | ( ) |
| Botão dedicado ("Concluir") | ( ) | ( ) | ( ) | ( ) | ( ) |
| Outra: ____________________________________ | ( ) | ( ) | ( ) | ( ) | ( ) |
| Não encontrou forma alguma (não concluiu) | ( ) | ( ) | ( ) | ( ) | ( ) |

**Descrição da afordância "outra" ou da tentativa fracassada:**

| Sigla | Descrição |
|-------|-----------|
| P1 | ______________________________________________________ |
| P2 | ______________________________________________________ |
| P3 | ______________________________________________________ |
| P4 | ______________________________________________________ |
| P5 | ______________________________________________________ |

---

## 6. Questionário pós-tarefa — registro de respostas

> Aplicar ao final da sessão (5 min), lendo os itens em voz alta na
> ordem original. O instrumento completo, com todas as âncoras e o
> cálculo da pontuação, está em
> [`docs/USABILIDADE-SUS.md`](./USABILIDADE-SUS.md).
> **Não registre respostas de participante que não aplicou todos os 10 itens.**

### 6.1 Respostas Likert (1 a 5)

| # | Item (resumo) | P1 | P2 | P3 | P4 | P5 |
|---|---------------|----|----|----|----|----|
| 1 | Gostaria de usar com frequência | ____ | ____ | ____ | ____ | ____ |
| 2 | Desnecessariamente complexo | ____ | ____ | ____ | ____ | ____ |
| 3 | Fácil de usar | ____ | ____ | ____ | ____ | ____ |
| 4 | Precisaria de ajuda técnica | ____ | ____ | ____ | ____ | ____ |
| 5 | Funções bem integradas | ____ | ____ | ____ | ____ | ____ |
| 6 | Muita inconsistência | ____ | ____ | ____ | ____ | ____ |
| 7 | A maioria aprenderia rápido | ____ | ____ | ____ | ____ | ____ |
| 8 | Muito complicado de usar | ____ | ____ | ____ | ____ | ____ |
| 9 | Me senti muito confiante | ____ | ____ | ____ | ____ | ____ |
| 10 | Precisei aprender muita coisa antes | ____ | ____ | ____ | ____ | ____ |

### 6.2 Pontuação SUS calculada

| Sigla | Soma dos 10 valores | **Pontuação SUS (0–100)** = soma × 2,5 | Faixa (acima da média / média / abaixo de 68) |
|-------|---------------------|------------------------------------------|------------------------------------------------|
| P1 | ______ | ______ | ____________________________ |
| P2 | ______ | ______ | ____________________________ |
| P3 | ______ | ______ | ____________________________ |
| P4 | ______ | ______ | ____________________________ |
| P5 | ______ | ______ | ____________________________ |

> Cálculo: itens **ímpares** (1, 3, 5, 7, 9) subtrair 1 da resposta;
> itens **pares** (2, 4, 6, 8, 10) subtrair a resposta de 5. Somar os
> 10 valores e multiplicar por **2,5**. Detalhes e exemplos em
> `docs/USABILIDADE-SUS.md`.

### 6.3 Perguntas abertas (transcrever literalmente)

| Sigla | O que mais te frustrou? | O que mais te agradou? | O que mudaria para recomendar a um colega? |
|-------|-------------------------|------------------------|-----------------------------------------------|
| P1 | ________________ | ________________ | ________________ |
| P2 | ________________ | ________________ | ________________ |
| P3 | ________________ | ________________ | ________________ |
| P4 | ________________ | ________________ | ________________ |
| P5 | ________________ | ________________ | ________________ |

---

## 7. Registro de defeitos — para onde vai cada achado

> **Cada linha desta seção vira uma linha da tabela 2.1 de**
> `USABILIDADE.md` **e** uma entrada `DEF-NN` em
> [`docs/DEFEITOS.md`](./DEFEITOS.md). Escreva a descrição em uma
> frase, com o passo de reprodução quando houver.

| ID da ficha (ex.: `FO-P3-01`) | Sigla | Tarefa (T1/T2/T3) | Descrição objetiva do achado | Passos para reproduzir | Frequência acumulada (x/5) | Severidade (Bloq./Crit./Men.) | `DEF-NN` atribuído |
|-------------------------------|-------|-------------------|------------------------------|-------------------------|----------------------------|-------------------------|--------------------|
| ____________ | ____ | ____ | ______________________________ | ____________________ | ______ | ______ | __________ |
| ____________ | ____ | ____ | ______________________________ | ____________________ | ______ | ______ | __________ |
| ____________ | ____ | ____ | ______________________________ | ____________________ | ______ | ______ | __________ |
| ____________ | ____ | ____ | ______________________________ | ____________________ | ______ | ______ | __________ |
| ____________ | ____ | ____ | ______________________________ | ____________________ | ______ | ______ | __________ |

**Severidade** conforme a política do E4.3
([`docs/DEFEITOS.md`](./DEFEITOS.md), Seção 1):

| Severidade | Critério |
|------------|----------|
| **Bloqueante** | Impede a execução de um fluxo crítico (app fecha, perde dados, inutiliza o caso principal). |
| **Crítico** | Degrada um fluxo crítico, mas existe contorno utilizável. |
| **Menor** | Cosmético, textual, animação, mensagem ambígua ou *edge case* que não bloqueia. |

> A **frequência** só pode ser preenchida depois de contar em **todas
> as cinco** sessões. Um defeito visto só em P3 é `1/5` — e isso já é
> informação: 1/5 Bloqueante entra como obrigatório no E4.3.

---

## 8. Checklist da sessão

> Preencher uma vez por sessão, antes de arquivar a ficha.

- [ ] Sigla atribuída (`P1`–`P5`); **nenhum nome, e-mail, telefone,
      matrícula ou nº de série** em nenhum campo desta ficha.
- [ ] TCLE lido em voz alta e **assinatura colhida** antes de iniciar
      as tarefas
      ([`docs/USABILIDADE-tcle-TCLE.md`](./USABILIDADE-tcle-TCLE.md)).
- [ ] Tarefas executadas na ordem **T1 → T2 → T3**, com os comandos
      de `USABILIDADE.md:148-189` lidos literalmente.
- [ ] Nenhum outro participante presente na sala durante a sessão
      (`USABILIDADE.md:89-91`).
- [ ] `POST_NOTIFICATIONS` e início do `adb screenrecord`
      registrados na Seção 1.
- [ ] Tempos de T1, T2 e T3 anotados em mm:ss.
- [ ] Pergunta de fechamento da T3 respondida e **afordância de
      status** marcada na Seção 5.
- [ ] Os **10 itens do SUS** respondidos e a pontuação calculada
      (Seção 6.2).
- [ ] As **3 perguntas abertas** transcritas literalmente.
- [ ] Todo achado da Seção 7 tem descrição de uma frase e foi
      encaminhado para `docs/DEFEITOS.md`.
- [ ] `screenrecord` **apagado do aparelho do participante** ao fim
      (`USABILIDADE.md:229`).
- [ ] Arquivo de `screenrecord` do autor listado para apagamento após
      o preenchimento da Parte 2 de `USABILIDADE.md`
      (`USABILIDADE.md:95-97`).

---

*Instrumento do marco **E4.2** do Ciclo 4 do Projeto Integrador.
Anexo citado em `docs/USABILIDADE.md:98-101`; tarefas em
`USABILIDADE.md:67-71` e `USABILIDADE.md:148-189`; política de
severidade em `docs/DEFEITOS.md`. Elaborado por **João Pedro G M
Silva** — ADS, PUC Goiás.*
