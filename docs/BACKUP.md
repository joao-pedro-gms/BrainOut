# BrainOut — contrato do backup pessoal local

O backup é manual, cifrado e inteiramente local. Configurações → Backup usa o Storage Access Framework para escolher um arquivo `.bnbak`. Não há rede nem sincronização. A política de contas foi aprovada em 26/10/2026 no [ADR 0002](adr/0002-contas-locais-independentes.md).

## 1. Jornada

Exportar pede uma senha de arquivo com pelo menos 8 caracteres e grava somente os dados da conta conectada. Restaurar pede confirmação de substituição, arquivo e senha; após decifrar, mostra a conta do arquivo para confirmação. Arquivos antigos com várias contas permitem selecionar uma delas.

A restauração substitui somente projetos, tarefas, etiquetas e ordenação da conta conectada. O nome de usuário, a senha, a sessão e as demais contas continuam intactos. Para importar em outra instalação, primeiro cadastre uma conta e entre nela. A senha do backup não se torna a senha de login.

## 2. Formato versionado

O envelope JSON mantém `format: brainout-backup`, `formatVersion`, `createdAtEpochMillis`, `kdf`, `cipher` e `payload`. Novas exportações usam **formatVersion 2**; a leitura aceita 1 e 2 e rejeita versões desconhecidas antes de gravar.

A cifra continua AES-256-GCM, com chave derivada por PBKDF2-HMAC-SHA256, 120000 iterações, sal aleatório de 16 bytes e IV de 12 bytes. O conteúdo cifrado inclui a tag de autenticação. O limite do arquivo permanece 20 MiB.

O payload v2 contém:

- `users`: exatamente uma identidade descritiva com `id`, `username`, `createdAtEpochMillis`.
- `projects`, `tasks`, `tags`, `projectTags`: somente registros da conta exportada e suas associações.
- `preferences.sortOrderByUser`: ordenação da conta exportada.

Campos opcionais nulos são omitidos. Não entram hashes, pepper, chaves do Keystore, sessão, tema, permissão de notificações ou dados de outras contas.

## 3. Validação e atomicidade

JSON, versão, tamanho, integridade, IDs duplicados, invariantes dos modelos e relações são validados antes da substituição. Associações entre donos diferentes são recusadas. Senha incorreta e arquivo adulterado compartilham o erro de integridade do AES-GCM.

Os dados são substituídos numa única `LocalTransaction`: falha de escrita reverte a substituição no Room. A ordenação reside no DataStore, que não compartilha transação com Room; falhas nessa preferência são uma limitação entre os dois armazenamentos. A sessão capturada é revalidada e sua troca fica bloqueada durante a escrita.

## 4. Compatibilidade v1

O leitor reconhece as antigas identidades `id`, `name`, `email`, `role`, `createdAtEpochMillis`. A seleção mostra nome e e-mail para distinguir homônimos. Somente a conta escolhida é importada; o papel antigo não concede permissões e o tema global do arquivo é ignorado.

## 5. Política de contas e senhas

A conta conectada é sempre o destino. A restauração nunca cria ou remove contas, altera nomes de usuário ou deriva uma senha de login a partir da senha do arquivo. Todos os projetos, tarefas e etiquetas importados recebem novos UUIDs; relações são remapeadas para os novos IDs e atribuições presentes apontam para a conta de destino.

Nomes de usuário são únicos por comparação exata no aparelho. A transferência não exige que o nome ou UUID da conta de origem coincida com o destino.

## 6. Lembretes

A reconciliação cancela trabalhos das tarefas substituídas e agenda somente tarefas ativas com prazo futuro. Lembretes exibem dados apenas da conta ativa; sair limpa as notificações e os trabalhos de prazo, e entrar reconcilia novamente. Workers revalidam acesso antes de publicar ou alterar dados.

## 7. Backup automático do Android

Continua desabilitado (`allowBackup=false`, `fullBackupContent=false` e regras de extração excludentes). O arquivo manual é o mecanismo portátil e não transporta material do Android Keystore.
