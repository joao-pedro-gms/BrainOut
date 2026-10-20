// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

/**
 * Erros do fluxo de backup local (issue #214 — LOCAL-03).
 *
 * Todas as falhas de backup são **recuperáveis**: a camada de dados
 * garante que um arquivo inválido, truncado, com versão desconhecida
 * ou fora do limite de tamanho falha **antes** de qualquer escrita —
 * o conteúdo já existente no aparelho nunca é alterado por uma
 * importação malsucedida.
 *
 * A hierarquia segue a regra do repositório: [DomainException] é a
 * raiz única do pacote `error/`, e estas classes ficam no mesmo
 * pacote (a selagem do Kotlin é resolvida no mesmo módulo).
 */
sealed class BackupException(
    message: String,
) : DomainException(message)

/**
 * O arquivo não é um backup do BrainOut — formato desconhecido, JSON
 * malformado ou envelope sem os campos obrigatórios.
 */
class BackupFormatException(
    message: String,
) : BackupException(message)

/**
 * O backup foi gerado por uma versão do formato que este aplicativo
 * não sabe ler.
 *
 * @property version número da versão declarada no envelope.
 */
class UnsupportedBackupVersionException(
    val version: Int,
) : BackupException("Versão de backup não suportada: $version")

/**
 * A verificação de integridade falhou: senha incorreta ou arquivo
 * adulterado/truncado.
 *
 * A mensagem é deliberadamente genérica — o AES-GCM não distingue
 * "senha errada" de "bytes corrompidos", e inventar essa distinção
 * seria mentir para o usuário.
 */
class BackupIntegrityException : BackupException("Senha incorreta ou arquivo de backup corrompido.")

/**
 * O arquivo excede o limite de tamanho aceito.
 *
 * @property limitBytes limite aplicado, em bytes.
 */
class BackupTooLargeException(
    val limitBytes: Int,
) : BackupException("Arquivo de backup excede o limite de $limitBytes bytes.")

/**
 * A restauração foi abortada por uma falha na gravação dos dados
 * (violação de restrição única, registro inconsistente ou erro de banco).
 */
class BackupRestoreException(
    message: String,
) : BackupException(message)
