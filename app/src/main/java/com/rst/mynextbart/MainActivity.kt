package com.rst.mynextbart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rst.mynextbart.navigation.NavDestination
import com.rst.mynextbart.ui.screens.*
import com.rst.mynextbart.ui.theme.MyNextBARTTheme
import com.rst.mynextbart.viewmodel.BartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: BartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyNextBARTTheme {
                MainContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainContent(viewModel: BartViewModel) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute by navController
                    .currentBackStackEntryFlow
                    .map { it.destination.route }
                    .collectAsState(initial = NavDestination.Home.route)
                    
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
        }
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