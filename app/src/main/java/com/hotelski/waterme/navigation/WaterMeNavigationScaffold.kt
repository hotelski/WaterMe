package com.hotelski.waterme.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun WaterMeNavigationScaffold(
    startDestination: String = WaterMeRoute.Onboarding.route,
) {
    val navController = rememberNavController()
    val navigationActions = WaterMeNavigationActions(navController)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomNavigation = shouldShowBottomNavigation(currentDestination?.route)

    WaterMeTheme {
        Scaffold(
            containerColor = GardenBackground,
            bottomBar = {
                if (showBottomNavigation) {
                    BottomNavigationBar(
                        currentDestination = currentDestination,
                        onDestinationClick = navigationActions::navigateToTopLevel,
                    )
                }
            },
        ) { innerPadding ->
            AppNavGraph(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
