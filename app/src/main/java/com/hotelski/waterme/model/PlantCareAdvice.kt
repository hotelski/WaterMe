package com.hotelski.waterme.model

data class PlantCareAdvice(
    val plantName: String = "",
    val scientificName: String? = null,
    val shortDescription: String = "",
    val careDifficulty: String = "",
    val matureHeight: String = "",
    val watering: String = "",
    val light: String = "",
    val temperature: String = "",
    val humidity: String = "",
    val fertilizing: String = "",
    val repotting: String = "",
    val flowering: String = "",
    val growth: String = "",
    val toxicity: String = "",
    val origin: String = "",
    val disclaimer: String = "",
    val suggestedWateringIntervalDays: Int? = null,
    val suggestedFertilizingIntervalDays: Int? = null,
    val suggestedLightLevel: String? = null,
    val suggestedNote: String? = null,
)
