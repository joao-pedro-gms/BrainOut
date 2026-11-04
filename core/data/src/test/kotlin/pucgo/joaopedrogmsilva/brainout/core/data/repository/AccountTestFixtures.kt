// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore

internal fun testSession(ownerId: String = "u1"): SessionStore = SessionStore(MemoryPreferences(ownerId))

internal fun testAccess(
    transaction: LocalTransaction = FakeLocalTransaction(),
    ownerId: String = "u1",
): AccountAccess = AccountAccess(testSession(ownerId), transaction)

private class MemoryPreferences(
    ownerId: String,
) : DataStore<Preferences> {
    private val state = MutableStateFlow(preferencesOf(SessionStore.USER_ID_KEY to ownerId))
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
