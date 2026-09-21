// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import pucgo.joaopedrogmsilva.brainout.notifications.WorkManagerDeadlineScheduler
import javax.inject.Inject

/**
 * Application raiz do BrainOut.
 *
 * Anotada com [HiltAndroidApp] para inicializar o grafo de injeção de
 * dependência (Hilt) usado por todos os módulos do app.
 *
 * No `onCreate()` cria o canal de notificações `brainout_deadlines`
 * (importância HIGH) usado pelos lembretes de prazo (E3.6). Também
 * implementa [Configuration.Provider] para que os `@HiltWorker`
 * (E3.3 sincronização e E3.6 lembretes) recebam dependências via
 * construtor — desabilitamos o inicializador default do WorkManager
 * no `AndroidManifest.xml` e fornecemos a [HiltWorkerFactory] aqui.
 */
@HiltAndroidApp
class BrainOutApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        WorkManagerDeadlineScheduler.ensureChannel(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
