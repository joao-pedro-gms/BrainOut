<!-- João Pedro G M Silva - PUC Goiás ADS - 20251012000740 -->
# core/data — AGENTS

## Escopo local

Persistência Room, preferências DataStore, sessão, segurança e calendário
nacional local. Não há Retrofit, HTTP, conectividade ou fila de sync. Não
reintroduzir contratos remotos descritos nos documentos históricos.

## Estrutura

- `local/BrainOutDatabase.kt`: Room v5, com users/projects/tasks/tags/project_tags.
- `local/Migrations.kt`: migrations manuais 1→2→3→4→5. A 4→5 remove pending_ops e normaliza o default legado de completed_at,
  preservando todos os registros. A 2→3 agora declara DEFAULT NULL conforme o schema.
- `local/LocalTransaction.kt`: contrato LocalTransaction e RoomLocalTransaction.
- `local/dao/` e `local/entity/`: consultas, conversores e cascatas.
- `repository/`: implementações locais de User/Project/Task/Tag/Holiday.
- `preferences/`: preferências de listagem por usuário e tema global.
- `session/`: ActiveUserProvider e SessionStore.
- `security/`: PasswordHasherImpl e PepperProvider, com módulo Hilt próprio.
- `di/DataModule.kt`: database, DAOs, transação e bindings das portas de domínio.
- `schemas/`: schemas Room históricos 1–5, versionados e preservados.

## Convenções

- Toda escrita de repositório que precisa ser **atômica com outra** usa
  LocalTransaction.run; cascatas dos DAOs continuam @Transaction. Uma falha
  de validação desfaz toda a escrita. Uma escrita isolada
  (`UserRepositoryImpl.save`, `TaskRepositoryImpl.repairCompletedProject`)
  já é atômica no Room e não é envolvida.
- Nunca usar fallbackToDestructiveMigration. Acrescentar Migration manual,
  schema exportado e teste para cada mudança de versão.
- DAOs suspend/Flow já isolam o I/O do Room; não adicionar Dispatchers.IO. A exceção é o KDF de senha (`PasswordHasherImpl`): PBKDF2-HMAC-SHA256 com 120 000 iterações é trabalho de CPU (não I/O de banco) e trava a interface se rodar no dispatcher principal — por isso o `withContext(Dispatchers.IO)` dentro do KDF.
- Domínio continua Kotlin JVM: sem Room, Hilt, Android ou implementação de portas.
- DI principal em DataModule; PepperProvider.Factory continua sendo módulo
  dedicado ao pepper. Não expor implementações para os módulos de feature.
- Header do autor em todo .kt; pacotes minúsculos.
- PBKDF2.verify lê as iterações do hash e valida a faixa antes de derivar.
  O pepper depende do Keystore; copiar o banco não garante backup portátil.
- SortOrder.toStorageKey em snake_case; manter leitura da grafia legada.
- HolidayRepositoryImpl calcula feriados nacionais por regras atuais no
  aparelho, sem pontos facultativos ou feriados municipais/estaduais.

## Testes e comandos

```bash
./gradlew :core:data:testDebugUnitTest :core:data:koverVerify
./gradlew :core:data:assembleDebugAndroidTest
./gradlew :core:data:connectedDebugAndroidTest
```

Sem flavors: não usar tasks DevDebug nem missingDimensionStrategy. A suíte
JVM usa Robolectric/Room real e Fake*Dao nos testes unitários de repositório,
sem MockK de DAOs. LocalUpgradeTest abre v4 pelo Room v5 e verifica os dados;
LocalPersistenceTest prova rollback e cascatas em transação real. O runner está declarado explicitamente, e os schemas são assets de androidTest
via androidComponents. A CI verifica ambos no APK de testes para evitar execução
vazia. O teste instrumentado MigrationTest cobre as migrations históricas e a 4→5; executar
connectedDebugAndroidTest requer emulador/dispositivo.

Kover exige 60% de linhas. Não enfraquecer exclusões para fazer o gate passar.
A dívida conhecida de lint NewApi de java.time/Base64 com minSdk24 permanece
separada (#201): abortOnError=false não é evidência de ausência de problemas.
