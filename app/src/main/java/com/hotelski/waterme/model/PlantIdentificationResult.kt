package com.hotelski.waterme.model

data class PlantIdentificationResult(
    val commonName: String,
    val scientificName: String,
    val confidenceScore: Double,
    val relatedImageUrl: String? = null,
    val relatedImageAttribution: String? = null,
)
