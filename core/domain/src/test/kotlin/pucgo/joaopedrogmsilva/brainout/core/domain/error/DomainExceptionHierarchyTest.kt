// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.error

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Testes da **raiz única** de exceção do domínio.
 *
 * Antes de 2026-10-08, [BusinessRuleException] estendia
 * `RuntimeException` e [DomainException] estendia
 * `IllegalArgumentException` — duas raízes irmãs e desconectadas.
 * Nenhum `catch (DomainException)` no repositório capturava uma
 * violação de regra de negócio, o que fazia
 * [ProjectTaskLimitReachedException] e
 * [TaskPriorityChangeForbiddenException] escaparem de handlers que
 * tratam violação permanente (ex.:
 * `app/.../notifications/CompleteTaskWorker.kt`).
 *
 * Estes testes fixam o contrato da hierarquia unificada sem quebrar o
 * contrato já testado pelos modelos: `require()` continua lançando
 * `IllegalArgumentException` cru, que segue sendo ancestral de
 * [DomainException].
 */
class DomainExceptionHierarchyTest {
    @Test
    fun `violacao de regra de negocio e capturada por DomainException`() {
        val limite = ProjectTaskLimitReachedException(projectId = "p-1", limit = 50)
        val prioridade = TaskPriorityChangeForbiddenException("t-1")

        listOf<Throwable>(limite, prioridade).forEach { excecao ->
            assertThat(excecao).isInstanceOf(DomainException::class.java)
            assertThat(excecao).isInstanceOf(BusinessRuleException::class.java)
        }
    }

    @Test
    fun `subclasses de RN continuam distinguiveis entre si`() {
        // A UI (ProjectDetailViewModel) distingue RN01 de RN02 por
        // tipo; a unificação não pode apagar essa distinção.
        val limite = ProjectTaskLimitReachedException("p-1", 50)
        val prioridade = TaskPriorityChangeForbiddenException("t-1")

        assertThat(prioridade)
            .isNotInstanceOf(ProjectTaskLimitReachedException::class.java)
        assertThat(limite)
            .isNotInstanceOf(TaskPriorityChangeForbiddenException::class.java)
    }

    @Test
    fun `DomainException continua sendo IllegalArgumentException para os modelos`() {
        // Contrato testado por UserTest/ProjectTest/TagInvariantTest/
        // TaskTest: as `require()` dos modelos lançam
        // IllegalArgumentException crua. Manter esse ancestral evita
        // quebrar esses testes sem padronizar (invisto demais).
        assertThat(InvalidModelException("nome vazio"))
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(TaskNotFoundException("t-1"))
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `violacao de RN tambem e IllegalArgumentException por heranca`() {
        // Consequência direta de DomainException : IllegalArgumentException
        // — um `catch (IllegalArgumentException)` legado continua
        // capturando erros de domínio em vez de deixar escapar.
        assertThat(TaskPriorityChangeForbiddenException("t-1"))
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `regra de negocio nao e capturada por InvalidModelException`() {
        // As duas famílias de erro continuam disjuntas: RN é
        // BusinessRuleException, modelo é InvalidModelException.
        assertThat(ProjectTaskLimitReachedException("p-1", 50))
            .isNotInstanceOf(InvalidModelException::class.java)
    }
}
