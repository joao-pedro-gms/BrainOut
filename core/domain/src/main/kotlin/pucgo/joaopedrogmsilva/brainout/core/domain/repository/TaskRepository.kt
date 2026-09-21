// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.repository

import kotlinx.coroutines.flow.Flow
import pucgo.joaopedrogmsilva.brainout.core.domain.model.Task
import pucgo.joaopedrogmsilva.brainout.core.domain.model.TaskStatus

interface TaskRepository {
    fun observeForProject(projectId: String): Flow<List<Task>>
    fun observeAllForOwner(ownerId: String): Flow<List<Task>>
    suspend fun findById(id: String): Task?
    suspend fun create(task: Task): Task
    suspend fun update(task: Task): Task
    suspend fun changeStatus(id: String, target: TaskStatus): Task
    suspend fun delete(id: String)

    /**
     * Conta tarefas ativas (status != DONE) de um projeto. Usado pelo
     * `CreateTaskUseCase` para aplicar a regra de negócio RN01 sem
     * precisar importar `:core:data` no domínio.
     */
    suspend fun countActiveByProject(projectId: String): Int

    /**
     * Marca uma tarefa como [TaskStatus.DONE] **e** aplica a cascata
     * de RN03 (E2.5) **na mesma transação Room**: ao concluir a
     * última tarefa ativa do projeto, o projeto é marcado como
     * concluído; se não for a última, o projeto permanece ativo.
     *
     * A regra do domínio (matriz de transições válidas + abertura
     * de [Task.completedAt]) é aplicada antes da escrita, e a
     * atualização do projeto só acontece se a transição resultar em
     * `countActive == 0` — ambas as operações dentro de uma única
     * `@Transaction` para que observadores no Room nunca vejam um
     * estado intermediário em que a tarefa está DONE mas o projeto
     * ainda não foi marcado.
     *
     * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException]
     * se a transição pedida não for permitida pela matriz, e
     * `IllegalArgumentException` se a tarefa não existir.
     */
    suspend fun completeAndCascade(taskId: String): Task

    /**
     * Reabre uma tarefa (transição para um estado ativo) **e** aplica
     * a cascata inversa de RN03 (E2.5) **na mesma transação Room**:
     * se o projeto estiver concluído e a tarefa voltar a ser ativa,
     * o projeto é desmarcado como concluído. Se o projeto já estava
     * ativo, permanece como está.
     *
     * Lança [pucgo.joaopedrogmsilva.brainout.core.domain.error.InvalidStateTransitionException]
     * se a transição pedida não for permitida pela matriz
     * (ex.: reabertura para um estado que não é target válido a
     * partir do status atual).
     */
    suspend fun reopenAndCascade(taskId: String, target: TaskStatus): Task
}
