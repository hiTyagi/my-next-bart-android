package com.rst.mynextbart.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rst.mynextbart.repository.BartRepository
import com.rst.mynextbart.ui.theme.MyNextBARTTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WidgetSettingsActivity : ComponentActivity() {
    @Inject
    lateinit var repository: BartRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Get the widget type from intent
        val isRouteWidget = intent?.getBooleanExtra(EXTRA_IS_ROUTE_WIDGET, false) ?: false

        setContent {
            MyNextBARTTheme {
                var currentAppearance by remember {
                    mutableStateOf(
                        if (isRouteWidget) {
                            RouteWidgetConfig.getWidgetRoute(this, appWidgetId)?.appearance
                        } else {
                            StationWidgetConfig.getWidgetStation(this, appWidgetId)?.appearance
                        } ?: WidgetAppearance()
                    )
                }

                WidgetSettingsDialog(
                    currentAppearance = currentAppearance,
                    onDismiss = { finish() },
                    onConfirm = { newAppearance ->
                        if (isRouteWidget) {
                            // Update route widget
                            RouteWidgetConfig.getWidgetRoute(this, appWidgetId)?.let { routeInfo ->
                                val updatedInfo = routeInfo.copy(appearance = newAppearance)
                                RouteWidgetConfig.saveWidgetRoute(this, appWidgetId, updatedInfo)
                                RouteWidget.updateWidget(
                                    this,
                                    AppWidgetManager.getInstance(this),
                                    appWidgetId,
                                    updatedInfo,
                                    repository
                                )
                            }
                        } else {
                            // Update station widget
                            StationWidgetConfig.getWidgetStation(this, appWidgetId)?.let { stationInfo ->
                                val updatedInfo = stationInfo.copy(appearance = newAppearance)
                                StationWidgetConfig.saveWidgetStation(this, appWidgetId, updatedInfo)
                                StationWidget.updateWidget(
                                    this,
                                    AppWidgetManager.getInstance(this),
                                    appWidgetId,
                                    updatedInfo,
                                    repository
                                )
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_IS_ROUTE_WIDGET = "extra_is_route_widget"
    }
} 