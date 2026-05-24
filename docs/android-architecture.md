# WaterMe Android Architecture

This document defines the complete Android app architecture for WaterMe using Kotlin, Jetpack Compose, MVVM, Room, WorkManager, Hilt, Navigation Compose, and Material 3.

The design follows Android's recommended layered architecture:

- UI layer renders state and sends user events.
- ViewModels hold screen state and coordinate user actions.
- Domain layer contains reusable business rules.
- Data layer owns repositories, Room, local file/photo storage, and background scheduling.
- Background and notification components run durable reminder work outside the visible UI.

Official Android references used for this architecture:

- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [UI layer](https://developer.android.com/topic/architecture/ui-layer)
- [Data layer](https://developer.android.com/topic/architecture/data-layer)
- [Navigation with Compose](https://developer.android.com/develop/ui/compose/navigation)
- [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager/index)

## Architectural Goals

- Use a single-activity Compose app.
- Keep UI declarative and stateless where practical.
- Use unidirectional data flow:
  - UI event -> ViewModel -> repository or use case -> data source -> Flow -> UI state.
- Keep Room as the local source of truth.
- Keep reminders durable through app restarts with WorkManager.
- Keep notification code outside composables and ViewModels.
- Keep Hilt dependency boundaries explicit and testable.
- Prefer feature packages for screens and common packages for shared app infrastructure.

## Recommended Project Structure

```text
WaterMe/
  app/
    build.gradle.kts
    src/main/
      AndroidManifest.xml
      java/com/hotelski/waterme/
        WaterMeApplication.kt
        MainActivity.kt

        core/
          common/
            AppDispatchers.kt
            CoroutineDispatchers.kt
            Result.kt
            TimeProvider.kt
            UuidProvider.kt
          designsystem/
            component/
              WaterMeButton.kt
              WaterMeCard.kt
              WaterMeEmptyState.kt
              WaterMePlantPhoto.kt
              WaterMeReminderChip.kt
              WaterMeTopAppBar.kt
            icon/
              WaterMeIcons.kt
            theme/
              Color.kt
              Shape.kt
              Theme.kt
              Type.kt
          navigation/
            WaterMeDestination.kt
            WaterMeNavGraph.kt
            WaterMeNavigator.kt
            TopLevelDestination.kt
          notifications/
            CareNotificationManager.kt
            NotificationChannels.kt
            NotificationPermissionManager.kt
          worker/
            CareNotificationWorker.kt
            ReminderReconciliationWorker.kt
            WaterMeWorkerFactory.kt

        data/
          local/
            WaterMeDatabase.kt
            WaterMeTypeConverters.kt
            dao/
              CareHistoryDao.kt
              CareTaskDao.kt
              PlantDao.kt
              PlantPhotoDao.kt
              ReminderDao.kt
              UserDao.kt
              UserSettingsDao.kt
            entity/
              CareHistoryEntity.kt
              CareTaskEntity.kt
              PlantEntity.kt
              PlantPhotoEntity.kt
              ReminderEntity.kt
              UserEntity.kt
              UserSettingsEntity.kt
            relation/
              PlantDetailsRelation.kt
              PlantListItemRelation.kt
              TaskWithPlantRelation.kt
          mapper/
            CareHistoryMapper.kt
            CareTaskMapper.kt
            PlantMapper.kt
            ReminderMapper.kt
            SettingsMapper.kt
          photo/
            PlantPhotoStorage.kt
          repository/
            CareRepositoryImpl.kt
            PlantRepositoryImpl.kt
            ReminderRepositoryImpl.kt
            SettingsRepositoryImpl.kt
            UserRepositoryImpl.kt
          scheduler/
            ReminderSchedulerImpl.kt

        di/
          AppModule.kt
          DatabaseModule.kt
          DispatcherModule.kt
          NotificationModule.kt
          RepositoryModule.kt
          SchedulerModule.kt

        domain/
          model/
            CareHistory.kt
            CareTask.kt
            CareType.kt
            Plant.kt
            PlantPhoto.kt
            Reminder.kt
            User.kt
            UserSettings.kt
          repository/
            CareRepository.kt
            PlantRepository.kt
            ReminderRepository.kt
            SettingsRepository.kt
            UserRepository.kt
          usecase/
            AddPlantUseCase.kt
            CompleteCareTaskUseCase.kt
            DeletePlantUseCase.kt
            GenerateCareTasksUseCase.kt
            GetCalendarTasksUseCase.kt
            GetTodayTasksUseCase.kt
            ScheduleReminderNotificationsUseCase.kt
            SkipCareTaskUseCase.kt
            SnoozeCareTaskUseCase.kt
            UpdatePlantUseCase.kt
            UpdateUserSettingsUseCase.kt

        feature/
          addplant/
            AddPlantRoute.kt
            AddPlantScreen.kt
            AddPlantUiEvent.kt
            AddPlantUiState.kt
            AddPlantViewModel.kt
          calendar/
            CalendarRoute.kt
            CalendarScreen.kt
            CalendarUiEvent.kt
            CalendarUiState.kt
            CalendarViewModel.kt
          onboarding/
            OnboardingRoute.kt
            OnboardingScreen.kt
            OnboardingUiState.kt
            OnboardingViewModel.kt
          plantdetails/
            PlantDetailsRoute.kt
            PlantDetailsScreen.kt
            PlantDetailsUiEvent.kt
            PlantDetailsUiState.kt
            PlantDetailsViewModel.kt
          plants/
            PlantsRoute.kt
            PlantsScreen.kt
            PlantsUiEvent.kt
            PlantsUiState.kt
            PlantsViewModel.kt
          settings/
            SettingsRoute.kt
            SettingsScreen.kt
            SettingsUiEvent.kt
            SettingsUiState.kt
            SettingsViewModel.kt
          today/
            TodayRoute.kt
            TodayScreen.kt
            TodayUiEvent.kt
            TodayUiState.kt
            TodayViewModel.kt
```

## Layer Responsibilities

### `core`

Shared infrastructure that has no WaterMe feature ownership.

- `common`: coroutine dispatchers, time provider, UUID provider, result wrappers.
- `designsystem`: Material 3 theme, shared colors, typography, shapes, and reusable composables.
- `navigation`: app destinations, top-level navigation metadata, graph setup.
- `notifications`: notification channels, runtime permission state helper, local notification display.
- `worker`: WorkManager workers that run reminder and notification jobs.

### `data`

Owns persistence, local storage, and external Android data APIs.

- Room entities and DAOs live in `data/local`.
- Repositories translate DAO data into domain models.
- Mappers keep Room entities out of ViewModels.
- Photo storage copies selected images into app-owned storage and stores URI references.
- Scheduler implementation wraps WorkManager.

### `domain`

Contains app business language and reusable logic.

- Domain models are independent from Room annotations.
- Repository interfaces define what the app can do.
- Use cases combine multiple repositories or enforce important rules.
- Domain code should avoid Android SDK types unless a boundary requires them.

### `feature`

Owns screen UI, screen ViewModels, screen state, and user events.

- Each feature has a route composable that connects Navigation Compose to a ViewModel.
- Screen composables receive state and callbacks only.
- ViewModels expose immutable `StateFlow<UiState>`.
- ViewModels accept user events through methods or `onEvent(event)`.

### `di`

Hilt modules that bind implementations to interfaces and provide singleton resources.

- Database and DAO providers are singleton scoped.
- Repositories are singleton scoped.
- ViewModels use `@HiltViewModel`.
- Workers use Hilt WorkManager integration.

## Gradle Dependencies

Recommended app dependencies:

```kotlin
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
}
```

Use KSP for Room and Hilt annotation processing.

## App Entry Points

### Application

```kotlin
@HiltAndroidApp
class WaterMeApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

### Activity

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WaterMeApp()
        }
    }
}
```

## Navigation Graph

Use one `NavHost` in `MainActivity`. Pass only primitive route arguments such as IDs. Do not pass full plant or task objects through navigation.

### Destinations

```kotlin
sealed interface WaterMeDestination {
    val route: String

    data object Onboarding : WaterMeDestination {
        override val route = "onboarding"
    }

    data object Today : WaterMeDestination {
        override val route = "today"
    }

    data object Plants : WaterMeDestination {
        override val route = "plants"
    }

    data object Calendar : WaterMeDestination {
        override val route = "calendar"
    }

    data object Settings : WaterMeDestination {
        override val route = "settings"
    }

    data object AddPlant : WaterMeDestination {
        override val route = "plants/add"
    }

    data object PlantDetails : WaterMeDestination {
        override val route = "plants/{plantId}"
        fun createRoute(plantId: String) = "plants/$plantId"
    }
}
```

### Top-Level Destinations

```kotlin
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
) {
    Today("today", "Today", Icons.Rounded.Home),
    Plants("plants", "Plants", Icons.Rounded.LocalFlorist),
    Calendar("calendar", "Calendar", Icons.Rounded.Event),
    Settings("settings", "Settings", Icons.Rounded.Settings),
}
```

### NavHost

```kotlin
@Composable
fun WaterMeNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(WaterMeDestination.Onboarding.route) {
            OnboardingRoute(
                onFinished = {
                    navController.navigate(WaterMeDestination.Today.route) {
                        popUpTo(WaterMeDestination.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

        composable(WaterMeDestination.Today.route) {
            TodayRoute(
                onAddPlant = { navController.navigate(WaterMeDestination.AddPlant.route) },
                onPlantClick = { plantId ->
                    navController.navigate(WaterMeDestination.PlantDetails.createRoute(plantId))
                },
            )
        }

        composable(WaterMeDestination.Plants.route) {
            PlantsRoute(
                onAddPlant = { navController.navigate(WaterMeDestination.AddPlant.route) },
                onPlantClick = { plantId ->
                    navController.navigate(WaterMeDestination.PlantDetails.createRoute(plantId))
                },
            )
        }

        composable(WaterMeDestination.Calendar.route) {
            CalendarRoute(
                onTaskClick = { plantId ->
                    navController.navigate(WaterMeDestination.PlantDetails.createRoute(plantId))
                },
            )
        }

        composable(WaterMeDestination.Settings.route) {
            SettingsRoute(
                onShowOnboarding = {
                    navController.navigate(WaterMeDestination.Onboarding.route)
                },
            )
        }

        composable(WaterMeDestination.AddPlant.route) {
            AddPlantRoute(
                onBack = navController::popBackStack,
                onPlantSaved = { plantId ->
                    navController.navigate(WaterMeDestination.PlantDetails.createRoute(plantId)) {
                        popUpTo(WaterMeDestination.AddPlant.route) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = WaterMeDestination.PlantDetails.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val plantId = requireNotNull(backStackEntry.arguments?.getString("plantId"))
            PlantDetailsRoute(
                plantId = plantId,
                onBack = navController::popBackStack,
            )
        }
    }
}
```

### App Scaffold

```kotlin
@Composable
fun WaterMeApp(
    appState: WaterMeAppState = rememberWaterMeAppState(),
) {
    val settings by appState.settings.collectAsStateWithLifecycle()

    WaterMeTheme(themePreference = settings.themePreference) {
        WaterMeScaffold(
            navController = appState.navController,
            topLevelDestinations = TopLevelDestination.entries,
            currentDestination = appState.currentTopLevelDestination,
            onNavigateToTopLevel = appState::navigateToTopLevelDestination,
        ) { innerPadding ->
            WaterMeNavGraph(
                navController = appState.navController,
                startDestination = appState.startDestination,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
```

## App State Management

`WaterMeAppState` owns process-level UI state that does not belong to one screen.

Responsibilities:

- Own `NavHostController`.
- Resolve current top-level destination.
- Know whether bottom navigation should show.
- Observe user settings for theme and notification preferences.
- Provide navigation helpers.

```kotlin
@Stable
class WaterMeAppState(
    val navController: NavHostController,
    private val settingsRepository: SettingsRepository,
) {
    val settings: StateFlow<UserSettings> =
        settingsRepository.observeCurrentUserSettings()
            .stateIn(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = UserSettings.defaults(),
            )

    val startDestination: String
        get() = if (settings.value.onboardingCompleted) {
            WaterMeDestination.Today.route
        } else {
            WaterMeDestination.Onboarding.route
        }

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val route = backStackEntry?.destination?.route
            return TopLevelDestination.entries.firstOrNull { it.route == route }
        }

    fun navigateToTopLevelDestination(destination: TopLevelDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
```

For implementation, prefer creating `WaterMeAppState` through a composable that uses Hilt-provided dependencies from an entry-point or a root ViewModel. Keep direct repository calls out of most composables.

## MVVM Feature Pattern

Each screen follows the same structure:

```text
FeatureRoute
  gets ViewModel with hiltViewModel()
  collects StateFlow with collectAsStateWithLifecycle()
  handles one-off effects if needed
  passes state and event callbacks to FeatureScreen

FeatureScreen
  pure Compose UI
  no repository access
  no navigation controller access unless it is a tiny route wrapper

FeatureViewModel
  receives repositories/use cases through constructor injection
  exposes StateFlow<FeatureUiState>
  exposes functions for user events
```

Example:

```kotlin
@Composable
fun TodayRoute(
    onAddPlant: () -> Unit,
    onPlantClick: (String) -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TodayScreen(
        uiState = uiState,
        onAddPlant = onAddPlant,
        onPlantClick = onPlantClick,
        onCompleteTask = viewModel::completeTask,
        onSkipTask = viewModel::skipTask,
        onSnoozeTask = viewModel::snoozeTask,
    )
}
```

## UI State Classes

Use immutable UI state. Keep loading and empty state explicit.

### Shared UI Models

```kotlin
data class PlantCardUiModel(
    val id: String,
    val name: String,
    val plantType: String,
    val location: String,
    val primaryPhotoUri: String?,
    val dueTaskCount: Int,
    val nextTaskLabel: String?,
)

data class CareTaskUiModel(
    val id: String,
    val plantId: String,
    val plantName: String,
    val plantLocation: String,
    val careType: CareType,
    val careLabel: String,
    val dueLabel: String,
    val isOverdue: Boolean,
    val isSnoozed: Boolean,
)

data class CalendarDayUiModel(
    val dateLabel: String,
    val tasks: List<CareTaskUiModel>,
)
```

### Today UI State

```kotlin
data class TodayUiState(
    val isLoading: Boolean = true,
    val todayTasks: List<CareTaskUiModel> = emptyList(),
    val recentHealthNotes: List<HealthNoteUiModel> = emptyList(),
    val plantCount: Int = 0,
    val reminderCount: Int = 0,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && todayTasks.isEmpty()
}
```

### Plants UI State

```kotlin
data class PlantsUiState(
    val isLoading: Boolean = true,
    val plants: List<PlantCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && plants.isEmpty()
}
```

### Add Plant UI State

```kotlin
data class AddPlantUiState(
    val name: String = "",
    val plantType: String = "",
    val location: String = "",
    val notes: String = "",
    val selectedPhotoUri: String? = null,
    val reminders: List<ReminderDraftUiModel> = ReminderDraftUiModel.defaults(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving
}
```

### Plant Details UI State

```kotlin
data class PlantDetailsUiState(
    val isLoading: Boolean = true,
    val plant: PlantDetailsUiModel? = null,
    val reminders: List<ReminderUiModel> = emptyList(),
    val pendingTasks: List<CareTaskUiModel> = emptyList(),
    val careHistory: List<CareHistoryUiModel> = emptyList(),
    val healthNoteDraft: String = "",
    val selectedHealthMood: HealthMood = HealthMood.ATTENTION,
    val errorMessage: String? = null,
)
```

### Calendar UI State

```kotlin
data class CalendarUiState(
    val isLoading: Boolean = true,
    val visibleStartMillis: Long = 0L,
    val visibleEndMillis: Long = 0L,
    val days: List<CalendarDayUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && days.all { it.tasks.isEmpty() }
}
```

### Settings UI State

```kotlin
data class SettingsUiState(
    val isLoading: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notificationPermissionState: NotificationPermissionState = NotificationPermissionState.NOT_REQUESTED,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val measurementUnits: MeasurementUnits = MeasurementUnits.METRIC,
    val backupSyncEnabled: Boolean = false,
    val plantCount: Int = 0,
    val activeReminderCount: Int = 0,
    val careHistoryCount: Int = 0,
    val errorMessage: String? = null,
)
```

## ViewModels

### TodayViewModel

Responsibilities:

- Observe today's tasks.
- Observe summary counts.
- Complete, skip, and snooze tasks.
- Keep UI state updated through Flows.

```kotlin
@HiltViewModel
class TodayViewModel @Inject constructor(
    getTodayTasks: GetTodayTasksUseCase,
    private val completeCareTask: CompleteCareTaskUseCase,
    private val skipCareTask: SkipCareTaskUseCase,
    private val snoozeCareTask: SnoozeCareTaskUseCase,
) : ViewModel() {
    val uiState: StateFlow<TodayUiState> =
        getTodayTasks()
            .map { tasks -> TodayUiState(isLoading = false, todayTasks = tasks.toUiModels()) }
            .catch { emit(TodayUiState(isLoading = false, errorMessage = it.message)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun completeTask(taskId: String) {
        viewModelScope.launch { completeCareTask(taskId) }
    }

    fun skipTask(taskId: String) {
        viewModelScope.launch { skipCareTask(taskId) }
    }

    fun snoozeTask(taskId: String, minutes: Int) {
        viewModelScope.launch { snoozeCareTask(taskId, minutes) }
    }
}
```

### PlantsViewModel

Responsibilities:

- Observe all active plants.
- Support search/filter state.
- Navigate via route callback only, not directly.

```kotlin
@HiltViewModel
class PlantsViewModel @Inject constructor(
    plantRepository: PlantRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<PlantsUiState> =
        combine(plantRepository.observePlants(), searchQuery) { plants, query ->
            val filtered = plants.filter { it.name.contains(query, ignoreCase = true) }
            PlantsUiState(
                isLoading = false,
                plants = filtered.map { it.toPlantCardUiModel() },
                searchQuery = query,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlantsUiState())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
```

### AddPlantViewModel

Responsibilities:

- Hold form draft.
- Validate input.
- Save plant through `AddPlantUseCase`.
- Expose one-off saved event.

```kotlin
@HiltViewModel
class AddPlantViewModel @Inject constructor(
    private val addPlant: AddPlantUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddPlantUiState())
    val uiState: StateFlow<AddPlantUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AddPlantEffect>()
    val effects: SharedFlow<AddPlantEffect> = _effects.asSharedFlow()

    fun onEvent(event: AddPlantUiEvent) {
        when (event) {
            is AddPlantUiEvent.NameChanged -> update { copy(name = event.value) }
            is AddPlantUiEvent.PhotoSelected -> update { copy(selectedPhotoUri = event.uri) }
            is AddPlantUiEvent.ReminderChanged -> updateReminder(event.draft)
            AddPlantUiEvent.SaveClicked -> save()
        }
    }

    private fun save() {
        viewModelScope.launch {
            update { copy(isSaving = true) }
            val plantId = addPlant(uiState.value.toInput())
            _effects.emit(AddPlantEffect.PlantSaved(plantId))
        }
    }

    private fun update(block: AddPlantUiState.() -> AddPlantUiState) {
        _uiState.update(block)
    }
}
```

### PlantDetailsViewModel

Responsibilities:

- Observe plant details, reminders, history, and pending tasks.
- Complete, skip, snooze tasks.
- Add health note or manual care history.
- Delete or archive plant if that action is added.

### CalendarViewModel

Responsibilities:

- Observe upcoming tasks for a selected date range.
- Group tasks by local date.
- Trigger task generation if calendar horizon is missing.

### SettingsViewModel

Responsibilities:

- Observe settings.
- Update notification toggle.
- Persist notification permission state.
- Update dark mode preference.
- Update units.
- Update backup/sync preference.

## Repositories

Repository interfaces live in `domain/repository`. Implementations live in `data/repository`.

### PlantRepository

```kotlin
interface PlantRepository {
    fun observePlants(): Flow<List<Plant>>
    fun observePlantDetails(plantId: String): Flow<PlantDetails>
    suspend fun addPlant(input: AddPlantInput): String
    suspend fun updatePlant(plantId: String, input: EditPlantInput)
    suspend fun archivePlant(plantId: String)
    suspend fun deletePlant(plantId: String)
}
```

Implementation responsibilities:

- Write `PlantEntity`.
- Write `PlantPhotoEntity`.
- Write initial reminder rows.
- Ask `ReminderRepository` or use case to create initial tasks.
- Never return Room entities to ViewModels.

### CareRepository

```kotlin
interface CareRepository {
    fun observeTodayTasks(): Flow<List<CareTask>>
    fun observeUpcomingTasks(startMillis: Long, endMillis: Long): Flow<List<CareTask>>
    fun observeCareHistory(plantId: String): Flow<List<CareHistory>>
    suspend fun completeTask(taskId: String, note: String = "")
    suspend fun skipTask(taskId: String, note: String = "")
    suspend fun snoozeTask(taskId: String, minutes: Int)
    suspend fun logManualCare(plantId: String, careType: CareType, note: String)
}
```

Implementation responsibilities:

- Update `CareTaskEntity`.
- Insert `CareHistoryEntity`.
- Update `ReminderEntity.next_due_at`.
- Insert next generated `CareTaskEntity`.
- Ask scheduler to cancel or enqueue WorkManager work.

### ReminderRepository

```kotlin
interface ReminderRepository {
    fun observeReminders(plantId: String): Flow<List<Reminder>>
    suspend fun upsertReminder(input: ReminderInput): String
    suspend fun disableReminder(reminderId: String)
    suspend fun generateTasks(reminderId: String, horizonEndMillis: Long)
}
```

Implementation responsibilities:

- Maintain recurring rules.
- Generate concrete tasks for calendar and notifications.
- Cancel pending tasks when reminders are disabled.

### SettingsRepository

```kotlin
interface SettingsRepository {
    fun observeCurrentUserSettings(): Flow<UserSettings>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setNotificationPermissionState(state: NotificationPermissionState)
    suspend fun setThemePreference(preference: ThemePreference)
    suspend fun setMeasurementUnits(units: MeasurementUnits)
    suspend fun setBackupSync(enabled: Boolean, provider: BackupSyncProvider)
}
```

Implementation responsibilities:

- Own `UserSettingsEntity`.
- Cancel all scheduled notification work if notifications are disabled.
- Reschedule pending work when notifications are enabled and permission is granted.

## Use Cases

Use cases are small classes with one public `operator fun invoke`.

Recommended use cases:

- `AddPlantUseCase`: creates plant, photos, reminders, first tasks, and schedules notifications.
- `UpdatePlantUseCase`: updates profile, photos, reminders, and future tasks.
- `DeletePlantUseCase`: deletes or archives plant and cancels pending work.
- `GetTodayTasksUseCase`: returns due tasks joined to plant UI data.
- `GetCalendarTasksUseCase`: returns grouped calendar tasks.
- `CompleteCareTaskUseCase`: completes task, inserts history, creates next task, schedules next notification.
- `SkipCareTaskUseCase`: skips task, inserts history, creates next task.
- `SnoozeCareTaskUseCase`: updates effective due time and replaces WorkManager request.
- `GenerateCareTasksUseCase`: fills calendar horizon from reminders.
- `ScheduleReminderNotificationsUseCase`: schedules pending notification work.
- `UpdateUserSettingsUseCase`: applies settings side effects.

Example:

```kotlin
class CompleteCareTaskUseCase @Inject constructor(
    private val careRepository: CareRepository,
) {
    suspend operator fun invoke(taskId: String, note: String = "") {
        careRepository.completeTask(taskId, note)
    }
}
```

## Dependency Injection Setup

### Database Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WaterMeDatabase =
        Room.databaseBuilder(
            context,
            WaterMeDatabase::class.java,
            "waterme.db",
        )
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides fun providePlantDao(db: WaterMeDatabase): PlantDao = db.plantDao()
    @Provides fun provideReminderDao(db: WaterMeDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideCareTaskDao(db: WaterMeDatabase): CareTaskDao = db.careTaskDao()
    @Provides fun provideCareHistoryDao(db: WaterMeDatabase): CareHistoryDao = db.careHistoryDao()
    @Provides fun provideSettingsDao(db: WaterMeDatabase): UserSettingsDao = db.settingsDao()
}
```

### Repository Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPlantRepository(impl: PlantRepositoryImpl): PlantRepository

    @Binds
    @Singleton
    abstract fun bindCareRepository(impl: CareRepositoryImpl): CareRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
```

### Dispatcher Module

```kotlin
@Qualifier
annotation class IoDispatcher

@Qualifier
annotation class DefaultDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

### Notification Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    @Singleton
    fun provideCareNotificationManager(
        @ApplicationContext context: Context,
    ): CareNotificationManager = CareNotificationManager(context)
}
```

### Scheduler Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerModule {
    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: ReminderSchedulerImpl): ReminderScheduler
}
```

## Room And Data Flow

### Entity To Domain Rule

Do not expose Room entities outside the data layer.

```text
DAO returns Entity or Relation
Repository maps Entity to Domain Model
UseCase returns Domain Model
ViewModel maps Domain Model to UiModel
Composable renders UiModel
```

### Transaction Rule

Use Room transactions for operations that update multiple tables.

Operations that must be transactional:

- Add plant with photos, reminders, and tasks.
- Complete task with history, reminder update, and next task.
- Skip task with history, reminder update, and next task.
- Edit reminder with future task regeneration.
- Delete plant with task cancellation metadata.

Example:

```kotlin
@Transaction
suspend fun completeTaskTransaction(
    task: CareTaskEntity,
    history: CareHistoryEntity,
    nextTask: CareTaskEntity,
    nextDueAt: Long,
    now: Long,
) {
    careTaskDao.markCompleted(task.taskId, now, now)
    careHistoryDao.insertHistory(history)
    reminderDao.markReminderCompleted(task.reminderId, now, nextDueAt, now)
    careTaskDao.insertTask(nextTask)
}
```

## Background Reminder Scheduling

Use `CareTask` as the schedule source for notifications.

### Reminder Generation

```text
ReminderEntity recurring rule
  -> GenerateCareTasksUseCase creates CareTaskEntity rows
  -> ReminderScheduler schedules WorkManager notification requests
```

Recommended generation horizon:

- Generate initial task immediately when a reminder is created.
- Generate 90 days of tasks for calendar visibility.
- Run `ReminderReconciliationWorker` once per day to fill missing future tasks.

### Scheduler Interface

```kotlin
interface ReminderScheduler {
    suspend fun scheduleTask(taskId: String)
    suspend fun scheduleTasks(taskIds: List<String>)
    suspend fun cancelTask(taskId: String)
    suspend fun cancelReminder(reminderId: String)
    suspend fun cancelAll()
}
```

### WorkManager Scheduler Implementation

```kotlin
class ReminderSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val careTaskDao: CareTaskDao,
    private val settingsRepository: SettingsRepository,
    private val timeProvider: TimeProvider,
) : ReminderScheduler {
    override suspend fun scheduleTask(taskId: String) {
        val task = careTaskDao.getTask(taskId) ?: return
        val settings = settingsRepository.getCurrentUserSettings()

        if (!settings.notificationsEnabled) return
        if (settings.notificationPermissionState != NotificationPermissionState.GRANTED) return
        if (task.status !in listOf(CareTaskStatus.PENDING, CareTaskStatus.SNOOZED)) return

        val delayMillis = (task.effectiveDueAt - timeProvider.nowMillis()).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<CareNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(CareNotificationWorker.KEY_TASK_ID to task.taskId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            task.notificationWorkName,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
```

### Reconciliation Worker

Responsibilities:

- Find active reminders.
- Generate missing tasks for the next 90 days.
- Schedule notification work for pending tasks.
- Cancel work for completed, skipped, canceled, deleted, or disabled reminders.

```kotlin
@HiltWorker
class ReminderReconciliationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val generateCareTasks: GenerateCareTasksUseCase,
    private val scheduleReminderNotifications: ScheduleReminderNotificationsUseCase,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        generateCareTasks(horizonDays = 90)
        scheduleReminderNotifications()
        return Result.success()
    }
}
```

Schedule it as periodic daily work:

```kotlin
val request = PeriodicWorkRequestBuilder<ReminderReconciliationWorker>(
    repeatInterval = 1,
    repeatIntervalTimeUnit = TimeUnit.DAYS,
).build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "reminder_reconciliation",
    ExistingPeriodicWorkPolicy.UPDATE,
    request,
)
```

## Notification Manager

`CareNotificationManager` owns Android notification APIs.

Responsibilities:

- Create notification channels.
- Check runtime permission.
- Show care task notifications.
- Build deep link pending intents.
- Keep notification text consistent.

```kotlin
class CareNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CARE_CHANNEL_ID,
            "Plant care reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifications for watering, fertilizing, repotting, misting, and pruning."
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showCareReminder(task: CareTask, plant: Plant) {
        if (!canPostNotifications()) return

        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(
                Intent(
                    Intent.ACTION_VIEW,
                    "waterme://plants/${plant.id}/tasks/${task.id}".toUri(),
                    context,
                    MainActivity::class.java,
                ),
            )
            getPendingIntent(task.id.hashCode(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(context, CARE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_leaf)
            .setContentTitle("${plant.name} needs ${task.careType.label.lowercase()}")
            .setContentText("Open WaterMe to log care and keep your plant on track.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CARE_CHANNEL_ID = "plant_care_reminders"
    }
}
```

### Notification Worker

```kotlin
@HiltWorker
class CareNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val careRepository: CareRepository,
    private val notificationManager: CareNotificationManager,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val notificationData = careRepository.getNotificationData(taskId) ?: return Result.success()

        if (notificationData.task.status !in listOf(CareTaskStatus.PENDING, CareTaskStatus.SNOOZED)) {
            return Result.success()
        }

        notificationManager.showCareReminder(notificationData.task, notificationData.plant)
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
    }
}
```

## Dark Mode Support

Store theme preference in `UserSettings`.

```kotlin
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}
```

Theme resolver:

```kotlin
@Composable
fun WaterMeTheme(
    themePreference: ThemePreference,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) WaterMeDarkColorScheme else WaterMeLightColorScheme,
        typography = WaterMeTypography,
        shapes = WaterMeShapes,
        content = content,
    )
}
```

Light palette:

- Primary: `#2F7D4B`
- Secondary: `#79B987`
- Background: `#F6FBF7`
- Surface: `#FFFFFF`
- Surface variant: `#E7F1EA`
- Tertiary: `#B88C4A`

Dark palette:

- Primary: `#A8D9B5`
- Secondary: `#79B987`
- Background: `#101A14`
- Surface: `#17231B`
- Surface variant: `#26372D`
- Tertiary: `#E2C188`

Settings behavior:

- `SYSTEM`: follow Android system theme.
- `LIGHT`: force light Material 3 color scheme.
- `DARK`: force dark Material 3 color scheme.

## Reusable UI Components

Put reusable components in `core/designsystem/component`.

### `WaterMeScaffold`

Wraps top app bar, content, and bottom navigation.

Responsibilities:

- Apply app background.
- Show bottom navigation only for top-level destinations.
- Provide consistent content padding.

### `WaterMeTopAppBar`

Reusable Material 3 app bar.

Parameters:

- `title`
- `navigationIcon`
- `actions`

### `WaterMeNavigationBar`

Bottom navigation for compact Android phones.

Parameters:

- `destinations`
- `currentDestination`
- `onNavigate`

### `PlantPhoto`

Displays a selected plant photo or fallback icon.

Parameters:

- `photoUri`
- `plantName`
- `size`
- `shape`

### `PlantCard`

Shared plant card for Plants and related surfaces.

Parameters:

- `PlantCardUiModel`
- `onClick`

### `CareTaskCard`

Task card for Today.

Parameters:

- `CareTaskUiModel`
- `onComplete`
- `onSkip`
- `onSnooze`
- `onOpenPlant`

### `ReminderChip`

Small status chip for care type.

Parameters:

- `careType`
- `label`
- `selected`

### `WaterMeEmptyState`

Reusable empty state surface.

Parameters:

- `title`
- `message`
- optional `icon`
- optional `actionLabel`
- optional `onAction`

### `WaterMeErrorState`

Reusable error panel.

Parameters:

- `message`
- `retryLabel`
- `onRetry`

### `WaterMeLoadingContent`

Reusable loading skeleton or centered progress indicator.

## UI Events

Prefer sealed interfaces for screen events when a screen has many actions.

```kotlin
sealed interface TodayUiEvent {
    data class CompleteTask(val taskId: String) : TodayUiEvent
    data class SkipTask(val taskId: String) : TodayUiEvent
    data class SnoozeTask(val taskId: String, val minutes: Int) : TodayUiEvent
    data class OpenPlant(val plantId: String) : TodayUiEvent
    data object AddPlantClicked : TodayUiEvent
}
```

Simple screens can expose direct callbacks instead.

## One-Off Effects

Use `SharedFlow` for one-time events such as navigation after save, snackbars, or permission prompts.

```kotlin
sealed interface AddPlantEffect {
    data class PlantSaved(val plantId: String) : AddPlantEffect
    data class ShowSnackbar(val message: String) : AddPlantEffect
}
```

Do not store one-time navigation events in persistent UI state.

## Notification Permission Flow

1. `SettingsRepository` starts with `NOT_REQUESTED`.
2. Root app or Settings screen asks permission on Android 13+ when appropriate.
3. Permission result is stored in `UserSettings`.
4. If granted and notifications are enabled, schedule pending task notifications.
5. If denied, keep reminders and tasks active but do not schedule notification work.

```kotlin
fun onNotificationPermissionResult(granted: Boolean) {
    viewModelScope.launch {
        val state = if (granted) {
            NotificationPermissionState.GRANTED
        } else {
            NotificationPermissionState.DENIED
        }
        settingsRepository.setNotificationPermissionState(state)
        if (granted) {
            scheduleReminderNotifications()
        } else {
            reminderScheduler.cancelAll()
        }
    }
}
```

## Testing Strategy

### Unit Tests

Test:

- Use cases.
- Reminder recurrence calculations.
- Repository mapping.
- ViewModel UI state.
- Task complete, skip, and snooze behavior.

### Room Tests

Use in-memory Room database.

Test:

- Foreign key behavior.
- Today task queries.
- Calendar range queries.
- Plant detail relations.

### Worker Tests

Use WorkManager testing APIs.

Test:

- Notification worker exits when task is completed.
- Notification worker shows notification for pending task.
- Reconciliation worker generates missing tasks.

### Compose Tests

Test:

- Empty states.
- Add plant validation.
- Completing a task removes it from Today.
- Settings theme selection updates UI state.

## Migration Plan From Current Prototype

The current app can migrate incrementally:

1. Add Hilt, Room, Navigation Compose, WorkManager dependencies.
2. Add `WaterMeApplication` and Hilt setup.
3. Add Room entities, DAOs, database, and type converters from `docs/database-and-app-logic.md`.
4. Add repository interfaces and implementations.
5. Add use cases for add plant, complete task, skip task, snooze task, and schedule notifications.
6. Split the existing single Compose file into `feature/*` screens and `core/designsystem` components.
7. Replace in-memory or SharedPreferences state with repository `Flow` state.
8. Add WorkManager workers and notification manager.
9. Add Navigation Compose graph and route wrappers.
10. Add tests around recurrence, DAO queries, and ViewModels.

## Responsibility Summary

| Layer | Owns | Must Not Do |
| --- | --- | --- |
| Composables | Render UI and emit events | Query Room, schedule work, own business logic |
| Routes | Connect navigation, ViewModels, and screens | Hold complex UI logic |
| ViewModels | Screen state and user action coordination | Hold Android `Context`, show notifications directly |
| Use cases | Reusable business operations | Render UI |
| Repositories | Data operations and model mapping | Expose Room entities to UI |
| DAOs | SQL queries | Business decisions |
| Workers | Durable background work | Update visible UI directly |
| Notification manager | Android notification APIs | Decide business eligibility alone |
| Hilt modules | Dependency graph | Contain feature logic |
