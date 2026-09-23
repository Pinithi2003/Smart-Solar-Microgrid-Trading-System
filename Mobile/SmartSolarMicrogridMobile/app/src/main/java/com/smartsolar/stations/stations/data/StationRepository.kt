package com.smartsolar.stations.stations.data

import com.smartsolar.stations.stations.model.SolarStation
import com.smartsolar.stations.core.MockData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Single Retrofit builder. BASE_URL comes from BuildConfig (10.0.2.2 = host localhost). */
object ApiClient {
    fun stationApi(baseUrl: String): StationApi {
        val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder().addInterceptor(log).build()
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StationApi::class.java)
    }
}

/**
 * Repository: tries live API first, falls back to MockData + SQLite cache.
 * Future: replace MockData branch with error surfacing once API is mandatory.
 */
class StationRepository(
    private val api: StationApi? = null,
    private val onCache: (List<SolarStation>) -> Unit = {}
) {
    suspend fun loadStations(): List<SolarStation> {
        if (api != null) {
            try {
                val live = api.getAll()
                onCache(live)
                return live
            } catch (_: Exception) {
                // Fall through to mock — show user-friendly offline note in UI.
            }
        }
        return MockData.stations
    }
}
