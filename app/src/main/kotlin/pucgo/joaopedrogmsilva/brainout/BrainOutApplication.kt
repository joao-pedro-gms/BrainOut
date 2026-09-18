// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application raiz do BrainOut.
 *
 * Anotada com [HiltAndroidApp] para inicializar o grafo de injeção de
 * dependência (Hilt) usado por todos os módulos do app.
 */
@HiltAndroidApp
class BrainOutApplication : Application()
