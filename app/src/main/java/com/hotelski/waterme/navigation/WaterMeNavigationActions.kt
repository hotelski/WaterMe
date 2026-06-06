package com.hotelski.waterme.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder

class WaterMeNavigationActions(
    private val navController: NavController,
) {
    fun navigateToTopLevel(destination: TopLevelDestination) {
        navController.navigate(destination.route.route) {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }
    }

    fun navigateToPlantsAfterPlantDeleted(message: String = "Plant deleted.") {
        navController.navigate(WaterMeRoute.Plants.route) {
            launchSingleTop = true
            restoreState = false
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
        }
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.set(WaterMeRoute.Plants.SUCCESS_MESSAGE_KEY, message)
    }

    fun navigateToFeedback() {
        navController.navigate(WaterMeRoute.Feedback.route) {
            launchSingleTop = true
        }
    }

    fun navigateToDonate() {
        navController.navigate(WaterMeRoute.Donate.route) {
            launchSingleTop = true
        }
    }

    fun navigateToLegal(document: String) {
        navController.navigate(WaterMeRoute.Legal.createRoute(document)) {
            launchSingleTop = true
        }
    }

    fun navigateToAddPlant() {
        navController.navigate(WaterMeRoute.AddPlant.route)
    }

    fun navigateToPlantDetails(plantId: String, builder: NavOptionsBuilder.() -> Unit = {}) {
        navController.navigate(WaterMeRoute.PlantDetails.createRoute(plantId), builder)
    }

    fun navigateToEditPlant(plantId: String) {
        navController.navigate(WaterMeRoute.EditPlant.createRoute(plantId))
    }

    fun navigateToReminderSetup(plantId: String? = null) {
        navController.navigate(WaterMeRoute.ReminderSetup.createRoute(plantId))
    }

    fun navigateToCalendar() {
        navController.navigate(WaterMeRoute.Calendar.route) {
            launchSingleTop = true
        }
    }

    fun navigateToCareHistory(plantId: String? = null) {
        navController.navigate(WaterMeRoute.CareHistory.createRoute(plantId))
    }

    fun navigateToCharacters() {
        navController.navigate(WaterMeRoute.Characters.route)
    }

    fun navigateToSettings() {
        navController.navigate(WaterMeRoute.Settings.route) {
            launchSingleTop = true
        }
    }

    fun onPlantSaved(_plantId: String) {
        navController.navigate(WaterMeRoute.Plants.route) {
            popUpTo(WaterMeRoute.AddPlant.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun onPlantUpdated(_plantId: String) {
        navController.navigate(WaterMeRoute.Plants.route) {
            popUpTo(WaterMeRoute.EditPlant.route) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    fun back() {
        navController.popBackStack()
    }
}
