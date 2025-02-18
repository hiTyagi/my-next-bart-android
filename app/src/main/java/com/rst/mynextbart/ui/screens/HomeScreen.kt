package com.rst.mynextbart.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.network.Estimate
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.viewmodel.DeparturesState
import com.rst.mynextbart.ui.components.DepartureItem
import com.rst.mynextbart.data.FavoriteRoute
import com.rst.mynextbart.data.FavoriteStation
import com.rst.mynextbart.utils.TimeUtils
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.rst.mynextbart.utils.ColorUtils
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.draw.rotate
import com.rst.mynextbart.ui.components.CommonScreen
import com.rst.mynextbart.ui.components.PinnedRouteCard
import com.rst.mynextbart.ui.components.RotatingRefreshButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: BartViewModel,
    onStationClick: (String, String) -> Unit,
    onExploreClick: () -> Unit
) {
    CommonScreen {
        // Existing content
        val context = LocalContext.current
        var routeRefreshTrigger by remember { mutableStateOf(0) }
        var stationRefreshTrigger by remember { mutableStateOf(0) }
        
        // Get pinned items
        val pinnedStations = viewModel.favoriteStations.filter { it.isPinned }
        val pinnedRoutes = viewModel.favoriteRoutes.filter { it.isPinned }

        if (pinnedStations.isEmpty() && pinnedRoutes.isEmpty()) {
            // Welcome message
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Welcome to MyNextBART!",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pin your favorite stations and routes to see real-time departures at a glance.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Use the Explore tab to find stations and create routes.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (pinnedRoutes.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Pinned Routes",
                            lastRefreshTime = viewModel.lastRouteRefreshTime,
                            onRefresh = { 
                                routeRefreshTrigger++
                                viewModel.refreshPinnedRoutes() 
                            }
                        )
                    }
                    
                    items(pinnedRoutes) { route ->
                        PinnedRouteCard(
                            route = route,
                            departures = viewModel.routeDeparturesState[route.fromStation + "_" + route.toStation],
                            fare = viewModel.routeFaresState[route.fromStation + "_" + route.toStation],
                            onUnpin = {
                                viewModel.togglePinRoute(route)
                                Toast.makeText(context, "Route unpinned", Toast.LENGTH_SHORT).show()
                            },
                            onClick = {
                                onStationClick(route.fromStation, route.fromStationName)
                            }
                        )
                    }
                }

                if (pinnedStations.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Pinned Stations",
                            lastRefreshTime = viewModel.lastStationRefreshTime,
                            onRefresh = {
                                stationRefreshTrigger++
                                viewModel.refreshPinnedStations()
                            }
                        )
                    }

                    items(pinnedStations) { station ->
                        PinnedStationCard(
                            station = station,
                            departures = viewModel.stationDeparturesState[station.code] ?: DeparturesState.Loading,
                            onSelect = {
                                onStationClick(station.code, station.name)
                            },
                            onTogglePin = {
                                viewModel.toggleStationPin(station)
                                Toast.makeText(context, "Station unpinned", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

            }
        }

        // Separate LaunchedEffects for routes and stations
        LaunchedEffect(routeRefreshTrigger) {
            Log.d("HomeScreen", "Starting route refresh cycle #$routeRefreshTrigger")
            viewModel.refreshPinnedRoutes()
            while (true) {
                delay(60000) // Refresh every minute
                Log.d("HomeScreen", "Auto refreshing routes")
                viewModel.refreshPinnedRoutes()
            }
        }

        LaunchedEffect(stationRefreshTrigger) {
            Log.d("HomeScreen", "Starting station refresh cycle #$stationRefreshTrigger")
            viewModel.refreshPinnedStations()
            while (true) {
                delay(60000) // Refresh every minute
                Log.d("HomeScreen", "Auto refreshing stations")
                viewModel.refreshPinnedStations()
            }
        }

        // Add logging for station departures state
        LaunchedEffect(viewModel.stationDeparturesState) {
            Log.d("HomeScreen", "Station departures state updated: ${viewModel.stationDeparturesState.entries.joinToString { "${it.key}: ${it.value::class.simpleName}" }}")
        }
    }
}

@Composable
fun RouteCard(route: FavoriteRoute) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = route.fromStationName,
                style = MaterialTheme.typography.titleMedium
            )
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                text = route.toStationName,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun StationDetailsCard(
    stationName: String,
    stationCode: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stationName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Station Code: $stationCode",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun PinnedStationCard(
    station: FavoriteStation,
    departures: DeparturesState,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onTogglePin) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Unpin station",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            when (departures) {
                is DeparturesState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                is DeparturesState.Success -> {
                    val station = departures.data.root.station.firstOrNull()
                    if (station == null || station.etd.isNullOrEmpty()) {
                        Text(
                            text = "No trains currently scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        // Group departures by destination and take latest 2 from each
                        val departuresByDestination = station.etd
                            .map { etd ->
                                etd.destination to etd.estimate
                                    .sortedBy { 
                                        if (it.minutes == "Leaving") -1
                                        else it.minutes.toIntOrNull() ?: Int.MAX_VALUE
                                    }
                                    .take(2)
                                    .map { estimate ->
                                        DepartureInfo(
                                            destination = etd.destination,
                                            estimate = estimate
                                        )
                                    }
                            }
                            .filter { (_, estimates) -> estimates.isNotEmpty() }

                        if (!departuresByDestination.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            departuresByDestination.forEach { (destination, departures) ->
                                // Add destination header
                                Text(
                                    text = "To $destination",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                // Show departures for this destination
                                departures.forEach { departure ->
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
                                                drawCircle(color = ColorUtils.parseHexColor(departure.estimate.hexColor))
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = TimeUtils.formatDepartureTime(departure.estimate.minutes).toString(),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = "[${TimeUtils.getArrivalTime(departure.estimate.minutes)}]",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${departure.estimate.length} cars",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                
                                // Add space between destinations
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            Text(
                                text = "No upcoming trains",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                is DeparturesState.Error -> {
                    if (departures.message.contains("required etd missing") || 
                        departures.message.contains("\$.root")) {
                        Text(
                            text = "No trains currently scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        Text(
                            text = departures.message,
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

private data class DepartureInfo(
    val destination: String,
    val estimate: Estimate
)

@Composable
private fun SectionHeader(
    title: String,
    lastRefreshTime: Long,
    onRefresh: () -> Unit
) {
    var secondsAgo by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Update the seconds ago every second
    LaunchedEffect(lastRefreshTime) {
        while (true) {
            secondsAgo = ((System.currentTimeMillis() - lastRefreshTime) / 1000).toInt()
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lastRefreshTime > 0) {
                Text(
                    text = when {
                        secondsAgo < 60 -> "$secondsAgo seconds ago"
                        secondsAgo < 3600 -> "${secondsAgo / 60} minutes ago"
                        else -> "${secondsAgo / 3600} hours ago"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RotatingRefreshButton(
                onClick = onRefresh,
                isRefreshing = isRefreshing
            )
        }
    }
} 