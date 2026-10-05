package com.smartsolar.stations.reservations.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.smartsolar.stations.BuildConfig
import com.smartsolar.stations.reservations.model.BookingSlot
import com.smartsolar.stations.reservations.model.CreateReservationBody
import com.smartsolar.stations.reservations.model.EnergyReservation
import com.smartsolar.stations.reservations.model.UpdateReservationBody
import com.smartsolar.stations.stations.model.SolarStation
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Loads and changes reservations through the API, and keeps a SQLite copy for offline reference. */
class ReservationRepository(private val db: ReservationDbHelper) {
    private val api: ReservationApi = retrofit()

    suspend fun reservations(): Pair<List<EnergyReservation>, Boolean> {
        return try {
            val live = api.all()
            db.replaceAll(live)
            live to false
        } catch (_: Exception) {
            db.readAll() to true
        }
    }

    suspend fun stations(): List<SolarStation> = api.stations()

    suspend fun slots(stationId: String): List<BookingSlot> = api.slots(stationId)

    suspend fun one(id: String): EnergyReservation = api.one(id)

    suspend fun create(userId: String, stationId: String, slotId: String, energy: Double): EnergyReservation {
        val saved = api.create(CreateReservationBody(userId, stationId, slotId, energy)).reservation
            ?: throw IllegalStateException("The server did not return the new booking.")
        return saved
    }

    suspend fun update(id: String, slotId: String): EnergyReservation {
        return api.update(id, UpdateReservationBody(slotId)).reservation
            ?: throw IllegalStateException("The server did not return the updated booking.")
    }

    suspend fun cancel(id: String): EnergyReservation {
        return api.cancel(id).reservation
            ?: throw IllegalStateException("The server did not return the cancelled booking.")
    }

    companion object {
        fun apiMessage(error: Throwable): String {
            val http = error as? HttpException ?: return error.message ?: "Request failed."
            val raw = http.response()?.errorBody()?.string().orEmpty()
            if (raw.isBlank()) return "Request failed (HTTP ${http.code()})."
            return runCatching {
                val json = JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString
                    ?: json.get("title")?.asString
                    ?: raw
            }.getOrDefault(raw)
        }

        private fun retrofit(): ReservationApi {
            val base = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }
            val log = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            val client = OkHttpClient.Builder().addInterceptor(log).build()
            return Retrofit.Builder()
                .baseUrl(base)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(Gson()))
                .build()
                .create(ReservationApi::class.java)
        }
    }
}
