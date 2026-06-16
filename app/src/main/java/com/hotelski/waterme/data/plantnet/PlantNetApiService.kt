package com.hotelski.waterme.data.plantnet

import java.util.concurrent.TimeUnit
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface PlantNetApiService {
    @Multipart
    @POST("v2/identify/{project}")
    suspend fun identifyPlant(
        @Path("project") project: String,
        @Query("api-key") apiKey: String,
        @Query("nb-results") resultCount: Int,
        @Query("lang") language: String,
        @Query("include-related-images") includeRelatedImages: Boolean,
        @Part image: MultipartBody.Part,
        @Part("organs") organ: RequestBody,
    ): Response<PlantNetIdentificationResponseDto>

    companion object {
        fun create(): PlantNetApiService {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(NetworkTimeoutSeconds, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BaseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlantNetApiService::class.java)
        }

        private const val BaseUrl = "https://my-api.plantnet.org/"
        private const val NetworkTimeoutSeconds = 30L
    }
}
