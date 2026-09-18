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
 */
interface PasswordHasher {

    /**
     * Recebe a senha em texto puro e retorna o hash a ser persistido.
     *
     * Lança [IllegalArgumentException] se [rawPassword] estiver vazia.
     */
    fun hash(rawPassword: String): String

    /**
     * Verifica se [rawPassword] corresponde ao [storedHash] previamente
     * gerado por [hash]. Deve ser resistente a timing attacks quando
     * executado em produção.
     */
    fun verify(rawPassword: String, storedHash: String): Boolean
}
