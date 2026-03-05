package org.oltionzefi.splinge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.painterResource
import org.oltionzefi.splinge.model.Group
import splinge.composeapp.generated.resources.Res
import splinge.composeapp.generated.resources.ic_splinge_logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
    groups: List<Group>,
    onGroupClick: (Group) -> Unit,
    onAddGroup: () -> Unit,
    onSettings: () -> Unit,
    onOpenPayme: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(Res.drawable.ic_splinge_logo),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Splinge",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Settings")
                        }
                        if (showMenu) {
                            Popup(
                                alignment = Alignment.TopEnd,
                                onDismissRequest = { showMenu = false },
                                properties = PopupProperties(focusable = true)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .padding(top = 48.dp, end = 8.dp)
                                        .width(200.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    tonalElevation = 8.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    onSettings()
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Text("App Settings", style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    onOpenPayme()
                                                }
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Text("My PayPal (Scan Me)", style = MaterialTheme.typography.bodyLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGroup,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Group") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(groups) { group ->
                OutlinedCard(
                    onClick = { onGroupClick(group) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    group.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(group.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${group.members.size} members • ${group.currency}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
