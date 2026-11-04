// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import pucgo.joaopedrogmsilva.brainout.core.data.local.dao.UserDao
import pucgo.joaopedrogmsilva.brainout.core.data.local.entity.UserEntity

/** Aviso persistente até confirmação; os antigos dados de login são apagados ao confirmar. */
@Composable
internal fun LegacyAccountNotice(dao: UserDao) {
    var migrated by remember { mutableStateOf(emptyList<UserEntity>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(dao) { migrated = dao.legacyLogins() }
    if (migrated.isEmpty()) return
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.account_migration_title)) },
        text = {
            Text(
                stringResource(R.string.account_migration_message) + "\n\n" +
                    migrated.joinToString("\n") { "${it.legacyLogin} → ${it.username}" },
                Modifier.verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    dao.dismissLegacyLogins()
                    migrated = emptyList()
                }
            }) {
                Text(stringResource(R.string.account_migration_ack))
            }
        },
    )
}
