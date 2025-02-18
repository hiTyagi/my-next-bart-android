package com.rst.mynextbart.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.DirectionsBike
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
import com.rst.mynextbart.network.StationInfo
import com.rst.mynextbart.utils.ColorUtils
import kotlinx.coroutines.launch
import com.rst.mynextbart.ui.components.CommonScreen
import com.rst.mynextbart.utils.TimeUtils
import kotlinx.coroutines.delay

@Composable
fun StationDetailsCard(
    stationName: String,
    stationCode: String,
    routeColors: List<String>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCreateRoute: () -> Unit,
    viewModel: BartViewModel
) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("Loading address...") }
    var stationInfo by remember { mutableStateOf<StationInfo?>(null) }
    
    LaunchedEffect(stationCode) {
        stationInfo = viewModel.getStationInfo(stationCode)
        address = stationInfo?.let { "${it.address}, ${it.city}, ${it.state} ${it.zipCode}" } ?: "Address not available"
    }

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
                    Row(
                        modifier = Modifier.clickable {
                            stationInfo?.let {
                                try {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    // Fallback to browser if Google Maps isn't installed
                                    val browserIntent = Intent(Intent.ACTION_VIEW, 
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${it.latitude},${it.longitude}")
                                    )
                                    context.startActivity(browserIntent)
                                }
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    text = "Routes serving this routes:",
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

@Composable
fun DepartureCard(
    destination: String,
    estimates: List<Estimate>,
    routeColor: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCircle(
                            color = ColorUtils.parseHexColor(routeColor)
                        )
                    }
                    Text(
                        text = destination,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "Platform ${estimates.first().platform}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            estimates.take(3).forEach { estimate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeUtils.formatDepartureTime(estimate.minutes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${estimate.length} cars",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (estimate.bikeFlag == "1") {
                            Icon(
                                imageVector = Icons.Default.DirectionsBike,
                                contentDescription = "Bikes allowed",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            if (estimates.size > 3) {
                Text(
                    text = "and ${estimates.size - 3} more...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlatformHeader(platform: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Platform $platform",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
                stationCode = viewModel.selectedStation.first,
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
                },
                viewModel = viewModel
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
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            validStations.forEach { station ->
                                station.etd?.forEach { etd ->
                                    item(key = "${etd.destination}_${etd.estimate.first().platform}") {
                                        DepartureCard(
                                            destination = etd.destination,
                                            estimates = etd.estimate,
                                            routeColor = etd.estimate.first().hexColor
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