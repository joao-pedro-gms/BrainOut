# QA — LOCAL-03 (backup local exportável e restauração segura)

Issue [#214](https://github.com/joao-pedro-gms/BrainOut/issues/214),
parte do épico [#211](https://github.com/joao-pedro-gms/BrainOut/issues/211).
Data: **20/10/2026**. Contrato do arquivo: [docs/BACKUP.md](../BACKUP.md).

---

## 1. Ambiente

| Item | Valor |
|---|---|
| Máquina | Windows 11, JDK 21 (Microsoft OpenJDK 21.0.12.1) |
| Emulador | `Medium_Phone_API_37.0` (Android 17 / API 37), renderização por software (`swiftshader_indirect`), sem áudio, sem snapshot |
| APK | `app-debug.apk` (`:app:assembleDebug`) |
| Idioma do aparelho | **en-US** — as capturas abaixo mostram a interface em **inglês** |
| Rede | O emulador está com **modo avião**; o app é totalmente local |

> O emulador não tem Play Services nem conta configurada. A
> verificação de pt/en de **string** é feita por lint
> (`MissingTranslation` como erro em `:feature:settings`); a
> verificação de pt/en de **interface** neste QA é parcial — ver
> [§5](#5-limites-desta-verificação).

---

## 2. O que foi verificado no aparelho

### 2.1 Exportação (caminho feliz)

1. **Configurações → BACKUP** mostra as duas linhas novas
   ("Export backup" / "Restore backup") e o texto que explica que o
   arquivo é cifrado e não é enviado para nenhum servidor.
   → [`config-backup-en.png`](local03/config-backup-en.png)
2. **Export backup** abre o diálogo **"Protect the backup"**, que
   explica que a senha escolhida será a senha das contas restauradas.
   → [`export-pwd-en.png`](local03/export-pwd-en.png)
3. **Senha curta (< 8)** é recusada com a mensagem *"The password must
   have at least 8 characters."*
   → [`export-pwd-curta-en.png`](local03/export-pwd-curta-en.png)
4. Com a senha válida, o **seletor de documentos do sistema** (Storage
   Access Framework) abre já com o nome sugerido
   `brainout-backup-2026-10-20.bnbak`.
   → [`saf-picker-en.png`](local03/saf-picker-en.png)
5. **SAVE** grava o arquivo e o app volta para Configurações com a
   mensagem *"Backup exported successfully."*
   → [`export-ok-en.png`](local03/export-ok-en.png)

**Arquivo gerado** (extraído do aparelho):

```
/sdcard/Download/brainout-backup-2026-10-20.bnbak   1 451 bytes
```

Conteúdo do envelope (texto claro):

```json
{"format":"brainout-backup","formatVersion":1,"createdAtEpochMillis":1792518631695,
 "kdf":{"algorithm":"pbkdf2_hmac_sha256","iterations":120000,"salt":"…"},
 "cipher":{"algorithm":"aes_256_gcm","iv":"…"},"payload":"…"}
```

**Verificação de sigilo:** o arquivo **não** contém o nome do projeto
(`teste`), nem nenhum e-mail, nem o hash de senha. A única ocorrência
de "pbkdf2" é o identificador do algoritmo no envelope.

### 2.2 Exportação — rastro de log

```
D BrainOut:BackupRepo: Backup exportado: 1 contas, 1 projetos, 1 tarefas, 1 etiquetas
D BrainOut:BackupVM: Backup exportado: 1 projetos, 1 tarefas
```

### 2.3 Restauração (caminho feliz)

1. **Restore backup** abre **"Replace local data?"** com o aviso de que
   a restauração apaga os dados locais e não pode ser desfeita.
   → [`restore-confirm-en.png`](local03/restore-confirm-en.png)
2. A confirmação abre o seletor de documentos; o arquivo `.bnbak`
   aparece na lista de Downloads.
3. A escolha do arquivo abre o diálogo **"Open the backup"**, que
   explica que a senha usada na exportação é necessária e que as
   contas que ainda não existirem no aparelho passam a usar essa
   senha.
   → [`restore-pwd-en.png`](local03/restore-pwd-en.png)
   → [`restore-pwd-preenchido-en.png`](local03/restore-pwd-preenchido-en.png)
4. A restauração conclui e o app **volta para a tela de login** — a
   sessão foi invalidada, como manda o contrato.
   → [`restore-ok-en.png`](local03/restore-ok-en.png)

**Log:**

```
D BrainOut:BackupVM: Backup restaurado: 1 projetos, 1 tarefas
```

### 2.4 Conteúdo restaurado (lido do banco depois da restauração)

```
users 1
  ('4590ac5b-…', 'joao pedro', 'joaopedroguimaraes@protonmail.com', 'OWNER', 'pbkdf2_sha256$120000…')
projects 1
  ('6cce8a5c-…', '4590ac5b-…', 'teste', 0)
tasks 1
  ('98decdc6-…', '6cce8a5c-…', 'oi', 'TODO', None)
tags 1
  ('197c4696-…', '4590ac5b-…', 'oi')
```

Confirmações:

- os **ids** foram preservados;
- o **vínculo** `projeto → dono` e `tarefa → projeto` continua
  correto;
- o hash da conta é o **local** (`pbkdf2_sha256$120000…`), e não o
  hash exportado — a conta já existia no aparelho, então o
  [contrato §5.2](../BACKUP.md#52-a-decisão) foi respeitado;
- a sessão foi encerrada: ao reabrir o app, ele voltou na **tela de
  abertura** (sem sessão), não na Home.

### 2.5 Senha errada (caminho de falha)

Restauração com a senha `senha-errada`:

- a tela mostra **"Wrong password or corrupted file."**
  → [`restore-senha-errada-en.png`](local03/restore-senha-errada-en.png)
- o **banco continua intacto**: as duas contas (a original e a conta de
  QA criada durante a verificação), o projeto e a tarefa seguem lá —
  a falha **não** alterou nenhum dado;
- a sessão **não** foi invalidada (a restauração nem chegou a
  escrever).

---

## 3. Verificação automatizada

Executada localmente em JDK 21, com o mesmo código do APK instalado:

| Verificação | Resultado |
|---|---|
| `testDebugUnitTest` (8 módulos) | **verde**, zero falhas/erros/skips |
| `:core:domain:koverVerify` | **verde** (limite de 60%) |
| `:core:data:koverVerify` | **verde** (limite de 60%) |
| `ktlintCheck` | **verde** |
| `detekt` | **verde** (0 issues) |
| `lintDebug` (8 módulos) | **verde** — inclusive `:core:data`, que voltou a ser rígido |
| `:app:assembleDebug` | **verde** |
| `:core:data:assembleDebugAndroidTest` | **verde** (compila) |
| `:feature:auth:assembleDebugAndroidTest` | **verde** (compila) |

Testes novos desta entrega:

| Teste | O que fixa |
|---|---|
| `core/domain/.../usecase/ExportBackupUseCaseTest.kt` | Resumo devolvido, senha repassada, senha curta recusada antes do repositório. |
| `core/domain/.../usecase/RestoreBackupUseCaseTest.kt` | Lembrete cancelado para tarefa que sumiu, reagendado com 1 h de antecedência, nada reagendado sem prazo, falha não mexe nos lembretes. |
| `core/data/.../backup/BackupCryptoTest.kt` | PBKDF2 manual igual ao vetor conhecido e à implementação da JDK; ida e volta da cifragem; senha errada e conteúdo adulterado falham com integridade. |
| `core/data/.../backup/BackupRepositoryImplTest.kt` | **Instalação distinta**: exporta de um banco e restaura em outro (pepper diferente), preservando ids e vínculos; arquivo inválido, truncado, de versão desconhecida, acima do limite e com referência órfã **não alteram** os dados; sessão invalidada; preferências reaplicadas; lembretes gerados; hash de senha **ausente** do arquivo. |
| `feature/settings/.../viewmodel/BackupViewModelTest.kt` | Cada falha vira o tipo de erro certo; sucesso publica resumo; `dismissMessage` limpa. |
| `feature/settings/.../ui/BackupSectionTest.kt` | Linhas renderizadas, diálogo de senha, validação (curta e divergente), confirmação de substituição, cancelamento e exibição do erro. |

---

## 4. Efeito colateral verificado

`coreLibraryDesugaring` passou a ser habilitado em `:core:data`
(issue #201 / DEF-63) porque o módulo usa `java.time` e
`java.util.Base64` com `minSdk 24`. Efeito **verificado**:

- `lintDebug` de `:core:data` saiu de **11 erros [NewApi]** para
  **0** — o gate do módulo pôde voltar a `abortOnError = true`;
- o mesmo mecanismo foi habilitado nos quatro módulos `feature/*`
  (o metadado do AAR do AGP exige consistência entre quem consome
  `:core:data` e quem faz o desugaring);
- `:app:assembleDebug` e as duas suítes instrumentadas continuam
  compilando.

---

## 5. Limites desta verificação

- **Não executado em aparelho físico** — tudo aqui é emulador
  Android 17 (API 37). Comportamento de SAF, teclado e layout podem
  diferir em fabricante.
- **TalkBack não foi exercitado** — não há verificação de leitura de
  tela nem de foco no diálogo de senha. O campo de senha do diálogo
  **não** tem `contentDescription` própria (só o botão de exibir/
  esconder tem), o que é um ponto a revisar para acessibilidade.
- **Interface em en-US** — as capturas mostram a versão em inglês. A
  paridade pt/en é garantida pelo lint (`MissingTranslation` como
  erro), mas **não** foi conferida visualmente em português.
- **A senha de uma conta já existente** não foi testada no aparelho
  (o contrato é coberto pelo teste automatizado
  `conta que ja existe no aparelho mantem o hash local`).
- **Sem restauração entre dois aparelhos diferentes** — a
  "instalação distinta" do contrato foi provada em teste automatizado
  (dois bancos Room, pepper diferente). No aparelho, a restauração
  ocorreu no mesmo aparelho de onde o arquivo veio.
- **Sem restauração de um arquivo de outra versão do formato** — o
  caminho é coberto por teste automatizado
  (`versao desconhecida falha sem alterar os dados`).
- **Sem verificação de backup do Android** — o desligamento de
  `allowBackup`/`dataExtractionRules` é verificado por inspeção do
  manifesto, não por comportamento do sistema.

---

## 6. Capturas

| Arquivo | Conteúdo |
|---|---|
| [`local03/config-backup-en.png`](local03/config-backup-en.png) | Seção de backup em Configurações |
| [`local03/export-pwd-en.png`](local03/export-pwd-en.png) | Diálogo de senha da exportação |
| [`local03/export-pwd-curta-en.png`](local03/export-pwd-curta-en.png) | Senha curta recusada |
| [`local03/saf-picker-en.png`](local03/saf-picker-en.png) | Seletor de documentos do sistema |
| [`local03/export-ok-en.png`](local03/export-ok-en.png) | Exportação concluída |
| [`local03/restore-confirm-en.png`](local03/restore-confirm-en.png) | Confirmação de substituição |
| [`local03/restore-pwd-en.png`](local03/restore-pwd-en.png) | Diálogo de senha da restauração |
| [`local03/restore-pwd-preenchido-en.png`](local03/restore-pwd-preenchido-en.png) | Senha preenchida |
| [`local03/restore-ok-en.png`](local03/restore-ok-en.png) | Restauração concluída, de volta ao login |
| [`local03/restore-senha-errada-en.png`](local03/restore-senha-errada-en.png) | Senha errada — dados intactos |

Refs: R5, R11, E5.1
