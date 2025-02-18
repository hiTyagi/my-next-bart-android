package com.rst.mynextbart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rst.mynextbart.data.FavoritesDataStore
import com.rst.mynextbart.navigation.NavDestination
import com.rst.mynextbart.ui.screens.FavoritesScreen
import com.rst.mynextbart.ui.screens.HomeScreen
import com.rst.mynextbart.ui.screens.FavoriteRoutesScreen
import com.rst.mynextbart.ui.screens.ExploreScreen
import com.rst.mynextbart.ui.theme.MyNextBARTTheme
import com.rst.mynextbart.viewmodel.BartViewModel
import com.rst.mynextbart.viewmodel.BartViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.map
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.flow.map
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val favoritesDataStore by lazy { FavoritesDataStore(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyNextBARTTheme(
                dynamicColor = false  // Set this to false to use our custom colors
            ) {
                MainScreen(
                    viewModel = viewModel(
                        factory = BartViewModelFactory(favoritesDataStore)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BartViewModel) {
    val navController = rememberNavController()
    
    // Track current route using collectAsState
    val currentRoute by navController
        .currentBackStackEntryFlow
        .map { it.destination.route }
        .collectAsState(initial = NavDestination.Home.route)
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentRoute == NavDestination.Home.route,
                    onClick = {
                        navController.navigate(NavDestination.Home.route) {
                            popUpTo(NavDestination.Home.route) { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Stations") },
                    label = { Text("Stations") },
                    selected = currentRoute == NavDestination.Favorites.route,
                    onClick = {
                        navController.navigate(NavDestination.Favorites.route)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Route, contentDescription = "Routes") },
                    label = { Text("Routes") },
                    selected = currentRoute == NavDestination.FavoriteRoutes.route,
                    onClick = {
                        navController.navigate(NavDestination.FavoriteRoutes.route)
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Explore") },
                    label = { Text("Explore") },
                    selected = currentRoute == NavDestination.Explore.route,
                    onClick = {
                        navController.navigate(NavDestination.Explore.route)
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(NavDestination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onStationClick = { code, name ->
                        viewModel.selectStation(code, name)
                        navController.navigate(NavDestination.Explore.route)
                    },
                    onExploreClick = {
                        navController.navigate(NavDestination.Explore.route)
                    }
                )
            }
            composable(NavDestination.Favorites.route) {
                FavoritesScreen(
                    viewModel = viewModel,
                    onStationSelected = { code, name ->
                        viewModel.selectStation(code, name)
                        navController.navigate(NavDestination.Explore.route)
                    }
                )
            }
            composable(NavDestination.FavoriteRoutes.route) {
                FavoriteRoutesScreen(viewModel)
            }
            composable(NavDestination.Explore.route) {
                ExploreScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.navigateUp()
                    },
                    onCreateRoute = { stationCode, stationName ->
                        viewModel.setRouteFromStation(stationCode, stationName)
                        navController.navigate(NavDestination.FavoriteRoutes.route)
                    }
                )
            }
        }
    }
}