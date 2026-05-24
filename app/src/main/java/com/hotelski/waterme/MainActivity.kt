package com.hotelski.waterme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.hotelski.waterme.data.PlantRepository
import com.hotelski.waterme.model.Plant
import com.hotelski.waterme.notifications.NotificationChannels
import com.hotelski.waterme.notifications.ReminderScheduler
import com.hotelski.waterme.ui.WaterMeApp
import com.hotelski.waterme.ui.theme.WaterMeTheme

class MainActivity : ComponentActivity() {
    private val plantRepository by lazy { PlantRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationChannels.ensureCreated(this)

        setContent {
            var plants by remember { mutableStateOf(plantRepository.loadPlants()) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) {}

            LaunchedEffect(Unit) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            LaunchedEffect(plants) {
                plantRepository.savePlants(plants)
                ReminderScheduler.scheduleAll(this@MainActivity, plants)
            }

            WaterMeTheme {
                WaterMeApp(
                    plants = plants,
                    onboardingComplete = plantRepository.isOnboardingComplete(),
                    onOnboardingComplete = {
                        plantRepository.setOnboardingComplete(true)
                    },
                    onPlantsChange = { updatedPlants: List<Plant> ->
                        plants = updatedPlants
                    },
                )
            }
        }
    }
}
