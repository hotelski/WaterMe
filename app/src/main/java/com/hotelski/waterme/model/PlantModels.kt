package com.hotelski.waterme.model

enum class CareType(
    val label: String,
    val shortLabel: String,
    val defaultFrequencyDays: Int,
) {
    WATERING("Watering", "Water", 4),
    FERTILIZING("Fertilizing", "Feed", 30),
    REPOTTING("Repotting", "Repot", 180),
    MISTING("Misting", "Mist", 3),
    PRUNING("Pruning", "Prune", 45),
}

enum class HealthMood(val label: String) {
    ATTENTION("Needs attention"),
    HEALTHY("Healthy"),
    GROWTH("New growth"),
}

enum class PlantEnvironment(val label: String) {
    INDOOR("Indoor"),
    OUTDOOR("Outdoor"),
}
