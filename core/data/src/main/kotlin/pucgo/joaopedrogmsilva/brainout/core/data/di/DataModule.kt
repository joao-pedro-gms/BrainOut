// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.core.data.BuildConfig
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_1_2
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_2_3
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.repository.ProjectRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TagRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.remote.RemoteDataSource
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository

/**
 * Módulo Hilt do `:core:data`.
 *
 * Fornece:
 * - Instância singleton do [BrainOutDatabase] construída via
 *   [Room.databaseBuilder] (não destrutiva — exige migration para
 *   upgrades de schema).
 * - DAOs individuais expostos a partir do database.
 * - Bind das implementações de `UserRepository` e `PasswordHasher`
 *   para as portas declaradas em `:core:domain`.
 * - Singleton de [SessionStore] para persistir o id do usuário
 *   autenticado em `DataStore` (E1.8).
 *
 * Consumido por `@HiltAndroidApp` em `:app` (ver
 * `BrainOutApplication`).
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideBrainOutDatabase(
        @ApplicationContext context: Context,
    ): BrainOutDatabase = Room.databaseBuilder(
        context,
        BrainOutDatabase::class.java,
        BrainOutDatabase.DATABASE_NAME,
    )
        // Migration v1 → v2: adiciona `projects`, `tasks`, `tags` e
        // `project_tags` sem destruir `users`. Mantém os cadastros
        // existentes do E1.5.
        // Migration v2 → v3 (RN03 — E2.5): adiciona a coluna
        // `tasks.completed_at` (`ALTER TABLE ADD COLUMN INTEGER
        // nullable`) sem destruir dados. Tarefas preexistentes
        // ficam com `completed_at IS NULL`.
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()

    @Provides
    fun provideUserDao(database: BrainOutDatabase): UserDao = database.userDao()

    @Provides
    fun provideProjectDao(database: BrainOutDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideTaskDao(database: BrainOutDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideTagDao(database: BrainOutDatabase): TagDao = database.tagDao()

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        tagDao: TagDao,
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, tagDao)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        projectDao: ProjectDao,
    ): TaskRepository = TaskRepositoryImpl(taskDao, projectDao)

    @Provides
    @Singleton
    fun provideTagRepository(tagDao: TagDao): TagRepository = TagRepositoryImpl(tagDao)

    @Provides
    @Singleton
    fun providePasswordHasher(impl: PasswordHasherImpl): PasswordHasher = impl

    @Provides
    @Singleton
    fun provideAuthDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.authDataStore

    @Provides
    @Singleton
    fun provideSessionStore(
        dataStore: DataStore<Preferences>,
    ): SessionStore = SessionStore(dataStore = dataStore)

    /**
     * Fonte de dados remota (E3.2). A URL base vem do `BuildConfig`
     * deste módulo, injetada por flavor (debug: backend-stub no host do
     * emulador; release: placeholder documentado).
     */
    @Provides
    @Singleton
    fun provideRemoteDataSource(): RemoteDataSource =
        RemoteDataSource(baseUrl = BuildConfig.BASE_URL)
}
