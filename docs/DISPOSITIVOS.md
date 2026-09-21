# João Pedro G M Silva - PUC Goiás ADS - 20251012000740
# docs/DISPOSITIVOS.md — Mapeamento de dispositivos físicos (E5.3)
#
# Este documento registra as características dos dispositivos físicos usados
# nos testes de E5.3 ("APK release assinado e testado em pelo menos 2
# dispositivos físicos diferentes"). O dispositivo primário já é conhecido;
# o secundário será emprestado e ainda não foi definido — os campos marcados
# com ⏳ devem ser preenchidos quando o aparelho chegar.
#
# Convenção: `[PREENCHIDO]` = confirmado no aparelho; `⏳` = aguardando o
# dispositivo físico.

# 1. Dispositivo primário

| Campo                          | Valor                                        |
|--------------------------------|----------------------------------------------|
| Modelo                         | Samsung Galaxy S20 FE 5G                     |
| Fabricante / ano               | Samsung / 2020                               |
| Versão do Android              | 13 (One UI 5.1)                              |
| API level                      | 33                                           |
| Tela / densidade               | 6,5" 1080×2340, 420 dpi                      |
| Estado da bateria / armazenamento livre | [PREENCHER antes dos testes de E5.3] ⏳ |
| POST_NOTIFICATIONS (Android 13+) | [PREENCHER: conceder em Configurações → Aplicativos → BrainOut → Notificações] ⏳ |
| Modo avião acessível           | [PREENCHER: atalho nas configurações rápidas] ⏳ |
| TalkBack / Accessibility Scanner | [PREENCHER: instalar via Play Store se ausentes] ⏳ |
| Opções do desenvolvedor habilitadas | [PREENCHER: 7 toques em "Número da versão"] ⏳ |
| Depuração USB ativa            | [PREENCHER: ativar em Opções do desenvolvedor] ⏳ |

Observações do primário: [PREENCHER ⏳ — ex.: conta Google usada, versão do One UI exata, comportamento de economia de bateria]

# 2. Dispositivo secundário (emprestado — modelo ainda indefinido)

## 2.1 Ficha do aparelho

| Campo                            | Valor |
|----------------------------------|-------|
| Modelo                           | ⏳    |
| Fabricante                       | ⏳    |
| Versão do Android                | ⏳    |
| API level                        | ⏳    |
| Tela / densidade                 | ⏳    |
| POST_NOTIFICATIONS (Android 13+) | ⏳ (se Android 13+, conceder permissão na 1ª execução do app; se Android 12-, o app herda o comportamento padrão e não há runtime permission) |
| Modo avião (teste offline)       | ⏳    |
| TalkBack / Accessibility Scanner | ⏳    |
| Opções do desenvolvedor habilitadas | ⏳ |
| Depuração USB ativa              | ⏳    |
| Conta Google / Play Store disponível | ⏳ |

## 2.2 Como descobrir os campos quando o aparelho chegar

Copie e cole no terminal (com o aparelho conectado por USB e a depuração USB autorizada):

```bash
adb devices -l                      # modelo: campo "device:", ex.: model:SM_G780F
adb shell getprop ro.build.version.release  # versão do Android, ex.: 13
adb shell getprop ro.build.version.sdk      # API level, ex.: 33
adb shell wm size && adb shell wm density   # resolução e densidade
```

# 3. Checklist de compatibilidade do 2º dispositivo

O app BrainOut compila com `compileSdk = 35`, `minSdk = 24` e
`targetSdk = 35` (ver `app/build.gradle.kts`, `defaultConfig`).

| Item de verificação                                              | Como conferir / comando | Resultado |
|------------------------------------------------------------------|-------------------------|-----------|
| 3.1 API level do aparelho ≥ `minSdk` (24)                        | `adb shell getprop ro.build.version.sdk` → ≥ 24 | ⏳ |
| 3.2 API level do aparelho ≥ 26 (canais de notificação, exigidos a partir do Android 8.0) | `adb shell getprop ro.build.version.sdk` → ≥ 26 | ⏳ |
| 3.3 `targetSdk` do app (35) instalável no aparelho                | Instalação bem-sucedida no passo 5.2 | ⏳ |
| 3.4 Permissão de notificações concedida (Android 13+, API 33+)    | Diálogo na 1ª execução ou Configurações → Aplicativos → BrainOut → Notificações | ⏳ |
| 3.5 Espaço livre suficiente para o APK/AAB (≥ 200 MB recomendado) | Configurações → Armazenamento | ⏳ |
| 3.6 Instalação via adb (release assinado)                        | Passo 5.2 deste documento | ⏳ |
| 3.7 Login e sincronização contra o backend no 2º dispositivo      | Passo 5.3 deste documento | ⏳ |
| 3.8 Modo avião: app continua utilizável offline                  | Passo 5.4 deste documento | ⏳ |

# 4. Instalação do release assinado

O artefato de release é produzido pelo workflow `Release APK/AAB`
(`.github/workflows/release-apk.yml`), disparado por push de tag `v*`.
Baixar o `.aab` do run correspondente em
<https://github.com/joao-pedro-gms/BrainOut/actions/workflows/release-apk.yml>.

Para instalar no dispositivo a partir do `.aab`, gerar APKs universais
localmente (Bundletool), ou instalar direto de um APK release se
disponível:

```bash
# .aab → APKs (requer bundletool: https://github.com/google/bundletool)
java -jar bundletool.jar build-apks \
  --bundle=app-release.aab \
  --output=brainout.apks \
  --mode=universal

# instalar no dispositivo conectado (primário ou secundário)
java -jar bundletool.jar install-apks --apks=brainout.apks

# conferir o pacote instalado
adb shell pm list packages | grep joaopedrogmsilva
```

Alternativa direta com um APK (se houver APK de release no artifact):

```bash
adb install -r app-release.apk
```

# 5. Procedimento de teste de E5.3 (instalação do release assinado em ambos)

Repetir os passos 5.1–5.5 uma vez por dispositivo. Registrar o resultado
de cada passo nas colunas "Primário" e "Secundário".

| # | Passo                                                                                     | Primário (S20 FE) | Secundário |
|---|-------------------------------------------------------------------------------------------|-------------------|------------|
| 5.1 | Habilitar Opções do desenvolvedor e depuração USB; conectar por USB; `adb devices` mostra o aparelho | ⏳ | ⏳ |
| 5.2 | Instalar o release assinado: `java -jar bundletool.jar install-apks --apks=brainout.apks` (ou `adb install -r app-release.apk`) | ⏳ | ⏳ |
| 5.3 | Abrir o app; conceder POST_NOTIFICATIONS (Android 13+); fazer login com as credenciais de teste; verificar que o login é aceito e os dados sincronizam contra o backend | ⏳ | ⏳ |
| 5.4 | Ativar o modo avião; verificar que o app continua utilizável e exibe o estado offline; desativar o modo avião e verificar que a sincronização retoma | ⏳ | ⏳ |
| 5.5 | Rodar Accessibility Scanner na tela inicial e na navegação principal; salvar relatório; registrar observações | ⏳ | ⏳ |

Resultado esperado: ambos os aparelhos instalam e executam o mesmo
`.aab` assinado; login e sync funcionam nos dois; modo avião e
acessibilidade verificados. Marcar cada linha com ✔/✘ e uma observação
curta. Qualquer ✘ deve gerar uma issue no repositório antes do
encerramento do Ciclo 4.

---

Assinatura: João Pedro G M Silva - PUC Goiás ADS - 20251012000740