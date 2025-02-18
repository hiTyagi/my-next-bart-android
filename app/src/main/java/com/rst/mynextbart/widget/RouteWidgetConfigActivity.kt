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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rst.mynextbart.ui.theme.MyNextBARTTheme
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.repository.BartRepository
import com.rst.mynextbart.data.FavoriteRoute
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RouteWidgetConfigActivity : ComponentActivity() {
    private val viewModel: BartViewModel by viewModels()
    
    @Inject
    lateinit var repository: BartRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_CANCELED, resultValue)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MyNextBARTTheme {
                RouteConfigurationScreen(
                    viewModel = viewModel,
                    onRouteSelected = { route, appearance ->
                        val routeInfo = RouteInfo(
                            fromStation = route.fromStation,
                            fromStationName = route.fromStationName,
                            toStation = route.toStation,
                            toStationName = route.toStationName,
                            appearance = appearance
                        )
                        
                        RouteWidgetConfig.saveWidgetRoute(this, appWidgetId, routeInfo)
                        
                        val appWidgetManager = AppWidgetManager.getInstance(this)
                        RouteWidget.updateWidget(
                            context = this,
                            appWidgetManager = appWidgetManager,
                            appWidgetId = appWidgetId,
                            routeInfo = routeInfo,
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
fun RouteConfigurationScreen(
    viewModel: BartViewModel,
    onRouteSelected: (FavoriteRoute, WidgetAppearance) -> Unit
) {
    val favoriteRoutes = viewModel.favoriteRoutes
    var showSettings by remember { mutableStateOf<FavoriteRoute?>(null) }
    
    if (showSettings != null) {
        WidgetSettingsDialog(
            currentAppearance = WidgetAppearance(), // Default settings
            onDismiss = { showSettings = null },
            onConfirm = { appearance ->
                onRouteSelected(showSettings!!, appearance)
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
                text = "Select a route for your widget",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (favoriteRoutes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No favorite routes available")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favoriteRoutes) { route ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSettings = route }
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
                                        text = route.fromStationName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "to",
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Text(
                                        text = route.toStationName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 