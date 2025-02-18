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
import com.rst.mynextbart.ui.components.FavoriteRouteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteRoutesScreen(
    viewModel: BartViewModel,
    modifier: Modifier = Modifier
) {
    var routeToDelete by remember { mutableStateOf<FavoriteRoute?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RouteCreationForm(
            viewModel = viewModel,
            onRouteCreated = { /* Optional callback when route is created */ }
        )

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
                    FavoriteRouteCard(
                        route = route,
                        onDelete = { routeToDelete = route },
                        fare = viewModel.routeFaresState[route.fromStation + "_" + route.toStation],
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteCreationForm(
    viewModel: BartViewModel,
    onRouteCreated: () -> Unit
) {
    var selectedFromStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedToStation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var fromStationExpanded by remember { mutableStateOf(false) }
    var toStationExpanded by remember { mutableStateOf(false) }
    var fare by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Fetch fare when both stations are selected
    LaunchedEffect(selectedFromStation, selectedToStation) {
        if (selectedFromStation != null && selectedToStation != null) {
            fare = viewModel.getFare(selectedFromStation!!.first, selectedToStation!!.first)
        } else {
            fare = null
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // From Station Row with Swap Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = fromStationExpanded,
                onExpandedChange = { fromStationExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = selectedFromStation?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromStationExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("From Station") }
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

            IconButton(
                onClick = {
                    val temp = selectedFromStation
                    selectedFromStation = selectedToStation
                    selectedToStation = temp
                },
                modifier = Modifier.width(68.dp) // Fixed width to match fare area
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap stations"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // To Station Row with Fare
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = toStationExpanded,
                onExpandedChange = { toStationExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = selectedToStation?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toStationExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    label = { Text("To Station") }
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

            Box(
                modifier = Modifier.width(68.dp), // Fixed width to match swap button
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$${fare ?: "0.00"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    onRouteCreated()
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
    }
}