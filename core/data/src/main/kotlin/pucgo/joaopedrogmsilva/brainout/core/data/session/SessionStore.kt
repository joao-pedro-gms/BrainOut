// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pucgo.joaopedrogmsilva.brainout.core.data.util.logDebug
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException

private const val TAG = "BrainOut:SessionStore"

/**
 * Persiste o id do [pucgo.joaopedrogmsilva.brainout.core.domain.model.User]
 * autenticado em [DataStore] (Preferences) para que o app restaure a
 * sessão após reinício (E1.8 do ROADMAP).
 *
 * O arquivo `auth_prefs.preferences_pb` é gerenciado pela delegate
 * [Context.authDataStore]. O id é a única informação necessária
 * para reidratar a sessão — nome, e-mail e papel são lidos do Room
 * usando `UserRepository.findById(id)`.
 *
 * Não armazena credenciais, tokens ou hash de senha.
 */
class SessionStore(
    private val dataStore: DataStore<Preferences>,
) {
    private val accountMutex = Mutex()

    /** Impede a troca de sessão durante uma escrita da conta capturada. */
    suspend fun <T> withUser(
        expectedId: String,
        block: suspend () -> T,
    ): T =
        accountMutex.withLock {
            if (currentUserId() != expectedId) throw OwnershipException()
            block()
        }

    /** Observa o id do usuário autenticado. `null` significa "sem sessão". */
    fun observeUserId(): Flow<String?> =
        dataStore.data.map { prefs ->
            prefs[USER_ID_KEY]
        }

    /**
     * Versão suspensa da leitura para uso em pontos de inicialização
     * (ex.: `MainActivity.onCreate`) onde o valor único é suficiente.
     *
     * Usa [Flow.first] para obter uma única emissão e suspender até
     * o `DataStore` estar pronto — é seguro cancelar normalmente.
     */
    suspend fun currentUserId(): String? {
        val id = observeUserId().first()
        logDebug(TAG, "Lendo userId atual do DataStore: $id")
        return id
    }

    /** Persiste o [userId] como sessão ativa. */
    suspend fun saveUserId(userId: String): Unit =
        accountMutex.withLock {
            require(userId.isNotBlank()) { "userId não pode ser vazio" }
            logDebug(TAG, "Salvando userId no DataStore: $userId")
            dataStore.edit { prefs ->
                prefs[USER_ID_KEY] = userId
            }
        }

    /**
     * Limpa **todo** o estado persistido neste [DataStore]: a sessão
     * atual e a marca de que a permissão de notificação já foi pedida.
     *
     * Usado no logout e quando a sessão aponta para um usuário que não
     * existe mais no Room. Para encerrar só a sessão (restauração de
     * backup), use [clearSession] — lá a permissão continua válida.
     */
    suspend fun clearAll(): Unit =
        accountMutex.withLock {
            logDebug(TAG, "Limpando sessão e marca de permissão no DataStore")
            dataStore.edit { prefs ->
                prefs.remove(USER_ID_KEY)
                prefs.remove(NOTIFICATION_PERMISSION_ASKED_KEY)
            }
        }

    /**
     * Encerra **apenas** a sessão, preservando a marca de permissão de
     * notificação.
     *
     * Usado pela restauração de backup (issue #214 — LOCAL-03): o
     * backup substitui as contas do aparelho, então a sessão persistida
     * pode apontar para um registro que não existe mais. A permissão de
     * notificação é independente da sessão e continua válida — pedir a
     * permissão de novo seria um transtorno desnecessário.
     */
    suspend fun clearSession(): Unit =
        accountMutex.withLock {
            logDebug(TAG, "Limpando apenas a sessão no DataStore (restauração de backup)")
            dataStore.edit { prefs ->
                prefs.remove(USER_ID_KEY)
            }
        }

    /**
     * `true` se a permissão `POST_NOTIFICATIONS` já foi pedida alguma
     * vez (marco E3.6). Independente da sessão: sobrevive a logout.
     * Removida apenas em [clearAll] (reset do DataStore).
     */
    suspend fun wasNotificationPermissionAsked(): Boolean =
        dataStore.data.first()[NOTIFICATION_PERMISSION_ASKED_KEY] == true

    /** Registra que a permissão de notificações foi pedida (E3.6). */
    suspend fun markNotificationPermissionAsked() {
        logDebug(TAG, "Marcando permissão POST_NOTIFICATIONS como pedida no DataStore")
        dataStore.edit { prefs ->
            prefs[NOTIFICATION_PERMISSION_ASKED_KEY] = true
        }
    }

    companion object {
        /** Nome do arquivo Preferences usado pelo [DataStore]. */
        const val PREFERENCES_NAME: String = "auth_prefs"

        /** Chave interna do id do usuário. */
        val USER_ID_KEY: Preferences.Key<String> = stringPreferencesKey("user_id")

        /** Chave da flag "permissão POST_NOTIFICATIONS já pedida" (E3.6). */
        val NOTIFICATION_PERMISSION_ASKED_KEY: Preferences.Key<Boolean> =
            booleanPreferencesKey("notification_permission_asked")
    }
}

/**
 * Delegate que cria a instância singleton de
 * [DataStore]<[Preferences]> para o nome
 * [SessionStore.PREFERENCES_NAME].
 *
 * Mantida no escopo do arquivo porque é detalhe de implementação do
 * Android DataStore — não deve ser acessada diretamente fora do
 * módulo `:core:data`.
 */
val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SessionStore.PREFERENCES_NAME,
)
