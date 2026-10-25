// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.usecase

/**
 * Regra de tamanho mínimo da senha.
 *
 * Única autoridade — a ViewModel e a camada de UI apenas espelham
 * esta constante para mensagens e validações antecipadas. Centralizar
 * evita divergência entre a regra client-side (UX rápida) e a regra
 * de domínio (defesa em profundidade).
 */
const val MIN_PASSWORD_LENGTH: Int = 8

/**
 * Exige uma senha com pelo menos [MIN_PASSWORD_LENGTH] caracteres.
 *
 * A guarda é a **fonte só** da regra: cadastro, login e backup exigem
 * o mesmo mínimo, e a mensagem da falha sai daqui. Sem ela, cada caso
 * de uso escrevia o seu próprio `require` — e o export e o restore
 * (que compartilham a senha do arquivo) podiam divergir entre si:
 * o app aceitaria uma senha no backup e a recusaria na restauração.
 *
 * @param password a senha em texto puro.
 * @param subject como a senha é chamada na mensagem de erro.
 * @throws IllegalArgumentException se a senha for curta demais.
 */
fun requireValidPassword(
    password: String,
    subject: String = "Senha",
) {
    require(password.length >= MIN_PASSWORD_LENGTH) {
        "$subject deve ter pelo menos $MIN_PASSWORD_LENGTH caracteres"
    }
}
