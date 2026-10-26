// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.backup

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferences
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder

internal inline fun <T> BrainOutDatabase.use(block: (BrainOutDatabase) -> T): T =
    try {
        block(this)
    } finally {
        close()
    }

internal class BackupListingPreferences : ListingPreferencesRepository {
    private val entries = mutableMapOf<String, MutableStateFlow<ListingPreferences>>()

    override fun observe(userId: String): Flow<ListingPreferences> = entry(userId)

    override suspend fun setSearchQuery(
        userId: String,
        query: String,
    ) {
        entry(userId).value =
            entry(userId).value.copy(searchQuery = query)
    }

    override suspend fun setSelectedTagId(
        userId: String,
        tagId: String?,
    ) {
        entry(userId).value =
            entry(userId).value.copy(selectedTagId = tagId)
    }

    override suspend fun setSortOrder(
        userId: String,
        order: SortOrder,
    ) {
        entry(userId).value =
            entry(userId).value.copy(sortOrder = order)
    }

    private fun entry(id: String): MutableStateFlow<ListingPreferences> =
        entries.getOrPut(id) {
            MutableStateFlow(ListingPreferences())
        }
}
