package com.rst.mynextbart.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.data.FavoriteRoute
import com.rst.mynextbart.network.Estimate
import com.rst.mynextbart.utils.ColorUtils
import com.rst.mynextbart.utils.TimeUtils
import com.rst.mynextbart.viewmodel.DeparturesState

@Composable
fun PinnedRouteCard(
    route: FavoriteRoute,
    departures: DeparturesState?,
    fare: String?,
    onUnpin: () -> Unit,
    onClick: () -> Unit
) {
    // Add logging for individual route card state
    LaunchedEffect(departures) {
        Log.d("PinnedRouteCard", "Departures for ${route.fromStation} to ${route.toStation}: $departures")
    }

    Card(
        onClick = onClick,
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
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = route.fromStationName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "to",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = route.toStationName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                IconButton(onClick = onUnpin) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Unpin route",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            fare?.let {
                Text(
                    text = "$$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                text = "No upcoming trains for this route",
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
                null -> {
                    Text(
                        text = "Loading departures...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

private data class DepartureInfo(
    val destination: String,
    val estimate: Estimate
) 