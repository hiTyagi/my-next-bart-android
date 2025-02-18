package com.rst.mynextbart.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.data.FavoriteStation
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.ui.components.CommonScreen

@Composable
fun FavoritesScreen(
    viewModel: BartViewModel,
    onStationSelected: (String, String) -> Unit
) {
    CommonScreen {
        val context = LocalContext.current
        var stationToDelete by remember { mutableStateOf<FavoriteStation?>(null) }
        
        if (viewModel.favoriteStations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No favorite stations yet")
            }
        } else {
            // Sort favorites by addedAt timestamp (most recently added first)
            val sortedFavorites = viewModel.favoriteStations.sortedByDescending { it.addedAt }
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sortedFavorites,
                    key = { it.code }
                ) { station ->
                    FavoriteStationItem(
                        station = station,
                        onSelect = { onStationSelected(station.code, station.name) },
                        onTogglePin = { viewModel.toggleStationPin(station) },
                        onDelete = { stationToDelete = station }
                    )
                }
            }
        }
        
        // Confirmation Dialog
        if (stationToDelete != null) {
            AlertDialog(
                onDismissRequest = { stationToDelete = null },
                title = { Text("Remove Station") },
                text = { Text("Are you sure you want to remove ${stationToDelete?.name} from favorites?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val station = stationToDelete // Capture the current value
                            if (station != null) {
                                viewModel.toggleFavorite(station.code)
                                Toast.makeText(context, "Station removed from favorites", Toast.LENGTH_SHORT).show()
                            }
                            stationToDelete = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { stationToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteStationItem(
    station: FavoriteStation,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Station Code: ${station.code}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row {
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = if (station.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (station.isPinned) "Unpin station" else "Pin station",
                        tint = if (station.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from favorites"
                    )
                }
            }
        }
    }
} 