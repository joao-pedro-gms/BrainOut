// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Abre uma instalação v4 real pelo Room v5 e verifica os dados após a migração. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class LocalUpgradeTest {
    @Test
    fun `cadeia completa da v1 ate a v6 preserva o cadastro original`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "upgrade-1-5.db"
            context.deleteDatabase(name)
            createLegacyDatabase(context, name, legacyDefault = false, version = 1)
            val database =
                Room
                    .databaseBuilder(context, BrainOutDatabase::class.java, name)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertThat(database.userDao().findById("u1")?.passwordHash).isEqualTo("hash-legado")
                assertThat(database.userDao().findById("u1")?.username).isEqualTo("conta_1")
                assertThat(database.openHelper.writableDatabase.version).isEqualTo(6)
                database.openHelper.writableDatabase
                    .query(
                        "SELECT name FROM sqlite_master WHERE name='pending_ops'",
                    ).use {
                        assertThat(it.moveToFirst()).isFalse()
                    }
            } finally {
                database.close()
                context.deleteDatabase(name)
            }
        }

    @Test
    fun `upgrade da v4 preserva todos os dados locais e remove apenas a fila`() = verifyUpgrade(legacyDefault = false)

    @Test
    fun `upgrade repara default legado sem perder tarefas ou prazos`() = verifyUpgrade(legacyDefault = true)

    private fun verifyUpgrade(legacyDefault: Boolean) =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "upgrade-local.db"
            context.deleteDatabase(name)
            createLegacyDatabase(context, name, legacyDefault)
            val database =
                Room
                    .databaseBuilder(context, BrainOutDatabase::class.java, name)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertThat(database.userDao().findById("u1")?.passwordHash).isEqualTo("hash-legado")
                assertThat(database.userDao().findById("u1")?.username).isEqualTo("conta_1")
                assertThat(database.projectDao().findById("p1")?.description).isEqualTo("Descrição")
                val task = database.taskDao().findById("t1")!!
                assertThat(task.title).isEqualTo("Tarefa legada")
                assertThat(task.assigneeId).isEqualTo("u1")
                assertThat(task.dueDate?.toEpochMilli()).isEqualTo(5000L)
                assertThat(
                    database
                        .taskDao()
                        .findById("t2")
                        ?.completedAt
                        ?.toEpochMilli(),
                ).isEqualTo(4000L)
                assertThat(database.tagDao().findById("g1")?.color).isEqualTo("#123456")
                val sql = database.openHelper.writableDatabase
                assertThat(sql.version).isEqualTo(6)
                sql.query("SELECT tag_id FROM project_tags WHERE project_id='p1'").use {
                    assertThat(it.moveToFirst()).isTrue()
                    assertThat(it.getString(0)).isEqualTo("g1")
                }
                sql.query("SELECT name FROM sqlite_master WHERE name='pending_ops'").use {
                    assertThat(it.moveToFirst()).isFalse()
                }
                sql.query("PRAGMA foreign_key_check").use {
                    assertThat(it.moveToFirst()).isFalse()
                }
            } finally {
                database.close()
                context.deleteDatabase(name)
            }
        }

    private fun createLegacyDatabase(
        context: Context,
        name: String,
        legacyDefault: Boolean,
        version: Int = 4,
    ) {
        val schemaFile =
            generateSequence(File(checkNotNull(System.getProperty("user.dir")))) { it.parentFile }
                .map { File(it, "core/data/schemas/$SCHEMA_DIRECTORY/$version.json") }
                .first { it.isFile }
        val schema = JSONObject(schemaFile.readText()).getJSONObject("database")
        context.getDatabasePath(name).parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            val entities = schema.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                val table = checkNotNull(entity.getString("tableName"))
                val createSql = entity.getString("createSql").replace("\${TABLE_NAME}", table)
                db.execSQL(if (table == "tasks" && legacyDefault) createSql.replace("DEFAULT NULL", "") else createSql)
                val indices = entity.getJSONArray("indices")
                for (item in 0 until indices.length()) {
                    db.execSQL(indices.getJSONObject(item).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
            db.execSQL("INSERT INTO users VALUES('u1','João','j@x','hash-legado','OWNER',1000)")
            if (version == 1) {
                db.version = version
                return@use
            }
            db.execSQL("INSERT INTO projects VALUES('p1','u1','Projeto','Descrição',1000,0)")
            db.execSQL("INSERT INTO tasks VALUES('t1','p1','Tarefa legada',2,'TODO','u1',5000,2000,NULL)")
            db.execSQL("INSERT INTO tasks VALUES('t2','p1','Concluída',4,'DONE',NULL,NULL,3000,4000)")
            db.execSQL("INSERT INTO tags VALUES('g1','u1','Tag','#123456',1000)")
            db.execSQL("INSERT INTO project_tags VALUES('p1','g1')")
            db.execSQL("INSERT INTO pending_ops VALUES(1,'TASK','t1','CREATE','{}',1000,0)")
            db.version = 4
        }
    }

    private companion object {
        const val SCHEMA_DIRECTORY = "pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase"
    }
}
