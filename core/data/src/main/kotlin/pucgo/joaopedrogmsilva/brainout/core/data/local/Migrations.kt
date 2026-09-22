// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migração Room v1 → v2.
 *
 * Adiciona as tabelas introduzidas na E2.1/E2.2/E2.6 sem destruir
 * dados existentes — `users` (criada na v1) é preservada.
 *
 * - `projects`: agrupador de tarefas por usuário.
 * - `tasks`: tarefas filhas de um projeto (FK CASCADE).
 * - `tags`: etiquetas pessoais com unicidade `(owner_id, name)`.
 * - `project_tags`: junção N:N entre projeto e tag (FKs CASCADE).
 *
 * Cada `CREATE TABLE` é idempotente (`IF NOT EXISTS`) para tolerar
 * reinstalações em desenvolvimento. O schema gerado pelo Room usa a
 * mesma forma textual (backticks, `INTEGER NOT NULL` para Boolean,
 * FOREIGN KEY inline), então o `identityHash` da v2 calculado pelo
 * Room durante o `assembleDebug` bate com o esperado quando o banco
 * já está na v1 e esta migração é executada.
 *
 * Não há migração destrutiva — uma instalação com `users` cadastrados
 * continua funcional após o upgrade; as novas tabelas começam vazias.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` TEXT NOT NULL,
                `owner_id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `created_at` INTEGER NOT NULL,
                `is_completed` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_projects_owner_id` ON `projects`(`owner_id`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` TEXT NOT NULL,
                `project_id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `priority_code` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `assignee_id` TEXT,
                `due_date` INTEGER,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_tasks_project_id` ON `tasks`(`project_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_tasks_status` ON `tasks`(`status`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` TEXT NOT NULL,
                `owner_id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `color` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `idx_tags_owner_name` ON `tags`(`owner_id`, `name`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `project_tags` (
                `project_id` TEXT NOT NULL,
                `tag_id` TEXT NOT NULL,
                PRIMARY KEY(`project_id`, `tag_id`),
                FOREIGN KEY(`project_id`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_project_tags_tag_id` ON `project_tags`(`tag_id`)",
        )
    }
}

/**
 * Migração Room v2 → v3 (RN03 — E2.5).
 *
 * Adiciona a coluna `completed_at` à tabela `tasks` para registrar
 * o instante em que cada tarefa passou a [TaskStatus.DONE] (RN03 —
 * conclusão cascata). A migração é **não destrutiva**:
 *
 * - `ALTER TABLE` com `ADD COLUMN completed_at INTEGER`. Não há
 *   `DROP`, `DELETE` ou recriação de tabela — dados existentes
 *   permanecem íntegros.
 * - Sem `DEFAULT` explícito: tarefas preexistentes (v2) ficam com
 *   `completed_at IS NULL`. A regra do domínio
 *   ([Task.init]) aceita `completedAt = null` mesmo para tarefas
 *   já em DONE, cobrindo o caso de registros legados sem inventar
 *   data retroativa. Apenas a próxima conclusão da tarefa
 *   preencherá o valor.
 * - Sem novo índice: a coluna não é critério de busca nesta
 *   release (a conclusão cascata usa `project_id` +
 *   `status != 'DONE'`, índices já presentes).
 *
 * Se o esquema divergir entre exportado (Room) e migração manual,
 * o `identityHash` calculado pelo Room durante o `assembleDebug`
 * baterá com o do `schemas/.../3.json` por causa da forma textual
 * idêntica (backticks, INTEGER nulo, mesma posição da coluna).
 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `completed_at` INTEGER")
    }
}

/**
 * Migração Room v3 → v4 (E3.3 — fila offline).
 *
 * Cria a tabela `pending_ops` da fila de operações pendentes de
 * sincronização. Não destrutiva: apenas `CREATE TABLE IF NOT EXISTS`
 * + índice; nenhum dado existente é tocado. A tabela não tem FK —
 * ops podem sobreviver à entidade referenciada (ver
 * [pucgo.joaopedrogmsilva.brainout.core.data.local.entity.PendingOpEntity]).
 * A forma textual casa com o schema exportado v4 (backticks, PK
 * autoincrement, índice `created_at`), então o `identityHash`
 * calculado durante o build bate com o esperado.
 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_ops` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `op_type` TEXT NOT NULL,
                `payload` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `idx_pending_ops_created_at` ON `pending_ops`(`created_at`)",
        )
    }
}
