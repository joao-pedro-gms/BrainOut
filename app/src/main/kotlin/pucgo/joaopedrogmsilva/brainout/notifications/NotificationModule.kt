// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler

/**
 * Módulo Hilt das notificações (marco E3.6): liga a porta de domínio
 * [DeadlineNotificationScheduler] à implementação WorkManager.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    abstract fun bindDeadlineScheduler(
        impl: WorkManagerDeadlineScheduler,
    ): DeadlineNotificationScheduler
}
