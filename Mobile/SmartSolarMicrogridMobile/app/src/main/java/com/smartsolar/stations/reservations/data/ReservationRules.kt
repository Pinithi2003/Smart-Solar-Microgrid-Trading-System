package com.smartsolar.stations.reservations.data

import com.smartsolar.stations.reservations.model.EnergyReservation
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date window and 12-hour checks used only to enable or disable buttons. The API enforces the rules. */
object ReservationRules {
    private val longDay = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    private val shortDay = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val clock = Regex("""(\d{1,2}):(\d{2})\s*(AM|PM)""", RegexOption.IGNORE_CASE)

    fun dayKey(iso: String?): String = iso?.take(10).orEmpty()

    fun windowDays(): List<LocalDate> = (0..7).map { LocalDate.now().plusDays(it.toLong()) }

    fun formatLong(iso: String): String = runCatching { LocalDate.parse(dayKey(iso)).format(longDay) }.getOrDefault(dayKey(iso))

    fun formatShort(iso: String): String = runCatching { LocalDate.parse(dayKey(iso)).format(shortDay) }.getOrDefault(dayKey(iso))

    fun statusLabel(status: String): String = if (status == "Confirmed") "Approved" else status.ifBlank { "Unknown" }

    fun startOf(reservation: EnergyReservation): LocalDateTime {
        val day = runCatching { LocalDate.parse(dayKey(reservation.date)) }.getOrElse { return LocalDateTime.now() }
        val match = clock.find(reservation.startTime) ?: return day.atStartOfDay()
        var hour = match.groupValues[1].toInt() % 12
        if (match.groupValues[3].equals("PM", true)) hour += 12
        return day.atTime(hour, match.groupValues[2].toInt())
    }

    fun hoursUntil(reservation: EnergyReservation): Double =
        Duration.between(LocalDateTime.now(), startOf(reservation)).toMinutes() / 60.0

    fun canChange(reservation: EnergyReservation): Boolean =
        reservation.status == "Confirmed" && hoursUntil(reservation) >= 12.0

    fun isPending(reservation: EnergyReservation): Boolean = reservation.status == "Confirmed"

    fun isCurrent(reservation: EnergyReservation): Boolean {
        val open = reservation.status == "Confirmed" || reservation.status == "Verified"
        return open && dayKey(reservation.date) == LocalDate.now().toString()
    }

    fun isFutureApproved(reservation: EnergyReservation): Boolean {
        val open = reservation.status == "Confirmed" || reservation.status == "Verified"
        return open && dayKey(reservation.date) >= LocalDate.now().toString()
    }

    fun isHistory(reservation: EnergyReservation): Boolean =
        reservation.status == "Completed" || reservation.status == "Cancelled" || dayKey(reservation.date) < LocalDate.now().toString()
}
