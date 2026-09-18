// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pucgo.joaopedrogmsilva.brainout.core.data.local.BrainOutDatabase
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.repository.UserRepositoryImpl
import pucgo.joaopedrogmsilva.brainout.core.data.security.PasswordHasherImpl
import pucgo.joaopedrogmsilva.brainout.core.domain.repository.PasswordHasher
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
        // Schema inicial — sem migrations ainda; uma nova versão do
        // banco exigirá Migration explícita para preservar dados
        // locais.
        .build()

    @Provides
    fun provideUserDao(database: BrainOutDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository = impl

    @Provides
    @Singleton
    fun providePasswordHasher(impl: PasswordHasherImpl): PasswordHasher = impl
}
