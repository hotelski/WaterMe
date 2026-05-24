package com.hotelski.waterme.appstate

import com.hotelski.waterme.data.local.entity.ThemePreference

data class WaterMeAppUiState(
    val isLoading: Boolean = true,
    val userId: String = WaterMeAppContainer.LOCAL_USER_ID,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val errorMessage: String? = null,
)
