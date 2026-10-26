// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.ProjectTagCrossRef
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TagEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.TaskEntity
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

/**
 * DAO de operações em massa do backup local (issue #214 — LOCAL-03).
 *
 * Fica separado dos DAOs de domínio ([UserDao], [ProjectDao],
 * [TaskDao], [TagDao]) por dois motivos:
 *
 * - **Escopo:** as operações de ler-tudo, gravar-em-massa e
 *   apagar-tudo existem **apenas** para a exportação e a restauração
 *   de backup. Elas não pertencem ao contrato de CRUD das telas.
 * - **Tamanho:** os DAOs de domínio já concentram as regras de
 *   cascata (RN03) e as consultas de listagem; mantê-los enxutos
 *   deixa o diff auditável.
 *
 * A restauração grava o conteúdo inteiro dentro de **uma**
 * transação Room — ver
 * [pucgo.joaopedrogmsilva.brainout.core.data.backup.BackupRepositoryImpl].
 *
 * A quantidade de funções é maior que a regra padrão porque a
 * interface é deliberadamente **plana**: uma função por operação de
 * leitura e de escrita em massa, sem lógica. O que a regra quer
 * evitar (objetos com responsabilidades demais) não se aplica a uma
 * lista de consultas de uma linha cada.
 */
@Dao
@Suppress("TooManyFunctions") // A justificativa está no KDoc da interface: uma função por operação, sem lógica.
interface BackupDao {
    // --- Leitura ---

    /** Todos os usuários, em ordem de criação. */
    @Query("SELECT * FROM users ORDER BY created_at ASC")
    suspend fun getAllUsers(): List<UserEntity>

    /** Todos os projetos, em ordem de criação. */
    @Query("SELECT * FROM projects ORDER BY created_at ASC")
    suspend fun getAllProjects(): List<ProjectEntity>

    /** Todas as tarefas, em ordem de criação. */
    @Query("SELECT * FROM tasks ORDER BY created_at ASC")
    suspend fun getAllTasks(): List<TaskEntity>

    /** Todas as etiquetas, em ordem de criação. */
    @Query("SELECT * FROM tags ORDER BY created_at ASC")
    suspend fun getAllTags(): List<TagEntity>

    /** Todas as associações projeto ↔ etiqueta. */
    @Query("SELECT * FROM project_tags")
    suspend fun getAllProjectTags(): List<ProjectTagCrossRef>

    /** Ids de todas as tarefas — usado pela reconciliação de lembretes. */
    @Query("SELECT id FROM tasks")
    suspend fun getAllTaskIds(): List<String>

    @Query("DELETE FROM projects WHERE owner_id = :ownerId")
    suspend fun deleteProjectsForOwner(ownerId: String)

    @Query("DELETE FROM tags WHERE owner_id = :ownerId")
    suspend fun deleteTagsForOwner(ownerId: String)

    // --- Escrita ---

    /** Grava as contas restauradas. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUsers(users: List<UserEntity>)

    /** Grava os projetos restaurados. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProjects(projects: List<ProjectEntity>)

    /** Grava as tarefas restauradas. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    /** Grava as etiquetas restauradas. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTags(tags: List<TagEntity>)

    /** Grava as associações projeto ↔ etiqueta restauradas. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectTags(refs: List<ProjectTagCrossRef>)

    /** Remove todas as contas. */
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    /** Remove todos os projetos. */
    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    /** Remove todas as tarefas. */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    /** Remove todas as etiquetas. */
    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    /** Remove todas as associações projeto ↔ etiqueta. */
    @Query("DELETE FROM project_tags")
    suspend fun deleteAllProjectTags()

    /**
     * Substitui o conjunto de associações projeto ↔ etiqueta.
     *
     * Executa a limpeza e a gravação na mesma transação Room — ou as
     * duas acontecem, ou nenhuma.
     */
    @Transaction
    suspend fun replaceProjectTags(refs: List<ProjectTagCrossRef>) {
        deleteAllProjectTags()
        if (refs.isNotEmpty()) {
            insertProjectTags(refs)
        }
    }
}
