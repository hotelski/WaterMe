package com.hotelski.waterme.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hotelski.waterme.feature.common.AddPlantRoute
import com.hotelski.waterme.feature.common.CalendarRoute
import com.hotelski.waterme.feature.common.CareHistoryRoute
import com.hotelski.waterme.feature.common.CharactersRoute
import com.hotelski.waterme.feature.common.EditPlantRoute
import com.hotelski.waterme.feature.common.HomeRoute
import com.hotelski.waterme.feature.common.PlantDetailsRoute
import com.hotelski.waterme.feature.common.PlantsRoute
import com.hotelski.waterme.feature.common.SettingsRoute
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.feedback.FeedbackScreen
import com.hotelski.waterme.feature.feedback.FeedbackUiState
import com.hotelski.waterme.feature.legal.LegalDocument
import com.hotelski.waterme.feature.legal.LegalScreen
import com.hotelski.waterme.feature.reminders.ReminderSetupEvent
import com.hotelski.waterme.feature.reminders.ReminderSetupScreen
import com.hotelski.waterme.feature.reminders.ReminderSetupUiState

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = WaterMeRoute.Today.route,
) {
    val navigationActions = WaterMeNavigationActions(navController)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(NavTransitionDurationMillis)) },
        exitTransition = { fadeOut(tween(NavTransitionDurationMillis)) },
        popEnterTransition = { fadeIn(tween(NavTransitionDurationMillis)) },
        popExitTransition = { fadeOut(tween(NavTransitionDurationMillis)) },
    ) {
        waterMeComposable(WaterMeRoute.Feedback.route) {
            FeedbackScreen(
                uiState = FeedbackUiState(),
                onBack = navigationActions::back,
            )
        }

        waterMeComposable(
            route = WaterMeRoute.Legal.route,
            arguments = listOf(navArgument(WaterMeRoute.Legal.DOCUMENT_ARG) { type = NavType.StringType }),
        ) { entry ->
            LegalScreen(
                document = LegalDocument.fromRouteValue(entry.arguments?.getString(WaterMeRoute.Legal.DOCUMENT_ARG)),
                onBack = navigationActions::back,
            )
        }

        waterMeComposable(WaterMeRoute.Today.route) {
            HomeRoute(
                onAddPlant = navigationActions::navigateToAddPlant,
                onOpenCalendar = navigationActions::navigateToCalendar,
                onOpenFeedback = navigationActions::navigateToFeedback,
                onOpenPlants = { navigationActions.navigateToTopLevel(TopLevelDestination.Plants) },
            )
        }

        waterMeComposable(WaterMeRoute.Plants.route) {
            PlantsRoute(
                onAddPlant = navigationActions::navigateToAddPlant,
                onOpenPlant = navigationActions::navigateToPlantDetails,
                onEditPlant = navigationActions::navigateToEditPlant,
            )
        }

        waterMeComposable(WaterMeRoute.AddPlant.route) {
            AddPlantRoute(
                onBack = navigationActions::back,
                onOpenPhotoPicker = {},
                onPlantCreated = navigationActions::onPlantSaved,
            )
        }

        waterMeComposable(
            route = WaterMeRoute.PlantDetails.route,
            arguments = listOf(navArgument(WaterMeRoute.PlantDetails.PLANT_ID_ARG) { type = NavType.StringType }),
        ) {
            PlantDetailsRoute(
                onBack = navigationActions::back,
                onEditPlant = navigationActions::navigateToEditPlant,
                onViewHistory = { plantId -> navigationActions.navigateToCareHistory(plantId) },
                onPlantDeleted = { navigationActions.navigateToTopLevel(TopLevelDestination.Plants) },
            )
        }

        waterMeComposable(
            route = WaterMeRoute.EditPlant.route,
            arguments = listOf(navArgument(WaterMeRoute.EditPlant.PLANT_ID_ARG) { type = NavType.StringType }),
        ) {
            EditPlantRoute(
                onBack = navigationActions::back,
                onPlantUpdated = navigationActions::onPlantUpdated,
                onPlantDeleted = { navigationActions.navigateToTopLevel(TopLevelDestination.Plants) },
                onOpenPhotoPicker = {},
            )
        }

        waterMeComposable(
            route = WaterMeRoute.ReminderSetup.route,
            arguments = listOf(
                navArgument(WaterMeRoute.ReminderSetup.PLANT_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val plantId = entry.arguments?.getString(WaterMeRoute.ReminderSetup.PLANT_ID_ARG)

            ReminderSetupScreen(
                uiState = ReminderSetupUiState(
                    plantId = plantId,
                    plantName = if (plantId == null) "New plant" else WaterMePreviewData.plantDetails.name,
                ),
                onEvent = { event ->
                    when (event) {
                        ReminderSetupEvent.BackClicked -> navigationActions.back()
                        ReminderSetupEvent.SaveClicked -> {
                            if (plantId == null) {
                                navigationActions.navigateToTopLevel(TopLevelDestination.Plants)
                            } else {
                                navigationActions.navigateToPlantDetails(plantId)
                            }
                        }
                        is ReminderSetupEvent.ReminderEnabledChanged -> Unit
                        is ReminderSetupEvent.EveryDaysChanged -> Unit
                        is ReminderSetupEvent.StartsInDaysChanged -> Unit
                        ReminderSetupEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(WaterMeRoute.Calendar.route) {
            CalendarRoute(
                onOpenPlant = { plantId -> navigationActions.navigateToPlantDetails(plantId) },
                onScrollToToday = {},
            )
        }

        waterMeComposable(
            route = WaterMeRoute.CareHistory.route,
            arguments = listOf(
                navArgument(WaterMeRoute.CareHistory.PLANT_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            CareHistoryRoute(
                onBack = navigationActions::back,
            )
        }

        waterMeComposable(WaterMeRoute.Settings.route) {
            SettingsRoute(
                onOpenFeedback = navigationActions::navigateToFeedback,
                onOpenLegal = { document -> navigationActions.navigateToLegal(document.routeValue) },
                onRequestNotificationPermission = {},
                onOpenCharacters = navigationActions::navigateToCharacters,
            )
        }

        waterMeComposable(WaterMeRoute.Characters.route) {
            CharactersRoute(
                onBack = navigationActions::back,
            )
        }
    }
}

private fun NavGraphBuilder.waterMeComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = { fadeIn(tween(NavTransitionDurationMillis)) },
        exitTransition = { fadeOut(tween(NavTransitionDurationMillis)) },
        popEnterTransition = { fadeIn(tween(NavTransitionDurationMillis)) },
        popExitTransition = { fadeOut(tween(NavTransitionDurationMillis)) },
    ) { backStackEntry ->
        content(backStackEntry)
    }
}

private const val NavTransitionDurationMillis = 180
