// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fonte do pepper aplicado antes do PBKDF2 em [PasswordHasherImpl].
 *
 * O pepper é concatenado à senha antes da derivação: um dump do banco
 * (com sal e hash) não basta para verificação offline — o atacante
 * também precisa do pepper.
 *
 * O pepper é gerado uma única vez por instalação, usando
 * [SecureRandom], e persistido em um [EncryptedSharedPreferences]
 * (chave AES-256 protegida por MasterKey do AndroidKeystore). Ele é
 * determinístico por dispositivo e instâncias do app no mesmo
 * dispositivo compartilham o mesmo pepper (escopo por instalação,
 * não por processo).
 *
 * Iterações seguras: 16 bytes = 128 bits de entropia, suficiente
 * para neutralizar rainbow tables mesmo em dump de banco.
 */
interface PepperProvider {

    fun bytes(): ByteArray

    /**
     * Implementação padrão baseada em [EncryptedSharedPreferences].
     */
    @Singleton
    class Default @Inject constructor(
        @ApplicationContext private val context: Context,
    ) : PepperProvider {

        override fun bytes(): ByteArray {
            val prefs: SharedPreferences = EncryptedSharedPreferences.create(
                context.applicationContext,
                PEPPER_PREFS_NAME,
                MasterKey.Builder(context.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
            val existing = prefs.getString(PEPPER_KEY, null)
            if (existing != null) {
                return Base64.decode(existing, Base64.NO_WRAP)
            }
            val generated = ByteArray(PEPPER_LENGTH_BYTES).also {
                SecureRandom().nextBytes(it)
            }
            prefs.edit()
                .putString(PEPPER_KEY, Base64.encodeToString(generated, Base64.NO_WRAP))
                .apply()
            return generated
        }

        private companion object {
            const val PEPPER_PREFS_NAME: String = "brainout_pepper"
            const val PEPPER_KEY: String = "v1"
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
