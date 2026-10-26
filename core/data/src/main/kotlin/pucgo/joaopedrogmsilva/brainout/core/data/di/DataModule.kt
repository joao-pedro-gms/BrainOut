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
import pucgo.joaopedrogmsilva.brainout.core.data.backup.BackupRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.LocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_1_2
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_2_3
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_3_4
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_4_5
import pucgo.joaopedrogmsilva.brainout.core.data.local.MIGRATION_5_6
import pucgo.joaopedrogmsilva.brainout.core.data.local.RoomLocalTransaction
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.ProjectDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TagDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.TaskDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ListingPreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.preferences.ThemePreferencesRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.HolidayRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.ProjectRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TagRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.TaskRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountAccess
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.data.session.authDataStore
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ActiveUserPort
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.BackupRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.HolidayRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ListingPreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ProjectRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TagRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.TaskRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.ThemePreferencesRepository
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.UserRepository
import java.time.Clock
import javax.inject.Singleton

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
    ): BrainOutDatabase =
        Room
            .databaseBuilder(
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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
    fun provideLocalTransaction(impl: RoomLocalTransaction): LocalTransaction = impl

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

    @Provides
    @Singleton
    fun provideActiveUserPort(impl: SessionActiveUserPort): ActiveUserPort = impl

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        tagDao: TagDao,
        access: AccountAccess,
    ): ProjectRepository = ProjectRepositoryImpl(projectDao, tagDao, access)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        projectDao: ProjectDao,
        access: AccountAccess,
    ): TaskRepository = TaskRepositoryImpl(taskDao, projectDao, access)

    @Provides
    @Singleton
    fun provideTagRepository(
        tagDao: TagDao,
        access: AccountAccess,
        projectDao: ProjectDao,
    ): TagRepository = TagRepositoryImpl(tagDao, access, projectDao)

    /**
     * Bind de [ListingPreferencesRepository] para a implementação
     * DataStore (E2.6 do ROADMAP). Reaproveita o mesmo
     * [DataStore]<[Preferences]> da sessão (injetado por
     * [provideAuthDataStore]) para evitar um arquivo de preferências
     * adicional — `SessionStore.clearAll()` remove a sessão e a flag de
     * notificação, mas preserva intencionalmente estas chaves.
     */
    @Provides
    @Singleton
    fun provideListingPreferencesRepository(dataStore: DataStore<Preferences>): ListingPreferencesRepository =
        ListingPreferencesRepositoryImpl(dataStore = dataStore)

    /**
     * Bind de [ThemePreferencesRepository] para a implementação
     * DataStore (NB-06, E4.5 — escolha manual System/Light/Dark).
     * Mesmo `DataStore` da sessão: a chave `theme_mode` é global e o
     * `SessionStore.clearAll()` de logout não a remove.
     */
    @Provides
    @Singleton
    fun provideThemePreferencesRepository(dataStore: DataStore<Preferences>): ThemePreferencesRepository =
        ThemePreferencesRepositoryImpl(dataStore = dataStore)

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
    fun provideSessionStore(dataStore: DataStore<Preferences>): SessionStore = SessionStore(dataStore = dataStore)

    @Provides
    @Singleton
    fun provideHolidayRepository(impl: HolidayRepositoryImpl): HolidayRepository = impl

    /**
     * Bind de [BackupRepository] para a implementação Room do backup
     * local exportável (issue #214 — LOCAL-03). A implementação usa
     * Room, DataStore e o hasher de senhas — todos já Provided
     * acima.
     */
    @Provides
    @Singleton
    fun provideBackupRepository(impl: BackupRepositoryImpl): BackupRepository = impl

    /**
     * Relógio do app (UTC).
     *
     * O Painel calcula o início da semana corrente a partir dele — e a
     * virada de semana precisa ser observável. Com `Instant.now()`
     * dentro do ViewModel, o cálculo ficava congelado na assinatura e
     * não tinha como ser testado.
     */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
