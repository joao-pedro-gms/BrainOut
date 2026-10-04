// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pucgo.joaopedrogmsilva.brainout.core.domain.model.ThemeMode
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ThemePreferencesRepository

/**
 * Implementação DataStore (Preferences) de [ThemePreferencesRepository]
 * (NB-06, issue #93 — E4.5).
 *
 * Reaproveita o [DataStore]<[Preferences]> singleton do módulo
 * (`auth_prefs`), o mesmo usado pela sessão e pelas preferências de
 * listagem: a chave é **global** (não é namespaced por `userId`)
 * porque o tema é preferência de dispositivo — sobrevive a
 * logout/login, e o `SessionStore.clear()` (granular, remove só
 * `user_id` e a flag de notificação) a preserva.
 *
 * Antes do NB-06 esta escolha simplesmente não existia; o default é
 * [ThemeMode.SYSTEM], o comportamento anterior do app (seguir o
 * sistema), então instalar esta porta não muda tela nenhuma.
 *
 * @property dataStore instância do `DataStore<Preferences>` injetada
 *  pelo Hilt via
 *  [pucgo.joaopedrogmsilva.brainout.core.data.di.DataModule].
 */
@Singleton
class ThemePreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository {

    override fun observe(): Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorageKey(prefs[THEME_MODE_KEY])
    }

    override suspend fun set(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.toStorageKey()
        }
    }

    companion object {
        /**
         * Chave única da preferência de tema. Exposta (como em
         * [pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore])
         * para os testes zerarem o estado residual — a chave não é
         * namespaced por usuário.
         */
        val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")
    }
}
