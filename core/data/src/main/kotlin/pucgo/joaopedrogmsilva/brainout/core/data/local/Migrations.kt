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
