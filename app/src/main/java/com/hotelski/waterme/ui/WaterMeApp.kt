package com.hotelski.waterme.ui

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.R
import com.hotelski.waterme.feature.common.icon
import com.hotelski.waterme.model.CareHistoryEntry
import com.hotelski.waterme.model.CareReminder
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.HealthMood
import com.hotelski.waterme.model.HealthNote
import com.hotelski.waterme.model.Plant
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.FreshGreen
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.SoftCream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterMeApp(
    plants: List<Plant>,
    onboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit,
    onPlantsChange: (List<Plant>) -> Unit,
) {
    var showOnboarding by remember { mutableStateOf(!onboardingComplete) }
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var selectedPlantId by remember { mutableStateOf<String?>(null) }
    var addingPlant by remember { mutableStateOf(false) }

    fun updatePlant(updatedPlant: Plant) {
        onPlantsChange(plants.map { plant -> if (plant.id == updatedPlant.id) updatedPlant else plant })
    }

    fun completeCare(plant: Plant, reminder: CareReminder) {
        val today = LocalDate.now()
        val updatedReminder = reminder.copy(nextDueDate = today.plusDays(reminder.frequencyDays.toLong()))
        val updatedPlant = plant.copy(
            reminders = plant.reminders.map { if (it.id == reminder.id) updatedReminder else it },
            careHistory = listOf(
                CareHistoryEntry(
                    type = reminder.type,
                    date = today,
                    note = "${reminder.type.label} completed.",
                ),
            ) + plant.careHistory,
        )
        updatePlant(updatedPlant)
    }

    fun addHealthNote(plant: Plant, note: String, mood: HealthMood) {
        val updatedPlant = plant.copy(
            healthNotes = listOf(
                HealthNote(
                    date = LocalDate.now(),
                    mood = mood,
                    note = note.trim(),
                ),
            ) + plant.healthNotes,
        )
        updatePlant(updatedPlant)
    }

    if (showOnboarding) {
        OnboardingScreen(
            onStart = {
                onOnboardingComplete()
                showOnboarding = false
            },
        )
        return
    }

    if (addingPlant) {
        AddPlantScreen(
            onBack = { addingPlant = false },
            onSave = { plant ->
                onPlantsChange(plants + plant)
                selectedTab = MainTab.Plants
                selectedPlantId = plant.id
                addingPlant = false
            },
        )
        return
    }

    val selectedPlant = selectedPlantId?.let { id -> plants.firstOrNull { it.id == id } }
    if (selectedPlant != null) {
        PlantDetailsScreen(
            plant = selectedPlant,
            onBack = { selectedPlantId = null },
            onCompleteCare = { reminder -> completeCare(selectedPlant, reminder) },
            onAddHealthNote = { note, mood -> addHealthNote(selectedPlant, note, mood) },
        )
        return
    }

    Scaffold(
        containerColor = GardenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = selectedTab.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = GardenBackground,
                    titleContentColor = Ink,
                ),
            )
        },
        floatingActionButton = {
            if (selectedTab == MainTab.Home || selectedTab == MainTab.Plants) {
                FloatingActionButton(
                    onClick = { addingPlant = true },
                    containerColor = LeafGreen,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add plant")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardWhite,
                tonalElevation = 10.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            selectedPlantId = null
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Home -> HomeScreen(
                plants = plants,
                contentPadding = innerPadding,
                onPlantClick = { selectedPlantId = it.id },
                onCompleteCare = ::completeCare,
            )

            MainTab.Plants -> MyPlantsScreen(
                plants = plants,
                contentPadding = innerPadding,
                onPlantClick = { selectedPlantId = it.id },
                onAddPlant = { addingPlant = true },
            )

            MainTab.Calendar -> CalendarScreen(
                plants = plants,
                contentPadding = innerPadding,
                onPlantClick = { selectedPlantId = it.id },
            )

            MainTab.Settings -> SettingsScreen(
                plants = plants,
                contentPadding = innerPadding,
                onShowOnboarding = { showOnboarding = true },
            )
        }
    }
}

@Composable
private fun OnboardingScreen(onStart: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(GardenBackground),
        color = GardenBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFFDFF2E6)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_waterme_logo),
                    contentDescription = "WaterMe logo",
                    modifier = Modifier.size(104.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "WaterMe",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "A calm plant care assistant for watering, feeding, pruning, repotting, and spotting small health changes before they become big ones.",
                modifier = Modifier.padding(top = 14.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedInk,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OnboardingPill("Care tasks", Modifier.weight(1f))
                OnboardingPill("Health notes", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OnboardingPill("Calendar", Modifier.weight(1f))
                OnboardingPill("Notifications", Modifier.weight(1f))
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
            ) {
                Text("Start caring for plants", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun OnboardingPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = LeafGreen,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeScreen(
    plants: List<Plant>,
    contentPadding: PaddingValues,
    onPlantClick: (Plant) -> Unit,
    onCompleteCare: (Plant, CareReminder) -> Unit,
) {
    val today = LocalDate.now()
    val todayTasks = plants
        .flatMap { plant -> plant.dueReminders(today).map { PlantTask(plant, it) } }
        .sortedBy { it.reminder.nextDueDate }
    val healthNotes = plants
        .flatMap { plant -> plant.healthNotes.take(1).map { plant to it } }
        .sortedByDescending { it.second.date }
        .take(3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroSummaryCard(
                title = if (todayTasks.isEmpty()) "All clear today" else "${todayTasks.size} care task${if (todayTasks.size == 1) "" else "s"} today",
                subtitle = if (todayTasks.isEmpty()) {
                    "Your plants are on schedule. Check the calendar for what is coming next."
                } else {
                    "A few plants need attention. Log care as you go and WaterMe will move the schedule forward."
                },
                plants = plants,
            )
        }

        item { SectionTitle("Today's Care") }

        if (todayTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No urgent care",
                    body = "Enjoy the quiet. Your next care task is waiting in the calendar.",
                )
            }
        } else {
            items(
                items = todayTasks,
                key = { "${it.plant.id}-${it.reminder.id}" },
            ) { task ->
                CareTaskCard(
                    task = task,
                    onOpenPlant = { onPlantClick(task.plant) },
                    onComplete = { onCompleteCare(task.plant, task.reminder) },
                )
            }
        }

        item { SectionTitle("Health Notes") }

        if (healthNotes.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "No notes yet",
                    body = "Use plant details to track yellow leaves, dry soil, fresh growth, or anything worth remembering.",
                )
            }
        } else {
            items(
                items = healthNotes,
                key = { "${it.first.id}-${it.second.id}" },
            ) { (plant, note) ->
                HealthNoteCard(
                    plant = plant,
                    note = note,
                    onClick = { onPlantClick(plant) },
                )
            }
        }
    }
}

@Composable
private fun MyPlantsScreen(
    plants: List<Plant>,
    contentPadding: PaddingValues,
    onPlantClick: (Plant) -> Unit,
    onAddPlant: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${plants.size} plants",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = "Care schedules, notes, and photos in one place.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                    )
                }
                FilledTonalButton(onClick = onAddPlant) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add")
                }
            }
        }

        if (plants.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Add your first plant",
                    body = "Create a care schedule with reminders and notes.",
                )
            }
        } else {
            items(items = plants, key = { it.id }) { plant ->
                PlantCard(
                    plant = plant,
                    onClick = { onPlantClick(plant) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlantScreen(
    onBack: () -> Unit,
    onSave: (Plant) -> Unit,
) {
    val today = LocalDate.now()
    var name by remember { mutableStateOf("") }
    var plantType by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var reminderDrafts by remember {
        mutableStateOf(
            CareType.entries.associateWith { type ->
                ReminderDraft(
                    enabled = type == CareType.WATERING || type == CareType.MISTING,
                    frequencyDays = type.defaultFrequencyDays.toString(),
                    startInDays = if (type == CareType.WATERING) "0" else type.defaultFrequencyDays.toString(),
                )
            },
        )
    }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        selectedPhotoUri = uri
    }

    Scaffold(
        containerColor = GardenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Plant", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GardenBackground),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        PlantPhoto(
                            photoUri = selectedPhotoUri?.toString(),
                            plantName = name.ifBlank { "New plant" },
                            size = 92.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Plant profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            Text(
                                text = "Add the details that help you recognize and care for it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedInk,
                            )
                            TextButton(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Choose photo")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Plant name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = plantType,
                        onValueChange = { plantType = it },
                        label = { Text("Plant type") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                }
            }

            SectionTitle("Care Reminders")

            CareType.entries.forEach { careType ->
                ReminderDraftCard(
                    careType = careType,
                    draft = reminderDrafts.getValue(careType),
                    onDraftChange = { draft ->
                        reminderDrafts = reminderDrafts.toMutableMap().also { it[careType] = draft }
                    },
                )
            }

            Button(
                onClick = {
                    val reminders = reminderDrafts
                        .filterValues { it.enabled }
                        .map { (type, draft) ->
                            CareReminder(
                                type = type,
                                frequencyDays = draft.frequencyDays.toPositiveFrequency(type.defaultFrequencyDays),
                                nextDueDate = today.plusDays(draft.startInDays.toNonNegativeInt(0).toLong()),
                            )
                        }

                    onSave(
                        Plant(
                            name = name.trim(),
                            type = plantType.trim().ifBlank { "Houseplant" },
                            location = location.trim().ifBlank { "Unassigned" },
                            notes = notes.trim(),
                            photoUri = selectedPhotoUri?.toString(),
                            reminders = reminders,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
            ) {
                Text("Save plant", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDetailsScreen(
    plant: Plant,
    onBack: () -> Unit,
    onCompleteCare: (CareReminder) -> Unit,
    onAddHealthNote: (String, HealthMood) -> Unit,
) {
    Scaffold(
        containerColor = GardenBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = plant.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = GardenBackground),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { PlantDetailHeader(plant) }

            item { SectionTitle("Reminders") }

            if (plant.reminders.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No reminders",
                        body = "Add reminders when editing support is expanded.",
                    )
                }
            } else {
                items(items = plant.reminders, key = { it.id }) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onComplete = { onCompleteCare(reminder) },
                    )
                }
            }

            item { SectionTitle("Health Notes") }

            item {
                HealthNoteComposer(onAdd = onAddHealthNote)
            }

            if (plant.healthNotes.isNotEmpty()) {
                items(items = plant.healthNotes, key = { it.id }) { note ->
                    HealthNoteCard(
                        plant = plant,
                        note = note,
                        onClick = {},
                    )
                }
            }

            item { SectionTitle("Care History") }

            if (plant.careHistory.isEmpty()) {
                item {
                    EmptyStateCard(
                        title = "No care logged",
                        body = "Tap Done on a reminder to build this plant's history.",
                    )
                }
            } else {
                items(items = plant.careHistory, key = { it.id }) { entry ->
                    HistoryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    plants: List<Plant>,
    contentPadding: PaddingValues,
    onPlantClick: (Plant) -> Unit,
) {
    val tasks = plants
        .flatMap { plant -> plant.reminders.filter { it.enabled }.map { PlantTask(plant, it) } }
        .sortedBy { it.reminder.nextDueDate }
    val groupedTasks = tasks.groupBy { it.reminder.nextDueDate }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroSummaryCard(
                title = "Upcoming care",
                subtitle = if (tasks.isEmpty()) {
                    "Add plants and reminders to build your care calendar."
                } else {
                    "Your next ${tasks.size} scheduled tasks across ${plants.size} plants."
                },
                plants = plants,
            )
        }

        if (groupedTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Calendar is empty",
                    body = "New reminders will appear here automatically.",
                )
            }
        } else {
            groupedTasks.forEach { (date, dateTasks) ->
                item(key = date.toString()) {
                    DateHeader(date)
                }
                items(
                    items = dateTasks,
                    key = { "${it.plant.id}-${it.reminder.id}" },
                ) { task ->
                    CalendarTaskRow(
                        task = task,
                        onClick = { onPlantClick(task.plant) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    plants: List<Plant>,
    contentPadding: PaddingValues,
    onShowOnboarding: () -> Unit,
) {
    val reminderCount = plants.sumOf { it.reminders.count { reminder -> reminder.enabled } }
    val historyCount = plants.sumOf { it.careHistory.size }
    val noteCount = plants.sumOf { it.healthNotes.size }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFFEAF6EC)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = LeafGreen)
                        }
                        Column {
                            Text(
                                text = "Plant care reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink,
                            )
                            Text(
                                text = "WaterMe schedules local notifications around 9:00 AM.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedInk,
                            )
                        }
                    }
                    SettingRow("Plants tracked", plants.size.toString())
                    SettingRow("Active reminders", reminderCount.toString())
                    SettingRow("Care history entries", historyCount.toString())
                    SettingRow("Health notes", noteCount.toString())
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                shape = RoundedCornerShape(22.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = Clay)
                        Text(
                            text = "Friendly defaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                    Text(
                        text = "New plants start with watering and misting suggestions. You can enable fertilizing, repotting, and pruning while adding each plant.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onShowOnboarding,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Show onboarding again")
            }
        }
    }
}

@Composable
private fun HeroSummaryCard(
    title: String,
    subtitle: String,
    plants: List<Plant>,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LeafGreen),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(30.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalFlorist,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = Color.White,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.86f),
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetricPill("${plants.size} plants")
                    MetricPill("${plants.sumOf { it.reminders.size }} reminders")
                }
            }
        }
    }
}

@Composable
private fun MetricPill(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Ink,
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun CareTaskCard(
    task: PlantTask,
    onOpenPlant: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenPlant)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CareGlyph(task.reminder.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.reminder.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = task.plant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = task.plant.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FilledTonalButton(onClick = onComplete) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Done")
            }
        }
    }
}

@Composable
private fun PlantCard(
    plant: Plant,
    onClick: () -> Unit,
) {
    val today = LocalDate.now()
    val dueCount = plant.dueReminders(today).size
    val nextReminder = plant.reminders
        .filter { it.enabled }
        .minByOrNull { it.nextDueDate }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantPhoto(
                photoUri = plant.photoUri,
                plantName = plant.name,
                size = 86.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (dueCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFE8C2),
                        ) {
                            Text(
                                text = "$dueCount due",
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6D4400),
                            )
                        }
                    }
                }
                Text(
                    text = "${plant.type} - ${plant.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = nextReminder?.let { "Next: ${it.type.shortLabel} ${it.nextDueDate.friendlyDate()}" }
                        ?: "No reminders yet",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = LeafGreen,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MutedInk)
        }
    }
}

@Composable
private fun PlantDetailHeader(plant: Plant) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantPhoto(
                    photoUri = plant.photoUri,
                    plantName = plant.name,
                    size = 112.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = plant.type,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LeafGreen,
                    )
                    Text(
                        text = plant.location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk,
                    )
                }
            }
            if (plant.notes.isNotBlank()) {
                Text(
                    text = plant.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${plant.reminders.count { it.enabled }} reminders") },
                    leadingIcon = { Icon(Icons.Rounded.Event, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${plant.careHistory.size} logs") },
                    leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: CareReminder,
    onComplete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CareGlyph(reminder.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "Every ${reminder.frequencyDays} days - next ${reminder.nextDueDate.friendlyDate()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
            }
            FilledTonalButton(onClick = onComplete) {
                Text("Done")
            }
        }
    }
}

@Composable
private fun ReminderDraftCard(
    careType: CareType,
    draft: ReminderDraft,
    onDraftChange: (ReminderDraft) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CareGlyph(careType, size = 42.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = careType.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                    )
                    Text(
                        text = "Suggested every ${careType.defaultFrequencyDays} days",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk,
                    )
                }
                Switch(
                    checked = draft.enabled,
                    onCheckedChange = { onDraftChange(draft.copy(enabled = it)) },
                )
            }

            if (draft.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = draft.frequencyDays,
                        onValueChange = { value ->
                            if (value.all(Char::isDigit) && value.length <= 3) {
                                onDraftChange(draft.copy(frequencyDays = value))
                            }
                        },
                        label = { Text("Every") },
                        suffix = { Text("days") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = draft.startInDays,
                        onValueChange = { value ->
                            if (value.all(Char::isDigit) && value.length <= 3) {
                                onDraftChange(draft.copy(startInDays = value))
                            }
                        },
                        label = { Text("Starts in") },
                        suffix = { Text("days") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthNoteComposer(onAdd: (String, HealthMood) -> Unit) {
    var note by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(HealthMood.ATTENTION) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Log a quick observation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthMood.entries.chunked(2).forEach { rowOptions ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowOptions.forEach { option ->
                            FilterChip(
                                selected = mood == option,
                                onClick = { mood = option },
                                label = { Text(option.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Yellow leaves, dry soil, new growth...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp),
            )
            Button(
                onClick = {
                    onAdd(note, mood)
                    note = ""
                },
                enabled = note.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
            ) {
                Text("Add note")
            }
        }
    }
}

@Composable
private fun HealthNoteCard(
    plant: Plant,
    note: HealthNote,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = note.mood.color().copy(alpha = 0.15f),
            ) {
                Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = note.mood.label.first().toString(),
                        color = note.mood.color(),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = LeafGreen,
                )
                Text(
                    text = note.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${note.mood.label} - ${note.date.friendlyDate()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk,
                )
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: CareHistoryEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareGlyph(entry.type, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.type.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                )
            }
            Text(
                text = entry.date.friendlyDate(),
                style = MaterialTheme.typography.labelMedium,
                color = LeafGreen,
            )
        }
    }
}

@Composable
private fun CalendarTaskRow(
    task: PlantTask,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CareGlyph(task.reminder.type, size = 42.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.reminder.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                )
                Text(
                    text = "${task.plant.name} - ${task.plant.location}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MutedInk)
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (date == LocalDate.now()) Color(0xFFFFE8C2) else Color(0xFFEAF6EC),
    ) {
        Text(
            text = date.friendlyDate(includeWeekday = true),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (date == LocalDate.now()) Color(0xFF6D4400) else LeafGreen,
        )
    }
}

@Composable
private fun EmptyStateCard(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SoftCream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun PlantPhoto(
    photoUri: String?,
    plantName: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = rememberImageBitmapFromUri(photoUri)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFEAF6EC))
            .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "$plantName photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Rounded.LocalFlorist,
                contentDescription = null,
                modifier = Modifier.size(size * 0.48f),
                tint = LeafGreen,
            )
        }
    }
}

@Composable
private fun rememberImageBitmapFromUri(photoUri: String?): ImageBitmap? {
    val context = LocalContext.current
    var imageBitmap by remember(photoUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(photoUri) {
        imageBitmap = photoUri?.let { loadImageBitmap(context, it) }
    }

    return imageBitmap
}

@Composable
private fun CareGlyph(
    type: CareType,
    size: Dp = 48.dp,
) {
    val color = type.color()
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 2.8f),
        color = color.copy(alpha = 0.14f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = type.icon(),
                contentDescription = type.label,
                tint = color,
                modifier = Modifier.size(size * 0.48f),
            )
        }
    }
}

private fun loadImageBitmap(context: Context, uriString: String): ImageBitmap? =
    runCatching {
        val uri = Uri.parse(uriString)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        bitmap.asImageBitmap()
    }.getOrNull()

private fun String.toPositiveFrequency(fallback: Int): Int =
    toIntOrNull()?.takeIf { it > 0 } ?: fallback

private fun String.toNonNegativeInt(fallback: Int): Int =
    toIntOrNull()?.takeIf { it >= 0 } ?: fallback

private fun LocalDate.friendlyDate(includeWeekday: Boolean = false): String {
    val today = LocalDate.now()
    return when (this) {
        today -> if (includeWeekday) "Today, ${format(ShortDateFormatter)}" else "Today"
        today.plusDays(1) -> if (includeWeekday) "Tomorrow, ${format(ShortDateFormatter)}" else "Tomorrow"
        else -> format(if (includeWeekday) WeekdayDateFormatter else ShortDateFormatter)
    }
}

private fun CareType.color(): Color =
    when (this) {
        CareType.WATERING -> MistBlue
        CareType.FERTILIZING -> FreshGreen
        CareType.REPOTTING -> Clay
        CareType.MISTING -> Color(0xFF6AA9A5)
        CareType.PRUNING -> LeafGreen
    }

private fun HealthMood.color(): Color =
    when (this) {
        HealthMood.ATTENTION -> Clay
        HealthMood.HEALTHY -> LeafGreen
        HealthMood.GROWTH -> FreshGreen
    }

private enum class MainTab(
    val title: String,
    val icon: ImageVector,
) {
    Home("Today", Icons.Rounded.Home),
    Plants("Plants", Icons.Rounded.LocalFlorist),
    Calendar("Calendar", Icons.Rounded.Event),
    Settings("Settings", Icons.Rounded.Settings),
}

private data class PlantTask(
    val plant: Plant,
    val reminder: CareReminder,
)

private data class ReminderDraft(
    val enabled: Boolean,
    val frequencyDays: String,
    val startInDays: String,
)

private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val WeekdayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
