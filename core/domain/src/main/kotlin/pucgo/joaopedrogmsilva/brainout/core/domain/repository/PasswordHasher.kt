// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

/**
 * Porta para serviços de hashing de senha.
 *
 * Permite que a camada de domínio dependa apenas de uma abstração,
 * enquanto a implementação concreta (BCrypt, Argon2, scrypt) é
 * escolhida no `:core:data` ou em uma biblioteca de segurança.
 *
 * A separação facilita testes unitários: testes injetam um fake
 * determinístico (ex.: sempre devolve "hashed(<senha>)").
 *
 * **As duas operações são `suspend`** porque o KDF é CPU-bound e
 * caro (PBKDF2-HMAC-SHA256 com 120 000 iterações leva centenas de
 * milissegundos). Com a porta síncrona, quem chamasse de dentro de
 * um `viewModelScope` (que roda no dispatcher principal) congelaria a
 * interface durante o login e o cadastro. A declaração `suspend` faz
 * o custo explícito no contrato, e a implementação decide em qual
 * dispatcher o trabalho acontece — de modo que nenhum chamador
 * precise saber disso.
 */
interface PasswordHasher {
    /**
     * Recebe a senha em texto puro e retorna o hash a ser persistido.
     *
     * Lança [IllegalArgumentException] se [rawPassword] estiver vazia.
     */
    suspend fun hash(rawPassword: String): String

    /**
     * Verifica se [rawPassword] corresponde ao [storedHash] previamente
     * gerado por [hash]. Deve ser resistente a timing attacks quando
     * executado em produção.
     */
    suspend fun verify(
        rawPassword: String,
        storedHash: String,
    ): Boolean
}
