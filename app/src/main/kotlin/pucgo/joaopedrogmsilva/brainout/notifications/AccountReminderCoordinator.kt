// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.session.AccountReminderSnapshot
import pucgo.joaopedrogmsilva.brainout.core.data.session.SessionStore
import pucgo.joaopedrogmsilva.brainout.core.domain.error.OwnershipException
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.DeadlineNotificationScheduler
import pucgo.joaopedrogmsilva.brainout.core.domain.notification.reconcileTaskReminder
import javax.inject.Inject
import javax.inject.Singleton

/** Reconcilia apenas mudanças de sessão, inclusive ao reiniciar o processo. */
@Singleton
class AccountReminderCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val session: SessionStore,
        private val snapshot: AccountReminderSnapshot,
        private val scheduler: DeadlineNotificationScheduler,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun start() {
            startIn(scope)
        }

        internal fun startIn(callerScope: CoroutineScope): Job =
            callerScope.launch {
                session.observeUserId().distinctUntilChanged().collectLatest { owner -> reconcileWithRetry(owner) }
            }

        private suspend fun reconcileWithRetry(owner: String?) {
            repeat(MAX_ATTEMPTS) { attempt ->
                try {
                    reconcile(owner)
                    return
                } catch (error: CancellationException) {
                    throw error
                } catch (error: OwnershipException) {
                    return
                } catch (error: Exception) {
                    Log.e("BrainOut:Reminders", "Falha na reconciliação da sessão", error)
                    if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MILLIS * (attempt + 1))
                }
            }
        }

        private suspend fun reconcile(owner: String?) {
            WorkManager
                .getInstance(context)
                .cancelAllWorkByTag(WorkManagerDeadlineScheduler.DEADLINE_TAG)
                .result
                .get()
            NotificationManagerCompat.from(context).cancelAll()
            if (owner != null) {
                session.withUser(owner) {
                    snapshot.tasks(owner).forEach { task ->
                        scheduler.reconcileTaskReminder(task.id, task.status, task.dueDate)
                    }
                }
            }
        }

        private companion object {
            const val MAX_ATTEMPTS = 3
            const val RETRY_DELAY_MILLIS = 500L
        }
    }
