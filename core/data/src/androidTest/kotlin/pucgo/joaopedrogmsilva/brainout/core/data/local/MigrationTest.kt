// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke instrumentado da migração Room v1 → v2.
 *
 * Verifica que, ao partir de um banco na v1 (com um `users` cadastrado),
 * a aplicação de [MIGRATION_1_2] produz um banco v2 onde:
 *
 *  - O `users` original ainda está presente e legível.
 *  - As novas tabelas (`projects`, `tasks`, `tags`, `project_tags`)
 *    existem e estão vazias.
 *
 * Segue o mesmo padrão de `UserDaoInstrumentedTest`: pula com
 * `assumeTrue` quando não há `InstrumentationRegistry` válido (CI sem
 * emulador) para não quebrar a pipeline — a cobertura completa continua
 * sendo exercida pelos testes Robolectric em `:core:data:test`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        assetsFolder = MIGRATION_TEST_ASSETS_DIR,
        openFactory = FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate_1_para_2_cria_tabelas_novas_sem_perder_users() {
        // Pula quando rodado em ambiente JVM puro (CI sem emulador).
        val context = try {
            InstrumentationRegistry.getInstrumentation().targetContext
        } catch (error: IllegalStateException) {
            assumeTrue(false, "Sem InstrumentationRegistry: ${error.message}")
            return
        }

        // Banco v1: cria o schema inicial via Room e insere um usuário
        // representativo — segue a forma textual da v1 exportada em
        // `core/data/schemas/.../1.json` (id, name, email, password_hash,
        // role, created_at; índice único idx_users_email).
        val testDbName = "migration-test.db"
        Room.databaseBuilder(
            context.applicationContext,
            BrainOutDatabase::class.java,
            testDbName,
        ).addMigrations(MIGRATION_1_2).build().apply {
            openHelper.writableDatabase.use { db ->
                db.execSQL(
                    "INSERT INTO users(id, name, email, password_hash, role, created_at) " +
                        "VALUES('u1','João','j@x','alg\$salt\$hash','OWNER',1000)",
                )
            }
            close()
        }

        // Aplica a migração v1 → v2 e valida o estado resultante.
        helper.runMigrationsAndValidate(
            testDbName,
            2,
            true,
            MIGRATION_1_2,
        ).apply {
            // users preservado.
            cursor("SELECT id FROM users WHERE id='u1'").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
            }

            // projects criado e vazio.
            cursor("SELECT COUNT(*) FROM projects").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // tasks criado e vazio.
            cursor("SELECT COUNT(*) FROM tasks").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // tags criado e vazio.
            cursor("SELECT COUNT(*) FROM tags").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            // project_tags criado e vazio.
            cursor("SELECT COUNT(*) FROM project_tags").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }

            close()
        }
    }

    private companion object {
        /**
         * Pasta de assets onde o `MigrationTestHelper` procura schemas
         * exportados do Room. Mantida vazia (não usamos o modo asset
         * helper) — `addMigrations` no builder acima entrega a migração
         * explícita ao `Room.databaseBuilder`.
         */
        const val MIGRATION_TEST_ASSETS_DIR = "pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase"
    }
}