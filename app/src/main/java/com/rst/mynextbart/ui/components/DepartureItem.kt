package com.rst.mynextbart.ui.components

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.network.Estimate
import com.rst.mynextbart.utils.TimeUtils
import com.rst.mynextbart.utils.ColorUtils

@Composable
fun DepartureItem(
    destination: String,
    estimate: Estimate,
    modifier: Modifier = Modifier
) {
    // Add logging
    Log.d("DepartureItem", "Hex color for $destination: ${estimate.hexColor}")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color indicator
            Canvas(modifier = Modifier.size(12.dp)) {
                val color = ColorUtils.parseHexColor(estimate.hexColor)
                drawCircle(color = color)
            }
            
            Column {
                Text(
                    text = destination,
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = TimeUtils.formatDepartureTime(estimate.minutes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "[${TimeUtils.getArrivalTime(estimate.minutes)}]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Text(
            text = "${estimate.length} cars",
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 