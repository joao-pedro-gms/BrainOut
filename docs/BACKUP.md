# BrainOut — contrato do backup local exportável

Decisão e implementação da issue **#214 (LOCAL-03)**, parte do épico
[#211](https://github.com/joao-pedro-gms/BrainOut/issues/211). O
produto alvo é um aplicativo Android **totalmente local**: o backup
manual é o único caminho de transferência de dados — não há servidor,
sincronização nem API externa.

Este documento é o **contrato** do arquivo de backup: ele define o
formato, o que entra e o que não entra, a política de contas, os
limites e as garantias de falha. A implementação segue o contrato;
mudá-lo exige mudar este arquivo junto.

---

## 1. Resumo para o usuário

1. **Configurações → Backup → Exportar backup.**
2. O app pede uma **senha de backup** (mínimo de 8 caracteres).
3. O seletor de arquivos do Android abre; o usuário escolhe onde
   gravar. O app **não** recebe acesso amplo ao armazenamento.
4. O arquivo gerado é **cifrado** (AES-256-GCM) e **não** sai do
   aparelho por conta própria.

Para restaurar: **Configurações → Backup → Restaurar backup** →
confirmação de substituição → escolha do arquivo → a mesma senha.

> **A senha do backup é a senha das contas restauradas.** Ver
> [§5](#5-política-de-contas-e-senhas).

---

## 2. Formato do arquivo

Arquivo **JSON em UTF-8**, com os metadados em texto claro e o
conteúdo **cifrado** em Base64. A separação é deliberada: a validação
de formato e de versão acontece **antes** de qualquer tentativa de
decifrar, o que permite responder com precisão "isto não é um backup
do BrainOut" ou "este backup é de uma versão futura" — em vez de um
genérico "senha incorreta".

```json
{
  "format": "brainout-backup",
  "formatVersion": 1,
  "createdAtEpochMillis": 1760000000000,
  "kdf":    { "algorithm": "pbkdf2_hmac_sha256", "iterations": 120000, "salt": "<base64>" },
  "cipher": { "algorithm": "aes_256_gcm", "iv": "<base64>" },
  "payload": "<base64 do texto cifrado + tag do GCM>"
}
```

### 2.1 Envelope (metadados)

| Campo | Tipo | Significado |
|---|---|---|
| `format` | texto | Sempre `brainout-backup`. Outro valor → o arquivo **não** é um backup do app. |
| `formatVersion` | inteiro | Versão do formato. Só a **1** é lida hoje; qualquer outra é recusada com "versão desconhecida". |
| `createdAtEpochMillis` | inteiro | Momento da exportação (epoch millis, UTC). |
| `kdf.algorithm` | texto | `pbkdf2_hmac_sha256`. |
| `kdf.iterations` | inteiro | Custo da derivação da chave. |
| `kdf.salt` | base64 | Sal aleatório de 16 bytes, por arquivo. |
| `cipher.algorithm` | texto | `aes_256_gcm`. |
| `cipher.iv` | base64 | Vetor de inicialização aleatório de 12 bytes, por arquivo. |
| `payload` | base64 | Conteúdo cifrado **com** a tag de autenticação do GCM. |

### 2.2 Payload (conteúdo, após decifrar)

```json
{
  "users":       [ { "id", "name", "email", "role", "createdAtEpochMillis" } ],
  "projects":    [ { "id", "ownerId", "name", "description", "createdAtEpochMillis", "isCompleted" } ],
  "tasks":       [ { "id", "projectId", "title", "priorityCode", "status",
                     "assigneeId", "dueDateEpochMillis", "createdAtEpochMillis",
                     "completedAtEpochMillis" } ],
  "tags":        [ { "id", "ownerId", "name", "color", "createdAtEpochMillis" } ],
  "projectTags": [ { "projectId", "tagId" } ],
  "preferences": { "themeMode": "system", "sortOrderByUser": { "<userId>": "created_desc" } }
}
```

Campos opcionais (`description`, `assigneeId`, `dueDateEpochMillis`,
`completedAtEpochMillis`) são **omitidos** quando nulos.

### 2.3 O que **não** entra no arquivo

| Item | Por quê |
|---|---|
| Hash de senha | Derivado com o pepper do aparelho — não é portável e não deve circular ([§5](#5-política-de-contas-e-senhas)). |
| Pepper / chave do Android Keystore | Material criptográfico **por instalação**. Nunca sai do aparelho. |
| Sessão ativa (`user_id`) | Sessão é estado local, não dado. |
| Preferência "permissão de notificação já pedida" | Estado de interface do aparelho, não do usuário. |
| Texto de busca da listagem | Estado transitório de interface. |

---

## 3. Limites e garantias de falha

| Garantia | Como é garantida |
|---|---|
| **Limite de tamanho: 20 MiB** | A leitura da importação para ao ultrapassar o limite (`BackupFormat.MAX_BACKUP_BYTES`). O arquivo não é carregado inteiro na memória. |
| **Formato inválido** | JSON malformado, `format` diferente ou envelope incompleto → `BackupFormatException`, **antes** de qualquer escrita. |
| **Versão desconhecida** | `formatVersion` diferente de 1 → `UnsupportedBackupVersionException`, antes de qualquer escrita. |
| **Senha errada / arquivo adulterado** | A tag de autenticação do AES-GCM não valida → `BackupIntegrityException`. O AES-GCM não distingue "senha errada" de "bytes corrompidos"; a mensagem é a mesma de propósito. |
| **Arquivo truncado** | Falha no JSON ou na tag do GCM → mesmo caminho de falha, sem tocar nos dados. |
| **Backup inconsistente** | Projeto sem dono, tarefa sem projeto, etiqueta sem dono ou associação órfã → `BackupRestoreException`, **antes** da transação. |
| **Falha na gravação** | Violação de restrição do banco → `BackupRestoreException`; a transação é revertida. |

Em **todos** esses casos, o conteúdo já existente no aparelho fica
**intacto**: a validação acontece antes de a transação abrir, e a
transação é atômica.

### 3.1 Atomicidade

A restauração grava **tudo** em uma única transação Room
(`RoomDatabase.withTransaction`): apaga o conteúdo anterior e grava o
novo, ou não grava nada. Não existe estado intermediário em que o
aparelho tenha metade do backup.

### 3.2 Limite de versão

`formatVersion` é a fronteira de compatibilidade. Quando o formato
evoluir, o número sobe e as versões antigas continuam legíveis: o
aplicativo recusa o que não sabe ler, em vez de tentar adivinhar.

---

## 4. Preferências e vínculos

- **Preservados:** ids de contas, projetos, tarefas e etiquetas;
  vínculos `projeto → dono`, `tarefa → projeto`, `etiqueta → dono` e
  as associações N:N `projeto ↔ etiqueta`; o tema do aplicativo; a
  ordenação da listagem de cada conta.
- **Reaplicados na restauração:** o modo de tema e a ordenação de
  cada conta restaurada.
- **Rejeitados:** backup com referência órfã é recusado antes de
  escrever.

---

## 5. Política de contas e senhas

Esta é a decisão que a issue [#214](https://github.com/joao-pedro-gms/BrainOut/issues/214)
pede explicitamente ("decidir o tratamento de contas locais e senhas").

### 5.1 O problema

A senha de uma conta é derivada por PBKDF2-HMAC-SHA256 a partir de
`senha + pepper`, em que o **pepper** é uma chave aleatória de 16
bytes gerada **por instalação** e guardada em
`EncryptedSharedPreferences` (chave AES-256 do Android Keystore).
Copiar o `password_hash` para outro aparelho não basta para
autenticar: o hash foi calculado com um pepper que o outro aparelho
não tem.

Três alternativas foram considered e **rejeitadas**:

| Alternativa | Por que foi rejeitada |
|---|---|
| Exportar o pepper / a chave do Keystore | Exportaria material criptográfico do aparelho. Inaceitável. |
| Exportar a senha em texto puro | expõe a credencial no arquivo. Inaceitável. |
| Restaurar a conta "travada" e pedir uma senha nova em um fluxo à parte | Criaria um estado intermediário (conta sem credencial), uma tela nova no módulo de autenticação e umavu leaks de existência de conta. |

### 5.2 A decisão

**O backup não carrega material de senha. A senha do backup é a
senha das contas restauradas.**

- Na **exportação**, a conta entra no arquivo com id, nome, e-mail,
  papel e data — **sem** hash.
- Na **restauração**:
  - a conta que **já existe** no aparelho de destino **mantém o hash
    local** (ele foi derivado com o pepper local, e continua válido);
  - a conta que **não existe** no aparelho de destino recebe a
    **senha do backup**, derivada com o pepper **local** — o pepper
    do outro aparelho nunca é necessário.
- A **sessão ativa é invalidada** depois da restauração: o usuário
  entra de novo.

Consequência prática: o usuário escolhe uma senha ao exportar e a
usa, no aparelho novo, para entrar nas contas restauradas. Se a
conta já existia no aparelho de destino, a senha local antiga
continua valendo.

### 5.3 Limitações conhecidas

- Todas as contas restauradas **novas** compartilham a mesma senha
  (a do backup). O aplicativo ainda não tem "alterar senha" por
  conta — é trabalho futuro.
- **Esquecer a senha do backup torna o arquivo irrecuperável.** É
  consequência direta de cifrar o conteúdo: o GCM não tem como
  validar uma senha errada sem o material correto.

---

## 6. Lembretes de prazo

Depois de uma restauração, os lembretes são **reconciliados**
(`RestoreBackupUseCase`):

1. Os ids das tarefas existentes **antes** da restauração são
   capturados.
2. As tarefas que **sumiram** têm o lembrete **cancelado** — senão o
   aparelho dispararia a notificação de uma tarefa que não existe
   mais.
3. As tarefas restauradas **com prazo** têm o lembrete **reagendado**
   com a antecedência de 1 hora (`DeadlineNotificationScheduler.REMINDER_LEAD`).
   Reagendar (em vez de apenas agendar as novas) é o que impede
   **notificações duplicadas** e corrige o horário quando o prazo
   mudou entre a exportação e a restauração.

O agendamento usa WorkManager com política de substituição
(`ExistingWorkPolicy.REPLACE`), então há **um** trabalho pendente por
tarefa.

---

## 7. Backup automático do Android (issue #175 / DEF-37)

O backup automático do Android (nuvem do Google e transferência entre
aparelhos) está **desligado**:

- `android:allowBackup="false"`;
- `android:fullBackupContent="false"` (Android 11 e anteriores);
- `android:dataExtractionRules="@xml/data_extraction_rules"`
  (Android 12+), que exclui `root`, `file`, `database`, `sharedpref`
  e `external`.

Motivo: o `brainout.db` guarda o hash das senhas e o
`auth_prefs.preferences_pb` guarda o id do usuário autenticado. O
hash não funciona em outro aparelho (pepper por instalação) e a
sessão não deve sair do dispositivo. **Backup manual não resolve
automaticamente `allowBackup`/`passwordHash`/sessão** — por isso o
desligamento é explícito, e não uma consequência acidental do
backup existir.

---

## 8. Onde o código está

| Camada | Arquivo | Papel |
|---|---|---|
| Domínio | `core/domain/.../repository/BackupRepository.kt` | Porta do backup + resultado da restauração. |
| Domínio | `core/domain/.../usecase/ExportBackupUseCase.kt` | Exporta; valida o mínimo da senha. |
| Domínio | `core/domain/.../usecase/RestoreBackupUseCase.kt` | Restaura e reconcilia os lembretes. |
| Domínio | `core/domain/.../error/BackupException.kt` | Falhas do fluxo (formato, versão, integridade, limite, gravação). |
| Dados | `core/data/.../backup/BackupFormat.kt` | Envelope: versão, metadados, decodificação. |
| Dados | `core/data/.../backup/BackupPayload.kt` | Conteúdo lógico e codec JSON. |
| Dados | `core/data/.../backup/BackupCrypto.kt` | PBKDF2 (implementação própria, API 24/25) + AES-256-GCM. |
| Dados | `core/data/.../backup/BackupRepositoryImpl.kt` | Exporta, valida e restaura em transação única. |
| Dados | `core/data/.../local/dao/BackupDao.kt` | Operações em massa (ler tudo, gravar em massa, apagar tudo). |
| Interface | `feature/settings/.../ui/BackupSection.kt` | Seção de Configurações, seletor do sistema e diálogos. |
| Interface | `feature/settings/.../viewmodel/BackupViewModel.kt` | Executa a operação e classifica o resultado para pt/en. |

### 8.1 Por que o PBKDF2 é implementado à mão

`SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")` só existe a
partir do **API 26**, e o `minSdk` do projeto é **24** — chamá-lo
diretamente quebraria o backup em API 24/25. A implementação em
`BackupCrypto` usa apenas `javax.crypto.Mac` ("HmacSHA256",
disponível desde o API 1) e segue a RFC 8018. A equivalência com a
implementação da JDK é fixada por teste de vetor conhecido
(`BackupCryptoTest`).

### 8.2 Onde a operação de I/O acontece

Abrir, ler e gravar o arquivo é I/O **bloqueante**. A camada de
dados não troca o dispatcher: o `BackupViewModel` (em
`:feature:settings`) executa a chamada em `Dispatchers.IO`. O acesso
ao Room continua `suspend` e roda no dispatcher interno do Room.

---

## 9. Limites conhecidos

- **Sem "alterar senha" por conta**: as contas restauradas novas
  compartilham a senha do backup ([§5.3](#53-limitações-conhecidas)).
- **Sem backup incremental**: o arquivo sempre contém o estado
  inteiro.
- **O texto de busca da listagem não é preservado** — é estado
  transitório de interface.
- **A senha do backup é a credencial do arquivo**: um arquivo com
  senha fraca é quebrável por força bruta offline. Por isso a senha
  mínima é de 8 caracteres e a interface diz que a senha será a das
  contas restauradas.

---

## 10. Evidências

- Testes JVM: `core/domain/src/test/.../usecase/ExportBackupUseCaseTest.kt`,
  `core/domain/src/test/.../usecase/RestoreBackupUseCaseTest.kt`,
  `core/data/src/test/.../backup/BackupCryptoTest.kt`,
  `core/data/src/test/.../backup/BackupRepositoryImplTest.kt`,
  `feature/settings/src/test/.../viewmodel/BackupViewModelTest.kt`,
  `feature/settings/src/test/.../ui/BackupSectionTest.kt`.
- QA manual: [qa/LOCAL-03.md](qa/LOCAL-03.md).
- Issue: [#214](https://github.com/joao-pedro-gms/BrainOut/issues/214).

Refs: R5, R11, E5.1
