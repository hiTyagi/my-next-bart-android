package com.rst.mynextbart.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WidgetSettingsDialog(
    currentAppearance: WidgetAppearance,
    onDismiss: () -> Unit,
    onConfirm: (WidgetAppearance) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(currentAppearance.theme) }
    var transparency by remember { mutableStateOf(1f - currentAppearance.backgroundAlpha) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Widget Settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Theme Selection
                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium
                    )
                    RadioButton(
                        selected = selectedTheme == WidgetTheme.SYSTEM,
                        onClick = { selectedTheme = WidgetTheme.SYSTEM },
                        label = "Follow System"
                    )
                    RadioButton(
                        selected = selectedTheme == WidgetTheme.LIGHT,
                        onClick = { selectedTheme = WidgetTheme.LIGHT },
                        label = "Light"
                    )
                    RadioButton(
                        selected = selectedTheme == WidgetTheme.DARK,
                        onClick = { selectedTheme = WidgetTheme.DARK },
                        label = "Dark"
                    )
                }

                // Transparency Slider
                Column {
                    Text(
                        text = "Background Transparency",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = transparency,
                        onValueChange = { transparency = it },
                        valueRange = 0f..0.9f,
                        steps = 9
                    )
                    Text(
                        text = "${(transparency * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        WidgetAppearance(
                            theme = selectedTheme,
                            backgroundAlpha = 1f - transparency
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
} 