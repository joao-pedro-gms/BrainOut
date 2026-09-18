// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

/**
 * [PepperProvider] determinístico usado em testes.
 *
 * Substitui a [PepperProvider.Default] (que precisa de
 * `MasterKey`/AndroidKeyStore) por uma fonte fixa — assim o KDF
 * pode ser exercitado sem dependência do subsistema de chaves do
 * Android.
 */
internal class FixedPepperProvider(
    private val pepper: ByteArray,
) : PepperProvider {
    override fun bytes(): ByteArray = pepper
}
