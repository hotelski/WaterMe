package com.hotelski.waterme.feature.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotelski.waterme.feature.aiplantcare.AiPlantCareEffect
import com.hotelski.waterme.feature.aiplantcare.AiPlantCareScreen
import com.hotelski.waterme.feature.aiplantcare.AiPlantCareViewModel
import com.hotelski.waterme.feature.addplant.AddPlantEffect
import com.hotelski.waterme.feature.addplant.AddPlantScreen
import com.hotelski.waterme.feature.addplant.AddPlantViewModel
import com.hotelski.waterme.feature.calendar.CalendarEffect
import com.hotelski.waterme.feature.calendar.CalendarScreen
import com.hotelski.waterme.feature.calendar.CalendarViewModel
import com.hotelski.waterme.feature.characters.CharactersEffect
import com.hotelski.waterme.feature.characters.CharactersScreen
import com.hotelski.waterme.feature.characters.CharactersViewModel
import com.hotelski.waterme.feature.history.CareHistoryEffect
import com.hotelski.waterme.feature.history.CareHistoryScreen
import com.hotelski.waterme.feature.history.CareHistoryViewModel
import com.hotelski.waterme.feature.guide.HowToUseScreen
import com.hotelski.waterme.feature.editplant.EditPlantEffect
import com.hotelski.waterme.feature.editplant.EditPlantScreen
import com.hotelski.waterme.feature.editplant.EditPlantViewModel
import com.hotelski.waterme.feature.donate.DonateScreen
import com.hotelski.waterme.feature.donate.DonateViewModel
import com.hotelski.waterme.feature.feedback.FeedbackScreen
import com.hotelski.waterme.feature.feedback.FeedbackViewModel
import com.hotelski.waterme.feature.plantdetails.PlantDetailsEffect
import com.hotelski.waterme.feature.plantdetails.PlantDetailsScreen
import com.hotelski.waterme.feature.plantdetails.PlantDetailsViewModel
import com.hotelski.waterme.feature.plantscanner.PlantScannerEffect
import com.hotelski.waterme.feature.plantscanner.PlantScannerEvent
import com.hotelski.waterme.feature.plantscanner.PlantScannerScreen
import com.hotelski.waterme.feature.plantscanner.PlantScannerViewModel
import com.hotelski.waterme.feature.plants.PlantsEffect
import com.hotelski.waterme.feature.plants.PlantsScreen
import com.hotelski.waterme.feature.plants.PlantsViewModel
import com.hotelski.waterme.feature.legal.LegalDocument
import com.hotelski.waterme.feature.settings.SettingsEffect
import com.hotelski.waterme.feature.settings.SettingsScreen
import com.hotelski.waterme.feature.settings.SettingsViewModel
import com.hotelski.waterme.feature.today.HomeEffect
import com.hotelski.waterme.feature.today.HomeViewModel
import com.hotelski.waterme.feature.today.TodayScreen
import com.hotelski.waterme.feature.today.TodayEvent
import com.hotelski.waterme.notifications.NotificationPermissionHelper

@Composable
fun HomeRoute(
    onAddPlant: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenDonate: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenPlants: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(homeViewModel) {
        homeViewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToAddPlant -> onAddPlant()
                HomeEffect.NavigateToCalendar -> onOpenCalendar()
                HomeEffect.NavigateToDonate -> onOpenDonate()
                HomeEffect.NavigateToFeedback -> onOpenFeedback()
                HomeEffect.NavigateToGuide -> onOpenGuide()
                HomeEffect.NavigateToPlants -> onOpenPlants()
            }
        }
    }

    TodayScreen(
        uiState = uiState,
        onEvent = homeViewModel::onEvent,
        onHowToUseClick = { homeViewModel.onEvent(TodayEvent.HowToUseClicked) },
        onFeedbackClick = { homeViewModel.onEvent(TodayEvent.FeedbackClicked) },
        onDonateClick = { homeViewModel.onEvent(TodayEvent.DonateClicked) },
    )
}

@Composable
fun GuideRoute(
    onBack: () -> Unit,
) {
    HowToUseScreen(onBack = onBack)
}

@Composable
fun DonateRoute(
    onBack: () -> Unit,
    donateViewModel: DonateViewModel = viewModel(),
) {
    val uiState by donateViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DonateScreen(
        uiState = uiState,
        onBack = onBack,
        onSupportTierClick = { productId ->
            donateViewModel.onSupportTierClicked(productId, context.findActivity())
        },
        onRetryClick = donateViewModel::onRetryClicked,
    )
}

@Composable
fun FeedbackRoute(
    onBack: () -> Unit,
    feedbackViewModel: FeedbackViewModel = viewModel(),
) {
    val uiState by feedbackViewModel.uiState.collectAsStateWithLifecycle()

    FeedbackScreen(
        uiState = uiState,
        onEvent = feedbackViewModel::onEvent,
        onBack = onBack,
    )
}

@Composable
fun PlantsRoute(
    onAddPlant: () -> Unit,
    onOpenAiCare: () -> Unit,
    onOpenPlantScanner: () -> Unit,
    onOpenPlant: (String) -> Unit,
    onEditPlant: (String) -> Unit,
    pendingSuccessMessage: String? = null,
    onPendingSuccessMessageConsumed: () -> Unit = {},
    plantsViewModel: PlantsViewModel = viewModel(),
) {
    val uiState by plantsViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pendingSuccessMessage) {
        if (pendingSuccessMessage != null) {
            plantsViewModel.showSuccessMessage(pendingSuccessMessage)
            onPendingSuccessMessageConsumed()
        }
    }

    LaunchedEffect(plantsViewModel) {
        plantsViewModel.effects.collect { effect ->
            when (effect) {
                PlantsEffect.NavigateToAddPlant -> onAddPlant()
                PlantsEffect.NavigateToAiPlantCare -> onOpenAiCare()
                PlantsEffect.NavigateToPlantScanner -> onOpenPlantScanner()
                is PlantsEffect.NavigateToPlantDetails -> onOpenPlant(effect.plantId)
                is PlantsEffect.NavigateToEditPlant -> onEditPlant(effect.plantId)
            }
        }
    }

    PlantsScreen(
        uiState = uiState,
        onEvent = plantsViewModel::onEvent,
    )
}

@Composable
fun AiPlantCareRoute(
    onBack: () -> Unit,
    aiPlantCareViewModel: AiPlantCareViewModel = viewModel(),
) {
    val uiState by aiPlantCareViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(aiPlantCareViewModel) {
        aiPlantCareViewModel.effects.collect { effect ->
            when (effect) {
                AiPlantCareEffect.NavigateBack -> onBack()
            }
        }
    }

    AiPlantCareScreen(
        uiState = uiState,
        onEvent = aiPlantCareViewModel::onEvent,
    )
}

@Composable
fun PlantScannerRoute(
    onBack: () -> Unit,
    onSaveToPlants: (String, String) -> Unit,
    plantScannerViewModel: PlantScannerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by plantScannerViewModel.uiState.collectAsStateWithLifecycle()
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            cropSourceUri = uri
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) {
            cameraUri?.let { cropSourceUri = it }
        }
        cameraUri = null
    }

    LaunchedEffect(plantScannerViewModel) {
        plantScannerViewModel.effects.collect { effect ->
            when (effect) {
                PlantScannerEffect.NavigateBack -> onBack()
                is PlantScannerEffect.NavigateToAddPlant -> onSaveToPlants(effect.name, effect.photoUri)
                PlantScannerEffect.OpenGallery -> {
                    runCatching { galleryPicker.launch(arrayOf("image/*")) }
                        .onFailure {
                            plantScannerViewModel.onEvent(
                                PlantScannerEvent.PhotoLaunchFailed("WaterMe could not open the gallery."),
                            )
                        }
                }
                PlantScannerEffect.OpenCamera -> {
                    runCatching { createPlantCameraImageUri(context) }
                        .onSuccess { uri ->
                            cameraUri = uri
                            runCatching { cameraLauncher.launch(uri) }
                                .onFailure {
                                    plantScannerViewModel.onEvent(
                                        PlantScannerEvent.PhotoLaunchFailed("WaterMe could not open the camera."),
                                    )
                                }
                        }
                        .onFailure {
                            plantScannerViewModel.onEvent(
                                PlantScannerEvent.PhotoLaunchFailed("WaterMe could not open the camera."),
                            )
                        }
                }
            }
        }
    }

    cropSourceUri?.let { sourceUri ->
        PlantPhotoCropDialog(
            sourceUri = sourceUri,
            onPhotoCropped = { croppedUri ->
                cropSourceUri = null
                plantScannerViewModel.onEvent(PlantScannerEvent.ImageSelected(croppedUri))
            },
            onDismiss = { cropSourceUri = null },
        )
    }

    PlantScannerScreen(
        uiState = uiState,
        onEvent = plantScannerViewModel::onEvent,
    )
}

@Composable
fun AddPlantRoute(
    onBack: () -> Unit,
    onOpenPhotoPicker: () -> Unit,
    onPlantCreated: (String) -> Unit,
    prefillName: String? = null,
    prefillPhotoUri: String? = null,
    addPlantViewModel: AddPlantViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by addPlantViewModel.uiState.collectAsStateWithLifecycle()
    val openPlantPhotoPicker = rememberPlantPhotoPicker(
        context = context,
        onOpenPhotoPicker = onOpenPhotoPicker,
        onPhotoSelected = addPlantViewModel::onPhotoSelected,
    )

    LaunchedEffect(prefillName, prefillPhotoUri) {
        addPlantViewModel.applyPrefill(
            name = prefillName,
            photoUri = prefillPhotoUri,
        )
    }

    LaunchedEffect(addPlantViewModel) {
        addPlantViewModel.effects.collect { effect ->
            when (effect) {
                AddPlantEffect.NavigateBack -> onBack()
                AddPlantEffect.OpenPhotoPicker -> openPlantPhotoPicker()
                is AddPlantEffect.NavigateToPlantDetails -> onPlantCreated(effect.plantId)
            }
        }
    }

    AddPlantScreen(
        uiState = uiState,
        onEvent = addPlantViewModel::onEvent,
    )
}

@Composable
fun EditPlantRoute(
    onBack: () -> Unit,
    onPlantUpdated: (String) -> Unit,
    onPlantDeleted: () -> Unit,
    onOpenPhotoPicker: () -> Unit,
    editPlantViewModel: EditPlantViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by editPlantViewModel.uiState.collectAsStateWithLifecycle()
    val openPlantPhotoPicker = rememberPlantPhotoPicker(
        context = context,
        onOpenPhotoPicker = onOpenPhotoPicker,
        onPhotoSelected = editPlantViewModel::onPhotoSelected,
    )

    LaunchedEffect(editPlantViewModel) {
        editPlantViewModel.effects.collect { effect ->
            when (effect) {
                EditPlantEffect.NavigateBack -> onBack()
                EditPlantEffect.OpenPhotoPicker -> openPlantPhotoPicker()
                is EditPlantEffect.NavigateToPlantDetails -> onPlantUpdated(effect.plantId)
                EditPlantEffect.NavigateToPlantsAfterDelete -> onPlantDeleted()
            }
        }
    }

    EditPlantScreen(
        uiState = uiState,
        onEvent = editPlantViewModel::onEvent,
    )
}

@Composable
fun PlantDetailsRoute(
    onBack: () -> Unit,
    onEditPlant: (String) -> Unit,
    onViewHistory: (String) -> Unit,
    onPlantDeleted: () -> Unit,
    plantDetailsViewModel: PlantDetailsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by plantDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val openHealthNotePhotoPicker = rememberPlantPhotoPicker(
        context = context,
        onOpenPhotoPicker = {},
        onPhotoSelected = plantDetailsViewModel::onHealthNotePhotoSelected,
    )

    LaunchedEffect(plantDetailsViewModel) {
        plantDetailsViewModel.effects.collect { effect ->
            when (effect) {
                PlantDetailsEffect.NavigateBack -> onBack()
                PlantDetailsEffect.NavigateToPlantsAfterDelete -> onPlantDeleted()
                PlantDetailsEffect.OpenHealthNotePhotoPicker -> openHealthNotePhotoPicker()
                is PlantDetailsEffect.NavigateToEditPlant -> onEditPlant(effect.plantId)
                is PlantDetailsEffect.NavigateToCareHistory -> onViewHistory(effect.plantId)
            }
        }
    }

    PlantDetailsScreen(
        uiState = uiState,
        onEvent = plantDetailsViewModel::onEvent,
    )
}

@Composable
fun CalendarRoute(
    onOpenPlant: (String) -> Unit,
    onScrollToToday: () -> Unit,
    calendarViewModel: CalendarViewModel = viewModel(),
) {
    val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(calendarViewModel) {
        calendarViewModel.effects.collect { effect ->
            when (effect) {
                CalendarEffect.ScrollToToday -> onScrollToToday()
                is CalendarEffect.NavigateToPlantDetails -> onOpenPlant(effect.plantId)
            }
        }
    }

    CalendarScreen(
        uiState = uiState,
        onEvent = calendarViewModel::onEvent,
    )
}

@Composable
fun CareHistoryRoute(
    onBack: () -> Unit,
    careHistoryViewModel: CareHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by careHistoryViewModel.uiState.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        careHistoryViewModel.onPhotoSelected(uri?.toString())
    }

    LaunchedEffect(careHistoryViewModel) {
        careHistoryViewModel.effects.collect { effect ->
            when (effect) {
                CareHistoryEffect.NavigateBack -> onBack()
                CareHistoryEffect.OpenPhotoPicker -> photoPicker.launch(arrayOf("image/*"))
            }
        }
    }

    CareHistoryScreen(
        uiState = uiState,
        onEvent = careHistoryViewModel::onEvent,
    )
}

@Composable
fun SettingsRoute(
    onOpenFeedback: () -> Unit,
    onOpenLegal: (LegalDocument) -> Unit,
    onOpenCharacters: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        settingsViewModel.onNotificationPermissionResult(granted)
    }

    LaunchedEffect(settingsViewModel) {
        settingsViewModel.effects.collect { effect ->
            when (effect) {
                SettingsEffect.NavigateToFeedback -> onOpenFeedback()
                is SettingsEffect.NavigateToLegal -> onOpenLegal(effect.document)
                SettingsEffect.RequestNotificationPermission ->
                    notificationPermissionLauncher.launch(NotificationPermissionHelper.permission)
                SettingsEffect.OpenNotificationSettings -> context.openAppNotificationSettings()
                SettingsEffect.NavigateToCharacters -> onOpenCharacters()
            }
        }
    }

    SettingsScreen(
        uiState = uiState,
        onEvent = settingsViewModel::onEvent,
    )
}

@Composable
fun CharactersRoute(
    onBack: () -> Unit,
    charactersViewModel: CharactersViewModel = viewModel(),
) {
    val uiState by charactersViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(charactersViewModel) {
        charactersViewModel.effects.collect { effect ->
            when (effect) {
                CharactersEffect.NavigateBack -> onBack()
            }
        }
    }

    CharactersScreen(
        uiState = uiState,
        onEvent = charactersViewModel::onEvent,
    )
}

@Composable
private fun rememberPlantPhotoPicker(
    context: Context,
    onOpenPhotoPicker: () -> Unit,
    onPhotoSelected: (String?) -> Unit,
): () -> Unit {
    var showSourceDialog by remember { mutableStateOf(false) }
    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            cropSourceUri = uri
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) {
            cameraUri?.let { cropSourceUri = it }
        }
        cameraUri = null
    }

    if (showSourceDialog) {
        PlantPhotoSourceDialog(
            onTakePhoto = {
                showSourceDialog = false
                runCatching { createPlantCameraImageUri(context) }
                    .onSuccess { uri ->
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    }
            },
            onChooseImage = {
                showSourceDialog = false
                galleryPicker.launch(arrayOf("image/*"))
            },
            onDismiss = { showSourceDialog = false },
        )
    }

    cropSourceUri?.let { sourceUri ->
        PlantPhotoCropDialog(
            sourceUri = sourceUri,
            onPhotoCropped = { croppedUri ->
                cropSourceUri = null
                onPhotoSelected(croppedUri)
            },
            onDismiss = { cropSourceUri = null },
        )
    }

    return {
        onOpenPhotoPicker()
        showSourceDialog = true
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}
