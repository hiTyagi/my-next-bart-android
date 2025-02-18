package com.rst.mynextbart.navigation

sealed class NavDestination(val route: String) {
    data object Home : NavDestination("home")
    data object Explore : NavDestination("explore")
    data object Favorites : NavDestination("favorites")
    data object FavoriteRoutes : NavDestination("favorite_routes")
} 