// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Tela Home do BrainOut — Scaffold com TopAppBar, lista vazia, FAB
// desabilitado e bottom bar com 3 tabs (Projetos / Tarefas / Configurações).
// Marco E1.3 — esqueleto navegável.

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pucgo.joaopedrogmsilva.brainout.feature.projects.R

/**
 * Tela Home do BrainOut.
 *
 * @param user identidade do usuário (placeholder enquanto não há sessão real).
 * @param onOpenProject chamado quando o usuário toca em um card de projeto
 *  (não dispara no E1.3 porque a lista está vazia; reservado para E2.1).
 * @param onOpenSettings chamado quando o usuário seleciona a tab
 *  "Configurações" da bottom bar — dispara `navigate(settings)` no
 *  `NavHost` externo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: HomeUser = DefaultHomeUser,
    onOpenProject: (projectId: String) -> Unit = {},
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTab by rememberSaveable { mutableStateOf(HomeTab.Projects) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(user = user)
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
            ExtendedFloatingActionButton(
                onClick = { /* desabilitado no esqueleto E1.3 — CRUD chega em E2.1 */ },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(id = R.string.home_fab_create)
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.home_fab_create),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (currentTab) {
            HomeTab.Projects -> HomeProjectsContent(
                contentPadding = innerPadding,
                onOpenProject = onOpenProject
            )
            HomeTab.Tasks -> HomeTasksPlaceholder(contentPadding = innerPadding)
            HomeTab.Settings -> {
                // A aba Settings dispara navegação externa via `onOpenSettings`.
                // Quando o callback termina, a `HomeScreen` deixa de existir na
                // pilha. Esta branch existe apenas para satisfazer a
                // exaustividade do `when`.
                HomeProjectsContent(
                    contentPadding = innerPadding,
                    onOpenProject = onOpenProject
                )
            }
        }
    }
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
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.initials,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column {
                    Text(
                        text = stringResource(id = R.string.home_topbar_greeting, user.displayName),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    val badgeLabel = when (user.role) {
                        HomeUserRole.Owner -> stringResource(id = R.string.home_role_badge_owner)
                        HomeUserRole.Member -> stringResource(id = R.string.home_role_badge_member)
                    }
                    AssistChip(
                        onClick = { /* badge é apenas decorativo no esqueleto E1.3 */ },
                        label = {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun HomeBottomBar(
    currentTab: HomeTab,
    onSelectTab: (HomeTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        HomeTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon(),
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(id = tab.labelRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                alwaysShowLabel = true
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
private fun HomeProjectsContent(
    contentPadding: PaddingValues,
    onOpenProject: (projectId: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        HomeEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // O callback `onOpenProject` será consumido pelos cards reais quando
        // o CRUD E2.1 entrar; enquanto isso usamos `if (false)` para manter a
        // referência sem disparar navegação no esqueleto E1.3.
        if (false) onOpenProject("placeholder")
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.tasks_placeholder_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.tasks_placeholder_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
