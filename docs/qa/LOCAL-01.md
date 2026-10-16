# LOCAL-01 — retirada de rede e validação do upgrade (#212)

Base: `92653a2`. Ambiente local: Windows, JDK 21, SDK/AVD Android 37.

## Mudança e garantias

- Room v5 mantém usuários, projetos, tarefas, tags e associações; retira a
  fila `pending_ops`. `LocalTransaction` mantém escritas atômicas e rollback.
- A antiga migration 2→3 omitia o `DEFAULT NULL` de `completed_at`. A 2→3 foi
  alinhada ao schema e a 4→5 repara bases que já passaram pela versão antiga,
  copiando todos os campos e recriando os índices, na transação da migration.
- O início do app cancela somente os nomes/tags históricos de sync.
- HTTP, stub, conectividade e flavors remotos foram retirados. WorkManager
  permanece para lembretes. O applicationId e o nome do banco são preservados.

## Evidência local

Passaram ktlint, detekt, **522 testes JVM** nos oito módulos (0 falhas/erros/
ignorados), Kover >=60% em domínio e dados, lint de todos os módulos, build
do APK debug e compilação release. Os cinco testes Python do release e os
verificadores de tokens/roadmap também passaram.

- `LocalUpgradeTest`: três casos JVM, incluindo a cadeia 1→5, v4 canônica e
  v4 herdada da antiga 2→3, com verificação de credenciais, prazos, conclusão,
  tags, associações e integridade referencial.
- `LocalPersistenceTest`: rollback real ao falhar uma tag, CRUD e cascatas de
  conclusão/reabertura/exclusão contra Room.
- `LegacySyncCleanupTest`: cancelamento idempotente do legado, preservando
  um trabalho de lembrete.
- Instrumentação em `Medium_Phone_API_37.0`, com `airplane_mode_on=1`:
  **7 testes, 0 falhas, 0 erros, 0 ignorados**. Inclui as migrations históricas,
  4→5 canônica, reparo do default legado e CRUD de usuários.
- APK instalado e MainActivity iniciada no mesmo emulador em modo avião;
  processo permaneceu ativo e não houve crash registrado para seu PID.
- Manifesto final e `aapt2 dump permissions` do APK: sem INTERNET ou
  ACCESS_NETWORK_STATE; POST_NOTIFICATIONS preservada, cleartext desativado.
- Suítes instrumentadas de dados e auth compiladas. A execução instrumentada
  de auth não faz parte desta validação.

## Falso verde encontrado durante a execução

A primeira instrumentação reportou sucesso com **zero testes**: faltava a
classe AndroidJUnitRunner no APK. Após declarar o runner, cinco migrations
falharam pela ausência dos assets; incluindo os schemas, duas falharam pela
diferença de default da 2→3. Nenhum desses resultados foi aceito como gate verde.

Os APKs agora incluem explicitamente o runner e os cinco schemas exportados.
A CI verifica ambos com `verify_data_test_apk.py` e verifica o manifesto final
com `verify_local_manifest.py`. O job local também compila as suítes
instrumentadas, sem alegar que executa um emulador na CI.

## Limites e sequência

O emulador foi iniciado sem janela/snapshot, em modo de leitura, e encerrado
após a validação. Não houve QA em aparelho físico, TalkBack ou teclado.
Os textos remanescentes de equipe/online são acabamento da #213. Backup manual
é #214; backup automático Android é #175. O calendário nacional é local e segue
as regras atuais; não cobre pontos facultativos ou feriados regionais.
