// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Bodies das ações de CRUD e preferências do [HomeViewModel]:
// extraídos para manter o orquestrador ≤200 LoC. Cada função recebe
// o `errorSink`/repositório/use case como parâmetro explícito para
// preservar a visibilidade `private` do estado do ViewModel.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase

/** Dispara [CreateProjectUseCase] capturando falhas em [errorSink]. */
internal fun runCreateProject(
    scope: CoroutineScope,
    useCase: CreateProjectUseCase,
    activeUserProvider: ActiveUserProvider,
    name: String,
    description: String?,
    tagIds: List<String>,
    errorSink: MutableStateFlow<String?>,
) {
    scope.launch {
        try {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            useCase.invoke(
                name = name,
                ownerId = ownerId,
                description = description,
                tagIds = tagIds,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            errorSink.value = e.toHomeActionErrorMessage()
        }
    }
}

/** Dispara [UpdateProjectUseCase] capturando falhas em [errorSink]. */
internal fun runUpdateProject(
    scope: CoroutineScope,
    useCase: UpdateProjectUseCase,
    project: Project,
    tagIds: List<String>,
    errorSink: MutableStateFlow<String?>,
) {
    scope.launch {
        try {
            useCase.invoke(project, tagIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            errorSink.value = e.toHomeActionErrorMessage()
        }
    }
}

/** Dispara [DeleteProjectUseCase] capturando falhas em [errorSink]. */
internal fun runDeleteProject(
    scope: CoroutineScope,
    useCase: DeleteProjectUseCase,
    projectId: String,
    errorSink: MutableStateFlow<String?>,
) {
    scope.launch {
        try {
            useCase.invoke(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            errorSink.value = e.toHomeActionErrorMessage()
        }
    }
}

/** Dispara [CreateTagUseCase] capturando falhas em [errorSink]. */
internal fun runCreateTag(
    scope: CoroutineScope,
    useCase: CreateTagUseCase,
    activeUserProvider: ActiveUserProvider,
    name: String,
    color: String,
    errorSink: MutableStateFlow<String?>,
) {
    scope.launch {
        try {
            val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
            useCase.invoke(ownerId, name, color)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            errorSink.value = e.toHomeActionErrorMessage()
        }
    }
}

/** Persiste o texto de busca via [ListingPreferencesRepository]. */
internal fun runOnSearchQueryChange(
    scope: CoroutineScope,
    repo: ListingPreferencesRepository,
    activeUserProvider: ActiveUserProvider,
    newQuery: String,
) {
    scope.launch {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
        repo.setSearchQuery(ownerId, newQuery)
    }
}

/** Persiste a tag selecionada como filtro (E2.6). */
internal fun runOnTagFilterChange(
    scope: CoroutineScope,
    repo: ListingPreferencesRepository,
    activeUserProvider: ActiveUserProvider,
    tagId: String?,
) {
    scope.launch {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
        repo.setSelectedTagId(ownerId, tagId)
    }
}

/** Persiste a ordenação escolhida (E2.6). */
internal fun runOnSortOrderChange(
    scope: CoroutineScope,
    repo: ListingPreferencesRepository,
    activeUserProvider: ActiveUserProvider,
    order: SortOrder,
) {
    scope.launch {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
        repo.setSortOrder(ownerId, order)
    }
}
