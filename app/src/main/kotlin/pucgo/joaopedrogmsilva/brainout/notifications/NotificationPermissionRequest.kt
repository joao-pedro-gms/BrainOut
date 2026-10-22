// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.R

private const val TAG = "BrainOut:NotificationPermission"

/**
 * Pede a permissão `POST_NOTIFICATIONS` uma única vez (E3.6).
 *
 * Fica **dentro da Home** (e não na raiz da Activity) porque o pedido
 * do sistema precisa de contexto: em instalação limpa, a primeira
 * composição da árvore é a tela de splash, e o diálogo aparecia
 * antes do login, sem o usuário saber do que se trata (DEF-45).
 *
 * Regras:
 * - Só dispara no Android 13+ (API < 33 não exige pedido runtime).
 * - Pergunta ao [NotificationPermissionStore] se já pedimos; se sim,
 *   não pede novamente (não insistir após negativa).
 * - Negativa → toast explicativo com o texto do E3.6.
 */
@Composable
internal fun NotificationPermissionRequest(permissionStore: NotificationPermissionStore) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val deniedToast = stringResource(R.string.deadline_permission_denied_toast)
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            Log.d(TAG, "Resultado da permissão POST_NOTIFICATIONS: concedida=$granted")
            if (!granted) {
                Toast
                    .makeText(
                        context,
                        deniedToast,
                        Toast.LENGTH_LONG,
                    ).show()
            }
            (context as? ComponentActivity)?.lifecycleScope?.launch {
                permissionStore.markAsked()
            }
        }

    LaunchedEffect(Unit) {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (shouldRequestNotificationPermission(permissionStore, alreadyGranted)) {
            Log.d(TAG, "Solicitando permissão POST_NOTIFICATIONS...")
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (alreadyGranted) {
            Log.d(TAG, "Permissão POST_NOTIFICATIONS já concedida previamente")
        } else {
            Log.d(TAG, "Permissão POST_NOTIFICATIONS negada anteriormente; não solicitando novamente")
        }
    }
}

/**
 * Decide se o pedido da permissão deve acontecer agora.
 *
 * - Já concedida → não pede.
 * - Já pedida antes (negativa) → não insiste.
 * - Caso contrário → pede.
 */
internal suspend fun shouldRequestNotificationPermission(
    permissionStore: NotificationPermissionStore,
    isPermissionGranted: Boolean,
): Boolean = !isPermissionGranted && !permissionStore.wasAsked()
