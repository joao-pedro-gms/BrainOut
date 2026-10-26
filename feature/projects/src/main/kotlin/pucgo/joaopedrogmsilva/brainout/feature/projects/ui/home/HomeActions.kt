// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Bodies das ações de CRUD e preferências do [HomeViewModel]:
// extraídos para manter o orquestrador ≤200 LoC. Cada função recebe
// o `errorSink`/repositório/use case como parâmetro explícito para
// preservar a visibilidade `private` do estado do ViewModel.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.ActiveUserProvider
import pucgo.joaopedrogmsilva.brainout.core.domain.error.DuplicateTagNameException
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Project
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.SortOrder
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.CreateTagUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.DeleteProjectUseCase
import pucgo.joaopedrogmsilva.brainout.core.domain.usecase.UpdateProjectUseCase
import kotlin.coroutines.cancellation.CancellationException

/** Tag de log das ações da Home. */
private const val TAG: String = "BrainOut:HomeActions"

/**
 * Executa [block] no [scope], convertendo falhas em mensagem no
 * [errorSink].
 *
 * Uma única implementação do try/catch: as sete ações da Home
 * repetiam o mesmo bloco, e uma delas divergir (ou esquecer de
 * re-lançar o cancelamento) seria um bug silencioso.
 */
private fun runAction(
    scope: CoroutineScope,
    errorSink: MutableStateFlow<HomeErrorType?>,
    block: suspend () -> Unit,
) {
    scope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: DuplicateTagNameException) {
            // DEF-61 — nome de tag repetido: a tela diz exatamente o
            // que houve, em vez da mensagem genérica de falha.
            Log.w(TAG, "Nome de tag duplicado: " + e.name)
            errorSink.value = HomeErrorType.DuplicateTagName
        } catch (e: OwnershipException) {
            // DEF-36 — o domínio recusou a operação: o papel do usuário
            // ativo não tem a permissão exigida.
            Log.w(TAG, "Operacao recusada por falta de permissao: " + e.message)
            errorSink.value = HomeErrorType.PermissionDenied
        } catch (e: Throwable) {
            Log.w(TAG, "Falha na acao da Home: " + e.message, e)
            errorSink.value = HomeErrorType.ActionFailed
        }
    }
}

/**
 * Executa [block] no [scope] com o id do usuário ativo.
 *
 * Sem usuário ativo (sessão encerrada), a ação é ignorada — a mesma
 * regra das quatro ações de CRUD.
 */
private fun runForActiveUser(
    scope: CoroutineScope,
    activeUserProvider: ActiveUserProvider,
    block: suspend (ownerId: String) -> Unit,
) {
    scope.launch {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@launch
        block(ownerId)
    }
}

/** Dispara [CreateProjectUseCase] capturando falhas em [errorSink]. */
internal fun runCreateProject(
    scope: CoroutineScope,
    useCase: CreateProjectUseCase,
    activeUserProvider: ActiveUserProvider,
    name: String,
    description: String?,
    tagIds: List<String>,
    errorSink: MutableStateFlow<HomeErrorType?>,
    onSuccess: () -> Unit = {},
) {
    runAction(scope, errorSink) {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@runAction
        useCase.invoke(
            name = name,
            ownerId = ownerId,
            description = description,
            tagIds = tagIds,
        )
        onSuccess()
    }
}

/** Dispara [UpdateProjectUseCase] capturando falhas em [errorSink]. */
internal fun runUpdateProject(
    scope: CoroutineScope,
    useCase: UpdateProjectUseCase,
    project: Project,
    tagIds: List<String>,
    errorSink: MutableStateFlow<HomeErrorType?>,
) {
    runAction(scope, errorSink) {
        useCase.invoke(project, tagIds)
    }
}

/** Dispara [DeleteProjectUseCase] capturando falhas em [errorSink]. */
internal fun runDeleteProject(
    scope: CoroutineScope,
    useCase: DeleteProjectUseCase,
    projectId: String,
    errorSink: MutableStateFlow<HomeErrorType?>,
) {
    runAction(scope, errorSink) {
        useCase.invoke(projectId)
    }
}

/** Dispara [CreateTagUseCase] capturando falhas em [errorSink]. */
internal fun runCreateTag(
    scope: CoroutineScope,
    useCase: CreateTagUseCase,
    activeUserProvider: ActiveUserProvider,
    name: String,
    color: String,
    errorSink: MutableStateFlow<HomeErrorType?>,
) {
    runAction(scope, errorSink) {
        val ownerId = activeUserProvider.observeActiveUserId().first() ?: return@runAction
        useCase.invoke(ownerId, name, color)
    }
}

/** Persiste o texto de busca via [ListingPreferencesRepository]. */
internal fun runOnSearchQueryChange(
    scope: CoroutineScope,
    repo: ListingPreferencesRepository,
    activeUserProvider: ActiveUserProvider,
    newQuery: String,
) {
    runForActiveUser(scope, activeUserProvider) { ownerId ->
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
    runForActiveUser(scope, activeUserProvider) { ownerId ->
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
    runForActiveUser(scope, activeUserProvider) { ownerId ->
        repo.setSortOrder(ownerId, order)
    }
}
