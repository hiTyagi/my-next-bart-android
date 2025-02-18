package com.rst.mynextbart.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.data.FavoriteRoute
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.ui.components.CommonScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteRoutesScreen(viewModel: BartViewModel) {
    CommonScreen {
        var fromStationExpanded by remember { mutableStateOf(false) }
        var toStationExpanded by remember { mutableStateOf(false) }
        
        // Get the pre-selected station if coming from Explore screen
        var selectedFromStation by remember { 
            mutableStateOf<Pair<String, String>?>(viewModel.getRouteFromStation())
        }
        var selectedToStation by remember { mutableStateOf<Pair<String, String>?>(null) }
        var routeToDelete by remember { mutableStateOf<FavoriteRoute?>(null) }
        val context = LocalContext.current
        var routeRefreshTrigger by remember { mutableStateOf(0) }

        // Clear the routeFromStation when leaving the screen
        DisposableEffect(Unit) {
            onDispose {
                viewModel.clearRouteFromStation()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // First column with dropdowns
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // From Station Selector
                    ExposedDropdownMenuBox(
                        expanded = fromStationExpanded,
                        onExpandedChange = { fromStationExpanded = !fromStationExpanded }
                    ) {
                        TextField(
                            value = selectedFromStation?.second ?: "Select departure station",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromStationExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = fromStationExpanded,
                            onDismissRequest = { fromStationExpanded = false }
                        ) {
                            viewModel.stations.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station.second) },
                                    onClick = {
                                        selectedFromStation = station
                                        fromStationExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // To Station Selector
                    ExposedDropdownMenuBox(
                        expanded = toStationExpanded,
                        onExpandedChange = { toStationExpanded = !toStationExpanded }
                    ) {
                        TextField(
                            value = selectedToStation?.second ?: "Select destination station",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toStationExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = toStationExpanded,
                            onDismissRequest = { toStationExpanded = false }
                        ) {
                            viewModel.stations.forEach { station ->
                                DropdownMenuItem(
                                    text = { Text(station.second) },
                                    onClick = {
                                        selectedToStation = station
                                        toStationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Second column with swap button
                IconButton(
                    onClick = {
                        val temp = selectedFromStation
                        selectedFromStation = selectedToStation
                        selectedToStation = temp
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap stations"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Route Button
            Button(
                onClick = {
                    if (selectedFromStation != null && selectedToStation != null) {
                        viewModel.addFavoriteRoute(selectedFromStation!!, selectedToStation!!)
                        selectedFromStation = null
                        selectedToStation = null
                        Toast.makeText(context, "Route added to favorites", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = selectedFromStation != null && selectedToStation != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Add Favorite Route")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Favorite Routes List
            if (viewModel.favoriteRoutes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No favorite routes yet")
                }
            } else {
                // Sort favorite routes by addedAt timestamp (most recently added first)
                val sortedRoutes = viewModel.favoriteRoutes.sortedByDescending { it.addedAt }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = sortedRoutes,
                        key = { "${it.fromStation}_${it.toStation}" }
                    ) { route ->
                        FavoriteRouteItem(
                            route = route,
                            onDelete = { routeToDelete = route },
                            onTogglePin = {
                                viewModel.togglePinRoute(route)
                                Toast.makeText(
                                    context,
                                    if (route.isPinned) "Route unpinned" else "Route pinned to home",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            }
        }

        // Confirmation Dialog
        if (routeToDelete != null) {
            AlertDialog(
                onDismissRequest = { routeToDelete = null },
                title = { Text("Remove Route") },
                text = { Text("Are you sure you want to remove this route from favorites?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeFavoriteRoute(routeToDelete!!)
                            Toast.makeText(context, "Route removed from favorites", Toast.LENGTH_SHORT).show()
                            routeToDelete = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { routeToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteRouteItem(
    route: FavoriteRoute,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = route.fromStationName,
                    style = MaterialTheme.typography.titleMedium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "to",
                    modifier = Modifier.padding(vertical = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = route.toStationName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Row {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (route.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (route.isPinned) "Unpin route" else "Pin route",
                        tint = if (route.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove route"
                    )
                }
            }
        }
    }
} 