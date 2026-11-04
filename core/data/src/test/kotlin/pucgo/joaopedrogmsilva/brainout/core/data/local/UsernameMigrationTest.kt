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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class UsernameMigrationTest {
    @Test
    fun `migration 5 para 6 preserva hashes vinculos e desempata nomes por UUID`() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "username-migration.db"
            context.deleteDatabase(name)
            val file = context.getDatabasePath(name)
            file.parentFile!!.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { sql ->
                val schema = JSONObject(File("schemas/$SCHEMA_DIRECTORY/5.json").readText()).getJSONObject("database")
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    sql.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.getJSONArray("indices")
                    for (j in 0 until indices.length()) {
                        sql.execSQL(
                            indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table),
                        )
                    }
                }
                sql.execSQL("INSERT INTO users VALUES('b','Mesmo Nome','b@exemplo.com','hash-b','MEMBER',1000)")
                sql.execSQL("INSERT INTO users VALUES('a','Mesmo Nome','a@exemplo.com','hash-a','OWNER',1000)")
                sql.execSQL("INSERT INTO projects VALUES('p','a','Projeto',NULL,1000,0)")
                sql.execSQL("INSERT INTO tasks VALUES('t','p','Tarefa',2,'TODO','b',NULL,1000,NULL)")
                sql.execSQL("INSERT INTO tags VALUES('g','a','Tag','#123456',1000)")
                sql.execSQL("INSERT INTO project_tags VALUES('p','g')")
                sql.version = 5
            }
            val db =
                Room
                    .databaseBuilder(
                        context,
                        BrainOutDatabase::class.java,
                        name,
                    ).addMigrations(MIGRATION_5_6)
                    .allowMainThreadQueries()
                    .build()
            try {
                assertThat(db.userDao().findById("a")?.username).isEqualTo("conta_1")
                assertThat(db.userDao().findById("b")?.username).isEqualTo("conta_2")
                assertThat(db.userDao().findById("b")?.passwordHash).isEqualTo("hash-b")
                assertThat(db.userDao().legacyLogins()).hasSize(2)
                assertThat(db.taskDao().findById("t")?.assigneeId).isEqualTo("b")
                assertThat(db.backupDao().getAllProjectTags()).hasSize(1)
                db.openHelper.writableDatabase
                    .query(
                        "PRAGMA foreign_key_check",
                    ).use { assertThat(it.moveToFirst()).isFalse() }
                db.userDao().dismissLegacyLogins()
                assertThat(db.userDao().legacyLogins()).isEmpty()
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }

    private companion object {
        const val SCHEMA_DIRECTORY = "pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase"
    }
}
