package com.hotelski.waterme.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface WaterMeRoute {
    val route: String

    data object Donate : WaterMeRoute {
        override val route = "donate"
    }

    data object Feedback : WaterMeRoute {
        override val route = "feedback"
    }

    data object Guide : WaterMeRoute {
        override val route = "guide"
    }

    data object Legal : WaterMeRoute {
        const val DOCUMENT_ARG = "document"
        override val route = "legal/{$DOCUMENT_ARG}"

        fun createRoute(document: String): String =
            "legal/${Uri.encode(document)}"
    }

    data object Today : WaterMeRoute {
        override val route = "today"
    }

    data object Plants : WaterMeRoute {
        const val SUCCESS_MESSAGE_KEY = "plants_success_message"
        override val route = "plants"
    }

    data object AddPlant : WaterMeRoute {
        override val route = "plants/add"
    }

    data object PlantDetails : WaterMeRoute {
        const val PLANT_ID_ARG = "plantId"
        override val route = "plants/{$PLANT_ID_ARG}"

        fun createRoute(plantId: String): String =
            "plants/${Uri.encode(plantId)}"
    }

    data object EditPlant : WaterMeRoute {
        const val PLANT_ID_ARG = "plantId"
        override val route = "plants/{$PLANT_ID_ARG}/edit"

        fun createRoute(plantId: String): String =
            "plants/${Uri.encode(plantId)}/edit"
    }

    data object ReminderSetup : WaterMeRoute {
        const val PLANT_ID_ARG = "plantId"
        override val route = "reminders/setup?$PLANT_ID_ARG={$PLANT_ID_ARG}"

        fun createRoute(plantId: String? = null): String =
            if (plantId == null) {
                "reminders/setup"
            } else {
                "reminders/setup?$PLANT_ID_ARG=${Uri.encode(plantId)}"
            }
    }

    data object Calendar : WaterMeRoute {
        override val route = "calendar"
    }

    data object CareHistory : WaterMeRoute {
        const val PLANT_ID_ARG = "plantId"
        override val route = "care-history?$PLANT_ID_ARG={$PLANT_ID_ARG}"

        fun createRoute(plantId: String? = null): String =
            if (plantId == null) {
                "care-history"
            } else {
                "care-history?$PLANT_ID_ARG=${Uri.encode(plantId)}"
        }
    }

    data object Characters : WaterMeRoute {
        override val route = "characters"
    }

    data object Settings : WaterMeRoute {
        override val route = "settings"
    }
}

enum class TopLevelDestination(
    val route: WaterMeRoute,
    val label: String,
    val icon: ImageVector,
) {
    Today(WaterMeRoute.Today, "Home", Icons.Rounded.Home),
    Plants(WaterMeRoute.Plants, "Plants", Icons.Rounded.LocalFlorist),
    Calendar(WaterMeRoute.Calendar, "Calendar", Icons.Rounded.Event),
    Settings(WaterMeRoute.Settings, "Settings", Icons.Rounded.Settings),
}

fun String?.orMissingPlantId(): String =
    requireNotNull(this) { "plantId is required for this destination." }
