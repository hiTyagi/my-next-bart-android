package com.rst.mynextbart.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.data.FavoriteStation
import com.rst.mynextbart.ui.theme.MyNextBARTTheme
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.repository.BartRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StationWidgetConfigActivity : ComponentActivity() {
    private val viewModel: BartViewModel by viewModels()
    
    @Inject
    lateinit var repository: BartRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get the widget ID from the intent
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Set the result to CANCELED in case the user backs out
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_CANCELED, resultValue)

        // If the widget ID is invalid, finish the activity
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyNextBARTTheme {
                StationConfigurationScreen(
                    viewModel = viewModel,
                    onStationSelected = { station, appearance ->
                        val stationInfo = StationInfo(
                            code = station.code,
                            name = station.name,
                            appearance = appearance
                        )
                        
                        StationWidgetConfig.saveWidgetStation(this, appWidgetId, stationInfo)
                        
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        StationWidget.updateWidget(
                            context = this,
                            appWidgetManager = appWidgetManager,
                            appWidgetId = appWidgetId,
                            stationInfo = stationInfo,
                            repository = repository
                        )
                        
                        val resultValue = Intent().apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun StationConfigurationScreen(
    viewModel: BartViewModel,
    onStationSelected: (FavoriteStation, WidgetAppearance) -> Unit
) {
    val favoriteStations = viewModel.favoriteStations
    var showSettings by remember { mutableStateOf<FavoriteStation?>(null) }
    
    if (showSettings != null) {
        WidgetSettingsDialog(
            currentAppearance = WidgetAppearance(),
            onDismiss = { showSettings = null },
            onConfirm = { appearance ->
                onStationSelected(showSettings!!, appearance)
                showSettings = null
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Select a station for your widget",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (favoriteStations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No favorite stations available")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteStations) { station ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSettings = station }
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Station Code: ${station.code}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 