package com.hotelski.waterme

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hotelski.waterme.navigation.WaterMeNavigationScaffold
import com.hotelski.waterme.notifications.NotificationChannels

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationChannels.ensureCreated(this)

        setContent {
            WaterMeNavigationScaffold()
        }
    }
}
