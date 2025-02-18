package com.rst.mynextbart.ui.components

import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

@Composable
fun RotatingRefreshButton(
    onClick: () -> Unit,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableFloatStateOf(0f) }

    val rotationAnimation = rememberInfiniteTransition(label = "refresh")
    val animatedRotation by rotationAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            // Do one full rotation when clicked
            rotation = 360f
            delay(1000)
            rotation = 0f
        }
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Refresh",
            modifier = Modifier.graphicsLayer {
                rotationZ = if (isRefreshing) animatedRotation else rotation
            }
        )
    }
}