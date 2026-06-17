package com.hotelski.waterme.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hotelski.waterme.feature.common.AiPlantCareRoute
import com.hotelski.waterme.feature.common.AddPlantRoute
import com.hotelski.waterme.feature.common.CalendarRoute
import com.hotelski.waterme.feature.common.CareHistoryRoute
import com.hotelski.waterme.feature.common.CharactersRoute
import com.hotelski.waterme.feature.common.DonateRoute
import com.hotelski.waterme.feature.common.EditPlantRoute
import com.hotelski.waterme.feature.common.FeedbackRoute
import com.hotelski.waterme.feature.common.GuideRoute
import com.hotelski.waterme.feature.common.HomeRoute
import com.hotelski.waterme.feature.common.PlantDetailsRoute
import com.hotelski.waterme.feature.common.PlantScannerRoute
import com.hotelski.waterme.feature.common.PlantsRoute
import com.hotelski.waterme.feature.common.SettingsRoute
import com.hotelski.waterme.feature.common.WaterMePreviewData
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
            FeedbackRoute(
                onBack = navigationActions::back,
            )
        }

        waterMeComposable(WaterMeRoute.Donate.route) {
            DonateRoute(
                onBack = navigationActions::back,
            )
        }

        waterMeComposable(WaterMeRoute.Guide.route) {
            GuideRoute(
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
                onAddPlant = { navigationActions.navigateToAddPlant() },
                onOpenCalendar = navigationActions::navigateToCalendar,
                onOpenDonate = navigationActions::navigateToDonate,
                onOpenFeedback = navigationActions::navigateToFeedback,
                onOpenGuide = navigationActions::navigateToGuide,
                onOpenPlants = { navigationActions.navigateToTopLevel(TopLevelDestination.Plants) },
            )
        }

        waterMeComposable(WaterMeRoute.Plants.route) { entry ->
            val pendingSuccessMessage by entry.savedStateHandle
                .getStateFlow<String?>(WaterMeRoute.Plants.SUCCESS_MESSAGE_KEY, null)
                .collectAsStateWithLifecycle()

            PlantsRoute(
                onAddPlant = { navigationActions.navigateToAddPlant() },
                onOpenAiCare = navigationActions::navigateToAiPlantCare,
                onOpenPlantScanner = navigationActions::navigateToPlantScanner,
                onOpenPlant = navigationActions::navigateToPlantDetails,
                onEditPlant = navigationActions::navigateToEditPlant,
                pendingSuccessMessage = pendingSuccessMessage,
                onPendingSuccessMessageConsumed = {
                    entry.savedStateHandle.remove<String>(WaterMeRoute.Plants.SUCCESS_MESSAGE_KEY)
                },
            )
        }

        waterMeComposable(
            route = WaterMeRoute.AddPlant.route,
            arguments = listOf(
                navArgument(WaterMeRoute.AddPlant.PREFILL_NAME_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(WaterMeRoute.AddPlant.PREFILL_PHOTO_URI_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(WaterMeRoute.AddPlant.PREFILL_SCIENTIFIC_NAME_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(WaterMeRoute.AddPlant.PREFILL_NOTES_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(WaterMeRoute.AddPlant.PREFILL_WATERING_DAYS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(WaterMeRoute.AddPlant.PREFILL_FERTILIZING_DAYS_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            AddPlantRoute(
                onBack = navigationActions::back,
                onOpenPhotoPicker = {},
                onPlantCreated = navigationActions::onPlantSaved,
                prefillName = entry.arguments?.getString(WaterMeRoute.AddPlant.PREFILL_NAME_ARG),
                prefillPhotoUri = entry.arguments?.getString(WaterMeRoute.AddPlant.PREFILL_PHOTO_URI_ARG),
                prefillScientificName = entry.arguments?.getString(
                    WaterMeRoute.AddPlant.PREFILL_SCIENTIFIC_NAME_ARG,
                ),
                prefillNotes = entry.arguments?.getString(WaterMeRoute.AddPlant.PREFILL_NOTES_ARG),
                prefillWateringDays = entry.arguments
                    ?.getString(WaterMeRoute.AddPlant.PREFILL_WATERING_DAYS_ARG)
                    ?.toIntOrNull(),
                prefillFertilizingDays = entry.arguments
                    ?.getString(WaterMeRoute.AddPlant.PREFILL_FERTILIZING_DAYS_ARG)
                    ?.toIntOrNull(),
            )
        }

        waterMeComposable(WaterMeRoute.PlantScanner.route) {
            PlantScannerRoute(
                onBack = navigationActions::back,
                onSaveToPlants = { name, photoUri ->
                    navigationActions.navigateToAddPlant(
                        prefillName = name,
                        prefillPhotoUri = photoUri,
                    )
                },
            )
        }

        waterMeComposable(WaterMeRoute.AiPlantCare.route) {
            AiPlantCareRoute(
                onBack = navigationActions::back,
                onAddPlant = { name, scientificName, notes, wateringDays, fertilizingDays ->
                    navigationActions.navigateToAddPlant(
                        prefillName = name,
                        prefillScientificName = scientificName,
                        prefillNotes = notes,
                        prefillWateringDays = wateringDays,
                        prefillFertilizingDays = fertilizingDays,
                    )
                },
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
                onPlantDeleted = { navigationActions.navigateToPlantsAfterPlantDeleted() },
            )
        }

        waterMeComposable(
            route = WaterMeRoute.EditPlant.route,
            arguments = listOf(navArgument(WaterMeRoute.EditPlant.PLANT_ID_ARG) { type = NavType.StringType }),
        ) {
            EditPlantRoute(
                onBack = navigationActions::back,
                onPlantUpdated = navigationActions::onPlantUpdated,
                onPlantDeleted = { navigationActions.navigateToPlantsAfterPlantDeleted() },
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
