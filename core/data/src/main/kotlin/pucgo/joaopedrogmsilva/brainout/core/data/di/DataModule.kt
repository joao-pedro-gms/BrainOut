// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
@file:Suppress("TooManyFunctions") // E3.5 adiciona providers de feriados.
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
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_3_4
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.PendingOpDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ListingPreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.remote.HolidayRemoteDataSource
import pucgo.joaopedrogmsilva.brainout.core.data.remote.RemoteDataSource
import pucgo.joaopedrogmsilva.brainout.core.data.repository.HolidayRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.ProjectRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TagRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.data.sync.BrainOutSyncDispatcher
import pucgo.joaopedrogmsilva.brainout.core.data.sync.SyncDispatcher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
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
        // Migration v3 → v4 (E3.3): cria a tabela `pending_ops` da
        // fila de sincronização offline — não destrutiva.
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    fun providePendingOpDao(database: BrainOutDatabase): PendingOpDao = database.pendingOpDao()

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        tagDao: TagDao,
        pendingOpDao: PendingOpDao,
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, tagDao, pendingOpDao)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        projectDao: ProjectDao,
        pendingOpDao: PendingOpDao,
    ): TaskRepository = TaskRepositoryImpl(taskDao, projectDao, pendingOpDao)

    @Provides
    @Singleton
    fun provideTagRepository(
        tagDao: TagDao,
        pendingOpDao: PendingOpDao,
    ): TagRepository = TagRepositoryImpl(tagDao, pendingOpDao)

    /**
     * Bind de [ListingPreferencesRepository] para a implementação
     * DataStore (E2.6 do ROADMAP). Reaproveita o mesmo
     * [DataStore]<[Preferences]> da sessão (injetado por
     * [provideAuthDataStore]) para evitar um arquivo de preferências
     * adicional — `SessionStore.clear()` já é granular e preserva
     * intencionalmente estas chaves.
     */
    @Provides
    @Singleton
    fun provideListingPreferencesRepository(
        dataStore: DataStore<Preferences>,
    ): ListingPreferencesRepository = ListingPreferencesRepositoryImpl(dataStore = dataStore)

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

    /**
     * Despachante de sincronização (E3.3): envolve o cliente remoto
     * com a tradução de erros IOException/5xx → retriable, 4xx →
     * permanente. O `SyncWorker` (`:app`) drena a fila `pending_ops`
     * através dele.
     */
    @Provides
    @Singleton
    fun provideSyncDispatcher(remote: RemoteDataSource): SyncDispatcher =
        BrainOutSyncDispatcher(remote)

    /**
     * Fonte de dados remota do serviço público de feriados nacionais
     * (E3.5). A URL vem do `BuildConfig.HOLIDAYS_BASE_URL` injetado
     * por flavor (BrasilAPI — sem autenticação).
     */
    @Provides
    @Singleton
    fun provideHolidayRemoteDataSource(): HolidayRemoteDataSource =
        HolidayRemoteDataSource(baseUrl = BuildConfig.HOLIDAYS_BASE_URL)

    /** Bind de [HolidayRepository] para a implementação HTTP (E3.5). */
    @Provides
    @Singleton
    fun provideHolidayRepository(
        remote: HolidayRemoteDataSource,
    ): HolidayRepository = HolidayRepositoryImpl(remote = remote)
}
