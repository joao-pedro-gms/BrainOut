// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import android.database.Cursor
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes instrumentados das migrações Room do [BrainOutDatabase]
 * (v1 → v2 → v3 → v4).
 *
 * Estratégia: [MigrationTestHelper.createDatabase] materializa o schema
 * histórico da versão inicial usando o JSON exportado em
 * `core/data/schemas/` (exposto como asset do `androidTest`), semeia
 * dados representativos via SQL cru e só então aplica a migração com
 * [MigrationTestHelper.runMigrationsAndValidate], que valida o schema
 * resultante contra o JSON da versão alvo — se a migração divergir do
 * schema exportado (coluna, índice ou `identityHash`), o teste falha.
 *
 * O ponto central de cada caso não é só "a migração roda", e sim
 * **os dados anteriores sobreviveram**: as linhas semeadas na versão
 * inicial são relidas após a migração.
 *
 * Não há `assumeTrue` aqui: um teste instrumentado só roda com
 * dispositivo/emulador de qualquer forma, e o auto-pulo transformava
 * uma lacuna real de cobertura (2→3 e 3→4 sem nenhum teste) em um
 * falso verde.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            assetsFolder = MIGRATION_TEST_ASSETS_DIR,
            openFactory = FrameworkSQLiteOpenHelperFactory(),
        )

    /**
     * v1 → v2 ([MIGRATION_1_2]): as tabelas `projects`, `tasks`, `tags` e
     * `project_tags` nascem, e a tabela `users` da v1 é preservada.
     */
    @Test
    fun migrate_1_para_2_cria_tabelas_novas_sem_perder_users() {
        val dbName = "migration-1-2.db"
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO users(id, name, email, password_hash, role, created_at) " +
                    "VALUES('u1','João','j@x','alg\$salt\$hash','OWNER',1000)",
            )
        }

        helper.runMigrationsAndValidate(dbName, 2, true, MIGRATION_1_2).use { db ->
            // users preservado.
            db.cursor("SELECT id, name, role FROM users WHERE id='u1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("u1")
                assertThat(cursor.getString(1)).isEqualTo("João")
                assertThat(cursor.getString(2)).isEqualTo("OWNER")
            }

            // projects criado e vazio.
            db.cursor("SELECT COUNT(*) FROM projects").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // tasks criado e vazio.
            db.cursor("SELECT COUNT(*) FROM tasks").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // tags criado e vazio.
            db.cursor("SELECT COUNT(*) FROM tags").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // project_tags criado e vazio.
            db.cursor("SELECT COUNT(*) FROM project_tags").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    /**
     * v2 → v3 ([MIGRATION_2_3]): `tasks` ganha `completed_at INTEGER`
     * via `ALTER TABLE ADD COLUMN`.
     *
     * A migração é não destrutiva e não tem `DEFAULT`: tarefas que já
     * existiam na v2 devem continuar íntegras com `completed_at IS
     * NULL`. Este caso semeia projeto + tarefa na v2 e verifica que a
     * tarefa sobrevive com todos os campos originais e que a coluna nova
     * aceita valor para as tarefas concluídas depois do upgrade.
     */
    @Test
    fun migrate_2_para_3_adiciona_completed_at_preservando_tarefas() {
        val dbName = "migration-2-3.db"
        helper.createDatabase(dbName, 2).use { db ->
            seedProjectAndTask(db)
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            // A tarefa pré-migração sobreviveu intacta e, como a coluna
            // nova não tem DEFAULT, ficou com completed_at nulo.
            db
                .cursor(
                    "SELECT project_id, title, priority_code, status, created_at, completed_at " +
                        "FROM tasks WHERE id='t1'",
                ).use { cursor ->
                    assertThat(cursor.moveToFirst()).isTrue()
                    assertThat(cursor.getString(0)).isEqualTo("p1")
                    assertThat(cursor.getString(1)).isEqualTo("Tarefa legada")
                    assertThat(cursor.getInt(2)).isEqualTo(2)
                    assertThat(cursor.getString(3)).isEqualTo("TODO")
                    assertThat(cursor.getLong(4)).isEqualTo(2000L)
                    assertThat(cursor.isNull(5)).isTrue()
                }

            // O projeto pai também sobreviveu (cascade não disparou).
            db.cursor("SELECT COUNT(*) FROM projects WHERE id='p1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }

            // A coluna nova existe e é gravável: tarefas concluídas
            // depois do upgrade registram o instante de conclusão.
            db.execSQL("UPDATE tasks SET status='DONE', completed_at=3000 WHERE id='t1'")
            db.cursor("SELECT completed_at FROM tasks WHERE id='t1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getLong(0)).isEqualTo(3000L)
            }
        }
    }

    /**
     * v3 → v4 ([MIGRATION_3_4]): nasce a fila de sincronização
     * `pending_ops`.
     *
     * Nenhum dado existente é tocado — a migração só cria tabela e
     * índice. O caso semeia usuário, projeto, tarefa (com
     * `completed_at` já preenchido, para garantir que a coluna
     * introduzida na v3 atravesse a migração seguinte) e um registro de
     * `project_tags`, e verifica que tudo sobrevive enquanto a nova
     * tabela já aceita a fila offline.
     */
    @Test
    fun migrate_3_para_4_cria_pending_ops_preservando_dados_existentes() {
        val dbName = "migration-3-4.db"
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO users(id, name, email, password_hash, role, created_at) " +
                    "VALUES('u1','João','j@x','alg\$salt\$hash','OWNER',1000)",
            )
            db.execSQL(
                "INSERT INTO tags(id, owner_id, name, color, created_at) " +
                    "VALUES('g1','u1','urgente','#FF0000',1500)",
            )
            seedProjectAndTask(db)
            db.execSQL("UPDATE tasks SET status='DONE', completed_at=3000 WHERE id='t1'")
            db.execSQL("INSERT INTO project_tags(project_id, tag_id) VALUES('p1','g1')")
        }

        helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4).use { db ->
            // pending_ops criada e vazia.
            db.cursor("SELECT COUNT(*) FROM pending_ops").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // users preservado.
            db.cursor("SELECT COUNT(*) FROM users WHERE id='u1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }

            // tasks preservada, incluindo a coluna da v3.
            db.cursor("SELECT title, status, completed_at FROM tasks WHERE id='t1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Tarefa legada")
                assertThat(cursor.getString(1)).isEqualTo("DONE")
                assertThat(cursor.getLong(2)).isEqualTo(3000L)
            }

            // project_tags preservada.
            db.cursor("SELECT COUNT(*) FROM project_tags").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(1)
            }

            // A fila offline aceita a primeira operação (E3.3): o id é
            // autoincrement e o índice por created_at existe.
            db.execSQL(
                "INSERT INTO pending_ops(entity_type, entity_id, op_type, payload, created_at, attempts) " +
                    "VALUES('TASK','t1','UPDATE','{\"title\":\"x\"}',4000,0)",
            )
            db.cursor("SELECT entity_type, entity_id, attempts FROM pending_ops").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("TASK")
                assertThat(cursor.getString(1)).isEqualTo("t1")
                assertThat(cursor.getInt(2)).isEqualTo(0)
            }
        }
    }

    /**
     * Percorre as três migrações em sequência sobre um banco v1 real,
     * provando que a cadeia 1→2→3→4 é aplicável de ponta a ponta (o
     * caso que o usuário realmente enfrenta ao atualizar o app) e que
     * os dados do usuário original chegam intactos na v4.
     */
    @Test
    fun migracoes_encadeadas_1_a_4_preservam_o_usuario_original() {
        val dbName = "migration-1-4.db"
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO users(id, name, email, password_hash, role, created_at) " +
                    "VALUES('u1','João','j@x','alg\$salt\$hash','OWNER',1000)",
            )
        }

        helper
            .runMigrationsAndValidate(
                dbName,
                4,
                true,
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
            ).use { db ->
                db.cursor("SELECT id, email FROM users WHERE id='u1'").use { cursor ->
                    assertThat(cursor.moveToFirst()).isTrue()
                    assertThat(cursor.getString(0)).isEqualTo("u1")
                    assertThat(cursor.getString(1)).isEqualTo("j@x")
                }
                // completed_at (v3) e pending_ops (v4) presentes.
                db.cursor("SELECT COUNT(*) FROM pending_ops").use { cursor ->
                    assertThat(cursor.moveToFirst()).isTrue()
                    assertThat(cursor.getInt(0)).isEqualTo(0)
                }
                db.cursor("SELECT completed_at FROM tasks LIMIT 1").use { cursor ->
                    assertThat(cursor.moveToFirst()).isFalse()
                }
            }
    }

    /**
     * Semeia o par `projects`/`tasks` válido nas versões v2 e v3 — a
     * FK `tasks.project_id → projects.id` exige o pai presente, e
     * `tasks` só existe a partir da v2.
     */
    private fun seedProjectAndTask(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO projects(id, owner_id, name, description, created_at, is_completed) " +
                "VALUES('p1','u1','Projeto legado',NULL,1000,0)",
        )
        db.execSQL(
            "INSERT INTO tasks(id, project_id, title, priority_code, status, assignee_id, due_date, created_at) " +
                "VALUES('t1','p1','Tarefa legada',2,'TODO',NULL,NULL,2000)",
        )
    }

    private companion object {
        /**
         * Pasta de assets onde o [MigrationTestHelper] procura os schemas
         * exportados do Room — precisa casar com o nome gerado pelo KSP
         * (`<namespace>.<classe do banco>`) e com `assets.srcDir`
         * apontando para `core/data/schemas` no `androidTest`.
         */
        const val MIGRATION_TEST_ASSETS_DIR = "pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase"
    }
}

/**
 * Executa [query] e devolve o [Cursor] para leitura — o chamador fecha.
 *
 * `SupportSQLiteDatabase` expõe `query()`, o caminho oficial, que já
 * devolve o cursor na primeira linha. Este atalho existe só para o teste
 * ler contagens e poucos valores sem repetir `query(...)` em cada bloco;
 * o `.use` nos call sites continua sendo quem fecha o cursor.
 */
private fun SupportSQLiteDatabase.cursor(query: String): Cursor = query(query)
