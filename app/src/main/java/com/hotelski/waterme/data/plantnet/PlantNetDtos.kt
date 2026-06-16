package com.hotelski.waterme.data.plantnet

import com.google.gson.annotations.SerializedName

data class PlantNetIdentificationResponseDto(
    @SerializedName("results")
    val results: List<PlantNetIdentificationResultDto>? = null,
)

data class PlantNetIdentificationResultDto(
    @SerializedName("score")
    val score: Double? = null,
    @SerializedName("species")
    val species: PlantNetSpeciesDto? = null,
    @SerializedName("images")
    val images: List<PlantNetRelatedImageDto>? = null,
)

data class PlantNetSpeciesDto(
    @SerializedName("scientificNameWithoutAuthor")
    val scientificNameWithoutAuthor: String? = null,
    @SerializedName("scientificName")
    val scientificName: String? = null,
    @SerializedName("commonNames")
    val commonNames: List<String>? = null,
)

data class PlantNetRelatedImageDto(
    @SerializedName("url")
    val url: PlantNetImageUrlDto? = null,
    @SerializedName("author")
    val author: String? = null,
    @SerializedName("license")
    val license: String? = null,
    @SerializedName("citation")
    val citation: String? = null,
)

data class PlantNetImageUrlDto(
    @SerializedName(value = "s", alternate = ["small"])
    val small: String? = null,
    @SerializedName(value = "m", alternate = ["medium"])
    val medium: String? = null,
    @SerializedName(value = "o", alternate = ["original"])
    val original: String? = null,
)
