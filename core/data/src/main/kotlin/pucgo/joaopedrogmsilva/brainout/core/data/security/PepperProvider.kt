// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import android.content.Context
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fonte do pepper aplicado antes do PBKDF2 em [PasswordHasherImpl].
 *
 * O pepper é concatenado à senha antes da derivação: um dump do banco
 * (com sal e hash) não basta para verificação offline — o atacante
 * também precisa do pepper, que vive em local seguro (hardware-backed
 * Keystore, quando disponível).
 *
 * O contrato é stateless — `bytes()` é idempotente e retorna o mesmo
 * pepper para a mesma instalação do app. A implementação [Default]
 * usa a [MasterKey] do `androidx.security:security-crypto` como
 * âncora: o alias da chave é determinístico por instalação e seu
 * resumo SHA-256 vira o pepper.
 */
interface PepperProvider {

    fun bytes(): ByteArray

    /**
     * Implementação padrão usando a [MasterKey] do security-crypto.
     *
     * Evita manter o pepper em texto puro no construtor e continua
     * determinístico entre execuções no mesmo dispositivo — a chave
     * mestre é restaurada a partir do AndroidKeyStore.
     */
    @Singleton
    class Default @Inject constructor(
        @ApplicationContext private val context: Context,
    ) : PepperProvider {
        override fun bytes(): ByteArray {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val alias = masterKey.toString().toByteArray(Charsets.UTF_8)
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(alias).copyOf(PEPPER_LENGTH_BYTES)
        }

        private companion object {
            const val PEPPER_LENGTH_BYTES: Int = 16
        }
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object Factory {
        @Provides
        @Singleton
        fun providePepperProvider(default: Default): PepperProvider = default
    }
}
