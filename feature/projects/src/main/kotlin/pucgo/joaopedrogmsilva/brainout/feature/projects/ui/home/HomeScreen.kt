// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela Home do BrainOut — agora com usuário real (E1.7), badge de
// papel colorido por role, e FAB gated pelo papel (Owner habilita,
// Member abre diálogo explicativo).

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Tela Home do BrainOut.
 *
 * @param onOpenProject chamado quando o usuário toca em um card de
 *  projeto (não dispara no E1.6 porque a lista está vazia; reservado
 *  para E2.1).
 * @param onOpenSettings chamado quando o usuário seleciona a tab
 *  "Configurações" da bottom bar — dispara `navigate(settings)` no
 *  `NavHost` externo.
 * @param viewModel injetado pelo Hilt; pode ser substituído por um
 *  fake nos `@Preview`/testes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenProject: (projectId: String) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val upgradeDialogVisible by viewModel.upgradeDialogVisible.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(HomeTab.Projects) }

    val homeUser: HomeUser = when (val current = state) {
        HomeUiState.Loading, HomeUiState.SignedOut -> DefaultHomeUser
        is HomeUiState.SignedIn -> HomeUser(
            displayName = current.displayName,
            initials = current.initials,
            role = current.role,
        )
    }

    val isOwner = homeUser.role == HomeUserRole.Owner

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(user = homeUser)
        },
        bottomBar = {
            HomeBottomBar(
                currentTab = currentTab,
                onSelectTab = { selected ->
                    if (selected == HomeTab.Settings) {
                        onOpenSettings()
                    } else {
                        currentTab = selected
                    }
                }
            )
        },
        floatingActionButton = {
            HomeFloatingActionButton(
                isOwner = isOwner,
                onCreateProjectClicked = { /* criação chega em E2.1 */ },
                onMemberFabClicked = viewModel::onMemberFabClicked,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (currentTab) {
            HomeTab.Projects -> HomeProjectsContent(
                contentPadding = innerPadding,
                onOpenProject = onOpenProject,
            )
            HomeTab.Tasks -> HomeTasksPlaceholder(contentPadding = innerPadding)
            HomeTab.Settings -> {
                // A aba Settings dispara navegação externa via `onOpenSettings`.
                // Quando o callback termina, a `HomeScreen` deixa de existir na
                // pilha. Esta branch existe apenas para satisfazer a
                // exaustividade do `when`.
                HomeProjectsContent(
                    contentPadding = innerPadding,
                    onOpenProject = onOpenProject,
                )
            }
        }
    }

    if (upgradeDialogVisible) {
        UpgradeDialog(onDismiss = viewModel::dismissUpgradeDialog)
    }
}

/** Identificadores usados por testes Compose. */
object HomeTestTags {
    const val FAB: String = "home_fab"
    const val ROLE_BADGE: String = "home_role_badge"
    const val UPGRADE_DIALOG: String = "home_upgrade_dialog"
    const val UPGRADE_DIALOG_ACK: String = "home_upgrade_dialog_ack"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(user: HomeUser) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = user.initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.home_topbar_greeting, user.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val badgeLabel = when (user.role) {
                        HomeUserRole.Owner -> stringResource(id = R.string.home_role_badge_owner)
                        HomeUserRole.Member -> stringResource(id = R.string.home_role_badge_member)
                    }
                    val roleContainer: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.primary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val roleLabel: Color = when (user.role) {
                        HomeUserRole.Owner -> MaterialTheme.colorScheme.onPrimary
                        HomeUserRole.Member -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    AssistChip(
                        onClick = { /* badge é apenas decorativo */ },
                        label = {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag(HomeTestTags.ROLE_BADGE),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = roleContainer,
                            labelColor = roleLabel,
                        ),
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun HomeBottomBar(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        HomeTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null,
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
            )
        }
    }
}

private fun HomeTab.icon(): ImageVector = when (this) {
    HomeTab.Projects -> Icons.Outlined.Folder
    HomeTab.Tasks -> Icons.AutoMirrored.Outlined.Assignment
    HomeTab.Settings -> Icons.Outlined.Settings
}

@Composable
private fun HomeFloatingActionButton(
    isOwner: Boolean,
    onCreateProjectClicked: () -> Unit,
    onMemberFabClicked: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = {
            if (isOwner) {
                onCreateProjectClicked()
            } else {
                onMemberFabClicked()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(id = R.string.home_fab_create),
            )
        },
        text = {
            Text(
                text = if (isOwner) {
                    stringResource(id = R.string.home_fab_create)
                } else {
                    stringResource(id = R.string.home_fab_disabled_owner)
                },
                style = MaterialTheme.typography.labelLarge,
            )
        },
        containerColor = if (isOwner) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isOwner) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.testTag(HomeTestTags.FAB),
    )
}

@Composable
private fun UpgradeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG_ACK),
            ) {
                Text(text = stringResource(id = R.string.home_role_member_dialog_ack))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_title))
        },
        text = {
            Text(text = stringResource(id = R.string.home_role_member_dialog_body))
        },
        modifier = Modifier.testTag(HomeTestTags.UPGRADE_DIALOG),
    )
}

@Composable
private fun HomeProjectsContent(
    contentPadding: PaddingValues,
    onOpenProject: (projectId: String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.home_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        HomeEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (false) onOpenProject("placeholder")
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HomeTasksPlaceholder(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.tasks_placeholder_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = R.string.tasks_placeholder_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(onOpenSettings = {})
}
