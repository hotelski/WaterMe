package com.hotelski.waterme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.navigation.WaterMeNavigationScaffold
import com.hotelski.waterme.notifications.NotificationChannels
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationChannels.ensureCreated(this)
        lifecycleScope.launch {
            WaterMeAppContainer.settingsDataStore(this@MainActivity).recordAppOpen()
            WaterMeAppContainer.reminderNotificationCoordinator(this@MainActivity).syncScheduledReminders()
        }

        setContent {
            WaterMeNavigationScaffold()
        }
    }
}
