package com.hotelski.waterme.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.local.entity.ThemePreference
import com.hotelski.waterme.data.preferences.SettingsPreferences
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun WaterMeNavigationScaffold(
    startDestination: String = WaterMeRoute.Today.route,
) {
    val context = LocalContext.current
    val settings by WaterMeAppContainer.settingsDataStore(context).settings.collectAsState(SettingsPreferences())
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (settings.themePreference) {
        ThemePreference.SYSTEM -> systemDarkTheme
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val navController = rememberNavController()
    val navigationActions = WaterMeNavigationActions(navController)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomNavigation = shouldShowBottomNavigation(currentDestination?.route)

    WaterMeTheme(
        darkTheme = darkTheme,
        textColorPreference = settings.textColorPreference,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
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
