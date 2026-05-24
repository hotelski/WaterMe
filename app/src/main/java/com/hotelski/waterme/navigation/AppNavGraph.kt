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
import com.hotelski.waterme.feature.addplant.AddPlantEvent
import com.hotelski.waterme.feature.addplant.AddPlantScreen
import com.hotelski.waterme.feature.addplant.AddPlantUiState
import com.hotelski.waterme.feature.calendar.CalendarEvent
import com.hotelski.waterme.feature.calendar.CalendarScreen
import com.hotelski.waterme.feature.calendar.CalendarUiState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.editplant.EditPlantEvent
import com.hotelski.waterme.feature.editplant.EditPlantScreen
import com.hotelski.waterme.feature.editplant.EditPlantUiState
import com.hotelski.waterme.feature.history.CareHistoryEvent
import com.hotelski.waterme.feature.history.CareHistoryScreen
import com.hotelski.waterme.feature.history.CareHistoryUiState
import com.hotelski.waterme.feature.onboarding.OnboardingEvent
import com.hotelski.waterme.feature.onboarding.OnboardingScreen
import com.hotelski.waterme.feature.onboarding.OnboardingUiState
import com.hotelski.waterme.feature.plantdetails.PlantDetailsEvent
import com.hotelski.waterme.feature.plantdetails.PlantDetailsScreen
import com.hotelski.waterme.feature.plantdetails.PlantDetailsUiState
import com.hotelski.waterme.feature.plants.PlantsEvent
import com.hotelski.waterme.feature.plants.PlantsScreen
import com.hotelski.waterme.feature.plants.PlantsUiState
import com.hotelski.waterme.feature.reminders.ReminderSetupEvent
import com.hotelski.waterme.feature.reminders.ReminderSetupScreen
import com.hotelski.waterme.feature.reminders.ReminderSetupUiState
import com.hotelski.waterme.feature.settings.SettingsEvent
import com.hotelski.waterme.feature.settings.SettingsScreen
import com.hotelski.waterme.feature.settings.SettingsUiState
import com.hotelski.waterme.feature.today.TodayEvent
import com.hotelski.waterme.feature.today.TodayScreen
import com.hotelski.waterme.feature.today.TodayUiState

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = WaterMeRoute.Onboarding.route,
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
        waterMeComposable(WaterMeRoute.Onboarding.route) {
            OnboardingScreen(
                uiState = OnboardingUiState(),
                onEvent = { event ->
                    when (event) {
                        OnboardingEvent.StartClicked -> navigationActions.navigateFromOnboardingToToday()
                        OnboardingEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(WaterMeRoute.Today.route) {
            TodayScreen(
                uiState = TodayUiState(
                    tasks = WaterMePreviewData.tasks,
                    healthNotes = WaterMePreviewData.healthNotes,
                    plantCount = WaterMePreviewData.plants.size,
                    reminderCount = WaterMePreviewData.reminders.size,
                ),
                onEvent = { event ->
                    when (event) {
                        TodayEvent.AddPlantClicked -> navigationActions.navigateToAddPlant()
                        is TodayEvent.PlantClicked -> navigationActions.navigateToPlantDetails(event.plantId)
                        is TodayEvent.CompleteTask -> Unit
                        is TodayEvent.SkipTask -> Unit
                        is TodayEvent.SnoozeTask -> Unit
                        TodayEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(WaterMeRoute.Plants.route) {
            PlantsScreen(
                uiState = PlantsUiState(plants = WaterMePreviewData.plants),
                onEvent = { event ->
                    when (event) {
                        PlantsEvent.AddPlantClicked -> navigationActions.navigateToAddPlant()
                        is PlantsEvent.PlantClicked -> navigationActions.navigateToPlantDetails(event.plantId)
                        is PlantsEvent.SearchQueryChanged -> Unit
                        PlantsEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(WaterMeRoute.AddPlant.route) {
            AddPlantScreen(
                uiState = AddPlantUiState(),
                onEvent = { event ->
                    when (event) {
                        AddPlantEvent.BackClicked -> navigationActions.back()
                        AddPlantEvent.ChoosePhotoClicked -> Unit
                        AddPlantEvent.SaveClicked -> navigationActions.navigateToReminderSetup()
                        is AddPlantEvent.NameChanged -> Unit
                        is AddPlantEvent.PlantTypeChanged -> Unit
                        is AddPlantEvent.LocationChanged -> Unit
                        is AddPlantEvent.NotesChanged -> Unit
                        is AddPlantEvent.ReminderEnabledChanged -> Unit
                        is AddPlantEvent.ReminderEveryDaysChanged -> Unit
                        is AddPlantEvent.ReminderStartsInChanged -> Unit
                        AddPlantEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(
            route = WaterMeRoute.PlantDetails.route,
            arguments = listOf(navArgument(WaterMeRoute.PlantDetails.PLANT_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val plantId = entry.arguments
                ?.getString(WaterMeRoute.PlantDetails.PLANT_ID_ARG)
                .orMissingPlantId()

            PlantDetailsScreen(
                uiState = PlantDetailsUiState(
                    plant = WaterMePreviewData.plantDetails.copy(id = plantId),
                    reminders = WaterMePreviewData.reminders,
                    pendingTasks = WaterMePreviewData.tasks,
                    careHistory = WaterMePreviewData.history,
                    healthNotes = WaterMePreviewData.healthNotes,
                ),
                onEvent = { event ->
                    when (event) {
                        PlantDetailsEvent.BackClicked -> navigationActions.back()
                        PlantDetailsEvent.EditClicked -> navigationActions.navigateToEditPlant(plantId)
                        PlantDetailsEvent.ViewAllHistoryClicked -> navigationActions.navigateToCareHistory(plantId)
                        PlantDetailsEvent.AddHealthNoteClicked -> Unit
                        is PlantDetailsEvent.CompleteTask -> Unit
                        is PlantDetailsEvent.SkipTask -> Unit
                        is PlantDetailsEvent.SnoozeTask -> Unit
                        is PlantDetailsEvent.HealthNoteChanged -> Unit
                        is PlantDetailsEvent.HealthMoodSelected -> Unit
                        PlantDetailsEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(
            route = WaterMeRoute.EditPlant.route,
            arguments = listOf(navArgument(WaterMeRoute.EditPlant.PLANT_ID_ARG) { type = NavType.StringType }),
        ) { entry ->
            val plantId = entry.arguments
                ?.getString(WaterMeRoute.EditPlant.PLANT_ID_ARG)
                .orMissingPlantId()

            EditPlantScreen(
                uiState = EditPlantUiState(
                    name = WaterMePreviewData.plantDetails.name,
                    plantType = WaterMePreviewData.plantDetails.plantType,
                    location = WaterMePreviewData.plantDetails.location,
                    notes = WaterMePreviewData.plantDetails.notes,
                    reminders = WaterMePreviewData.reminderDrafts,
                ),
                onEvent = { event ->
                    when (event) {
                        EditPlantEvent.BackClicked -> navigationActions.back()
                        EditPlantEvent.SaveClicked -> navigationActions.onPlantUpdated(plantId)
                        EditPlantEvent.DeleteClicked -> navigationActions.navigateToTopLevel(TopLevelDestination.Plants)
                        EditPlantEvent.ChangePhotoClicked -> Unit
                        is EditPlantEvent.NameChanged -> Unit
                        is EditPlantEvent.PlantTypeChanged -> Unit
                        is EditPlantEvent.LocationChanged -> Unit
                        is EditPlantEvent.NotesChanged -> Unit
                        is EditPlantEvent.ReminderEnabledChanged -> Unit
                        is EditPlantEvent.ReminderEveryDaysChanged -> Unit
                        EditPlantEvent.RetryClicked -> Unit
                    }
                },
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
            CalendarScreen(
                uiState = CalendarUiState(days = WaterMePreviewData.calendarDays),
                onEvent = { event ->
                    when (event) {
                        CalendarEvent.TodayClicked -> navigationActions.navigateToCalendar()
                        is CalendarEvent.TaskClicked -> navigationActions.navigateToPlantDetails(event.plantId)
                        is CalendarEvent.CompleteTask -> Unit
                        CalendarEvent.RetryClicked -> Unit
                    }
                },
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
            CareHistoryScreen(
                uiState = CareHistoryUiState(entries = WaterMePreviewData.history),
                onEvent = { event ->
                    when (event) {
                        CareHistoryEvent.BackClicked -> navigationActions.back()
                        is CareHistoryEvent.FilterSelected -> Unit
                        CareHistoryEvent.RetryClicked -> Unit
                    }
                },
            )
        }

        waterMeComposable(WaterMeRoute.Settings.route) {
            SettingsScreen(
                uiState = SettingsUiState(
                    plantCount = WaterMePreviewData.plants.size,
                    activeReminderCount = WaterMePreviewData.reminders.size,
                    careHistoryCount = WaterMePreviewData.history.size,
                    healthNoteCount = WaterMePreviewData.healthNotes.size,
                ),
                onEvent = { event ->
                    when (event) {
                        SettingsEvent.ShowOnboardingClicked -> navController.navigate(WaterMeRoute.Onboarding.route)
                        SettingsEvent.RequestNotificationPermissionClicked -> Unit
                        is SettingsEvent.NotificationsChanged -> Unit
                        is SettingsEvent.ThemePreferenceChanged -> Unit
                        is SettingsEvent.MeasurementUnitsChanged -> Unit
                        is SettingsEvent.BackupSyncChanged -> Unit
                        SettingsEvent.RetryClicked -> Unit
                    }
                },
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
