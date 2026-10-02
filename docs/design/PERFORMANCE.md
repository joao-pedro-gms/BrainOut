# BrainOut — Roteiro de desempenho (F0 / NB-01, decisão Q27)

**Nenhuma medição foi feita.** Este documento é o **procedimento** exigido pela decisão Q27 do [plano](../plans/2026-10-01-redesign-neobrutalista.md); a execução está bloqueada por dois fatos do ambiente medidos em 02/10/2026: não há JVM no perfil padrão e **não há dispositivo conectado** (`adb devices` vazio, AVD `Medium_Phone_API_37.0` disponível). Nenhum número abaixo é resultado: são referências de leitura.

Razão de existir: o redesign muda sombra, motion, tipografia e hierarquia em toda a UI. Sem um procedimento comparável, "ficou mais pesado" vira opinião. O baseline numérico (a ser preenchido) e a medição final (NB-34) precisam usar **o mesmo build, o mesmo aparelho e o mesmo cenário**.

## 1. Regras do procedimento

1. **Nunca comparar builds diferentes**: debug contra release é inválido. Use release para ambos os lados.
2. **Mesmo aparelho**, com a taxa de atualização registrada (60 Hz e 120 Hz dão orçamentos diferentes por frame).
3. **Três execuções** por cenário; publicar as três e a variação, não só a melhor.
4. **Nenhuma captura de tela é evidência de desempenho.** Vale `am start -W`, `gfxinfo` e log.
5. Registrar SHA, variante, assinatura, aparelho, Hz, tema (claro/escuro) e fonte.

## 2. Build de medição

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

# Opção A — release assinado localmente (keystore FORA do versionamento)
#   Gere uma única vez e nunca versione (BRAINOUT_KEYSTORE_* já existem no projeto):
keytool -genkeypair -v -keystore /caminho/fora/do/repo/brainout-medicao.jks \
  -alias brainout -keyalg RSA -keysize 2048 -validity 3650
export BRAINOUT_KEYSTORE_PATH=/caminho/fora/do/repo/brainout-medicao.jks
export BRAINOUT_KEYSTORE_PASSWORD=...          # variáveis, nunca arquivo versionado
export BRAINOUT_KEY_ALIAS=brainout
export BRAINOUT_KEY_PASSWORD=...
./gradlew :app:assembleDevRelease

# Opção B — artefato de CI (mesma variante, build reprodutível pelo runner)
gh run download <run-id> -n <artifact>
```

Instalação:

```bash
adb install -r app/build/outputs/apk/dev/release/app-dev-release.apk
# ou, se estiver usando o artefato de CI:
# adb install -r <apk-baixado>.apk
```

**Nunca** use a variante `prod` para simular integração: a `BASE_URL` dela é um placeholder (`https://TBD/`), decisão E3.1.

## 3. Cenários fixos

| Cenário | Sequência |
|---|---|
| S1 — startup frio | `am force-stop` → `am start -W` → anotar `TotalTime` |
| S2 — navegação | Projetos → abrir um detalhe de projeto → voltar (3 ciclos) |
| S3 — lista longa | Projetos com as fixtures de volume (60 tarefas) → rolar até o fim |
| S4 — interação | abrir e fechar o diálogo de criação de projeto; abrir o menu de status de uma tarefa |

Comandos:

```bash
PKG=pucgo.joaopedrogmsilva.brainout

# S1 — startup frio (3x)
for i in 1 2 3; do
  adb shell am force-stop $PKG
  adb shell am start -W -S -n $PKG/.MainActivity | grep -E 'TotalTime|WaitTime'
done

# S2/S3/S4 — frames
adb shell dumpsys gfxinfo $PKG reset
# ... executar o cenário manualmente ...
adb shell dumpsys gfxinfo $PKG framestats > /tmp/framestats-<cenario>-<n>.txt
adb shell dumpsys gfxinfo $PKG | grep -E 'Total frames|Janky frames|50th|90th|95th|99th'
```

## 4. Métricas e referências de leitura

| Métrica | Onde sai | Referência |
|---|---|---|
| Tempo total de startup | `am start -W` (`TotalTime`) | comparar antes/depois no mesmo aparelho |
| Proporção de frames lentos | `gfxinfo` (`Janky frames`) | quanto menor, melhor; registrar o absoluto |
| p95 por frame | `framestats` / linha `95th percentile` | 16,7 ms a 60 Hz · 8,3 ms a 120 Hz |
| p50 / p90 / p99 | `gfxinfo` | contexto para o p95 |

Orçamentos por frame **não** são meta de projeto: são o limite físico da taxa de atualização. O que o plano exige (Q27) é **não regredir** sem análise, medida no mesmo cenário.

## 5. Registro (preencher na execução)

| Campo | Valor |
|---|---|
| SHA | _pendente_ |
| Variante / assinatura | `devRelease` / keystore local fora do repo ou artefato de CI |
| Aparelho / API / Hz | _pendente_ |
| Tema / fonte / idioma | _pendente_ |
| Execução 1 / 2 / 3 | _pendente_ |

Baseline numérico de F0: **não obtido**. Sem ele, NB-34 (desempenho comparado) não tem contra o que comparar — a primeira medição que existir passa a ser o baseline, desde que siga exatamente este roteiro.

## 6. Como destravar

```bash
export ANDROID_HOME=/home/joaopgms/Android/Sdk
$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_37.0 &   # emulador (não substitui os 2 aparelhos físicos de Q26)
$ANDROID_HOME/platform-tools/adb devices
```

Emulador serve para smoke e para o roteiro acima; o critério de aceite do plano (Q26) exige **dois aparelhos físicos**, preferencialmente um de 60 Hz e outro de 120 Hz, para press/sombra/IME/fonte 2.0/dark/tablet.

## 7. Limites honestos deste documento

- Nenhuma medição, nenhum aparelho, nenhum número — só procedimento.
- O roteiro não cobre memória, CPU, GPU nem tamanho de APK/AAB; se o redesign adotar uma biblioteca de animação, esses itens passam a valer e entram em NB-28/NB-34.
- Macrobenchmark não está previsto: a decisão Q27 só o admite se uma regressão for encontrada.
