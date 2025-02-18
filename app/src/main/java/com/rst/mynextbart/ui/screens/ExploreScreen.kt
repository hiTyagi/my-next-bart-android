package com.rst.mynextbart.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.ui.components.DepartureItem
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.viewmodel.DeparturesState
import com.rst.mynextbart.network.Estimate
import com.rst.mynextbart.utils.ColorUtils
import kotlinx.coroutines.launch
import com.rst.mynextbart.ui.components.CommonScreen

@Composable
fun StationDetailsCard(
    stationName: String,
    stationAddress: String,
    routeColors: List<String>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCreateRoute: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stationAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onCreateRoute) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = "Create route from this station",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Route colors
            if (routeColors.isNotEmpty()) {
                Text(
                    text = "Routes serving this station:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    routeColors.forEach { hexColor ->
                        Canvas(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                        ) {
                            drawCircle(
                                color = ColorUtils.parseHexColor(hexColor),
                                radius = size.minDimension / 2
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: BartViewModel,
    onBackClick: () -> Unit,
    onCreateRoute: (String, String) -> Unit
) {
    CommonScreen {
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current
        var routeColors by remember { mutableStateOf<List<String>>(emptyList()) }
        val coroutineScope = rememberCoroutineScope()

        // Fetch route colors when station changes
        LaunchedEffect(viewModel.selectedStation.first) {
            coroutineScope.launch {
                routeColors = viewModel.getStationRouteColors(viewModel.selectedStation.first)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Station Selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = viewModel.selectedStation.second,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    viewModel.stations.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                viewModel.selectStation(code, name)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Station Details Card
            StationDetailsCard(
                stationName = viewModel.selectedStation.second,
                stationAddress = viewModel.getStationAddress(viewModel.selectedStation.first),
                routeColors = routeColors,
                isFavorite = viewModel.favoriteStations.any { it.code == viewModel.selectedStation.first },
                onToggleFavorite = { 
                    viewModel.toggleFavorite(viewModel.selectedStation.first)
                    Toast.makeText(
                        context,
                        if (viewModel.favoriteStations.any { it.code == viewModel.selectedStation.first }) 
                            "Removed from favorites" else "Added to favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onCreateRoute = {
                    onCreateRoute(
                        viewModel.selectedStation.first,
                        viewModel.selectedStation.second
                    )
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Departures List
            when (val state = viewModel.departuresState) {
                is DeparturesState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is DeparturesState.Success -> {
                    val validStations = state.data.root.station.filter { station ->
                        station.etd != null && station.etd.isNotEmpty()
                    }
                    
                    if (validStations.isEmpty()) {
                        Text(
                            text = "No trains currently scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            validStations.forEach { station ->
                                station.etd?.forEach { etd ->
                                    items<Estimate>(
                                        items = etd.estimate,
                                        key = { estimate -> "${etd.destination}_${estimate.minutes}_${estimate.platform}" }
                                    ) { estimate ->
                                        DepartureItem(
                                            destination = etd.destination,
                                            estimate = estimate
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is DeparturesState.Error -> {
                    if (state.message.contains("required etd missing") || 
                        state.message.contains("\$.root")) {
                        Text(
                            text = "No trains currently scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
} 