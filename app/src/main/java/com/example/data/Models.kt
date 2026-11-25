package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class BoxType(val displayName: String, val subtitle: String) {
    CRICKET("Cricket Box", "AstroTurf • Floodlights • Nets"),
    VOLLEYBALL("Volleyball Box", "Pro Court • Floodlights • Nets");

    companion object {
        fun fromRaw(raw: String): BoxType =
            entries.firstOrNull { it.name == raw } ?: CRICKET
    }
}

enum class BookingStatus(val label: String) {
    PENDING_VERIFICATION("Pending Verification"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    companion object {
        fun fromRaw(raw: String): BookingStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_VERIFICATION
    }
}

data class PlayerProfile(
    val id: String = "",
    val userId: String = "",
    val playerName: String = "",
    val mobileNumber: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class Booking(
    val id: String = "",
    val userId: String = "",
    val passCode: String = "",
    val playerName: String = "",
    val playerMobile: String = "",
    val boxType: String = BoxType.CRICKET.name,
    val date: String = "",
    val startHour: Int = 6,
    val durationHours: Int = 1,
    val timeLabel: String = "",
    val tierLabel: String = "",
    val totalAmount: Long = 0L,
    val advanceAmount: Long = 0L,
    val balanceAmount: Long = 0L,
    val utrNumber: String = "",
    val screenshotBase64: String? = null,
    val status: String = BookingStatus.PENDING_VERIFICATION.name,
    val adminNote: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun boxTypeEnum(): BoxType = BoxType.fromRaw(boxType)
    fun statusEnum(): BookingStatus = BookingStatus.fromRaw(status)
}

data class SlotLock(
    val id: String = "",
    val userId: String = "",
    val bookingId: String = "",
    val boxType: String = BoxType.CRICKET.name,
    val date: String = "",
    val hour: Int = 0,
    val status: String = BookingStatus.PENDING_VERIFICATION.name,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class DateOption(
    val dateIso: String,
    val dayOfWeekLabel: String,
    val dayNumberLabel: String,
    val monthLabel: String,
    val fullDisplay: String,
    val isWeekend: Boolean
)

data class TariffQuote(
    val totalAmount: Long,
    val advanceAmount: Long,
    val balanceAmount: Long,
    val tierLabel: String,
    val timeLabel: String
)

data class SlotCoordinate(
    val dateIso: String,
    val hour: Int
) {
    fun lockId(boxType: BoxType): String = "${dateIso}_${boxType.name}_$hour"
}

object PhoneNumberHelper {
    private val tenDigitRegex = Regex("^[0-9]{10}$")

    /**
     * Normalizes an Indian mobile number supporting formats with or without `+91`, `91`,
     * leading `0`, spaces, hyphens, or parentheses. Returns the 10-digit string or null if invalid.
     */
    fun normalizeIndianMobile(raw: String): String? {
        val cleaned = raw.trim().replace(Regex("[\\s\\-()]"), "")
        if (cleaned.isEmpty()) return null

        val candidate = when {
            cleaned.startsWith("+91") -> cleaned.removePrefix("+91")
            cleaned.startsWith("91") && cleaned.length == 12 -> cleaned.removePrefix("91")
            cleaned.startsWith("0") && cleaned.length == 11 -> cleaned.removePrefix("0")
            else -> cleaned
        }

        return candidate.takeIf { tenDigitRegex.matches(it) }
    }
}

object TariffCalculator {

    const val UPI_ID = "dhandhalavishal@okaxis"
    const val UPI_PAYEE_NAME = "Somnath Cricket Box & Cafe"
    const val HELPDESK_DISPLAY = "94285 05051"
    const val HELPDESK_RAW = "9428505051"

    // Day Shift: 6:00 AM to 6:00 PM (start slots 6 AM to 5 PM, ending by 6 PM)
    val DAY_SHIFT_HOURS: List<Int> = (6..17).toList()
    // Night Shift: 6:00 PM to 6:00 AM (start slots 6 PM to 5 AM, ending by 6 AM)
    val NIGHT_SHIFT_HOURS: List<Int> = (18..23).toList() + (0..5).toList()
    // Ordered sports-day schedule hours: 6 AM (6) .. 11 PM (23), then 12 AM (0) .. 5 AM (5)
    val SCHEDULE_HOURS: List<Int> = DAY_SHIFT_HOURS + NIGHT_SHIFT_HOURS

    /**
     * Checks whether a booking of [durationHours] starting at [startHour] stays completely within
     * Day Shift (ending by 6:00 PM) or Night Shift (ending by 6:00 AM).
     */
    fun isWithinShift(startHour: Int, durationHours: Int, isDayShift: Boolean): Boolean {
        val safeDuration = durationHours.coerceIn(1, 3)
        val shiftHours = if (isDayShift) DAY_SHIFT_HOURS else NIGHT_SHIFT_HOURS
        val startIdx = shiftHours.indexOf(startHour)
        return startIdx >= 0 && startIdx + safeDuration <= shiftHours.size
    }

    /**
     * Returns the exact consecutive hours (in 0..23) covered by [durationHours] from [startHour].
     */
    fun consecutiveHours(startHour: Int, durationHours: Int): List<Int> {
        val safeDuration = durationHours.coerceIn(1, 3)
        return (0 until safeDuration).map { offset -> ((startHour + offset) % 24 + 24) % 24 }
    }

    fun generateUpcomingDates(days: Int = 14): List<DateOption> {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dowFormat = SimpleDateFormat("EEE", Locale.US)
        val dayNumFormat = SimpleDateFormat("dd", Locale.US)
        val monthFormat = SimpleDateFormat("MMM", Locale.US)
        val fullFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)

        val cal = Calendar.getInstance()
        return List(days) { offset ->
            val date = cal.time
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val weekend = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
            val option = DateOption(
                dateIso = isoFormat.format(date),
                dayOfWeekLabel = if (offset == 0) "Today" else dowFormat.format(date),
                dayNumberLabel = dayNumFormat.format(date),
                monthLabel = monthFormat.format(date),
                fullDisplay = fullFormat.format(date),
                isWeekend = weekend
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
            option
        }
    }

    fun nextIsoDate(dateIso: String): String {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = isoFormat.parse(dateIso) ?: return dateIso
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return isoFormat.format(cal.time)
    }

    fun isWeekendIsoDate(dateIso: String): Boolean {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val parsed = isoFormat.parse(dateIso) ?: return false
        val cal = Calendar.getInstance().apply { time = parsed }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    /**
     * Resolves the calendar date & hour (0..23) for each hour in a booking starting on [selectedDateIso].
     * Hours 0..5 selected inside the "6 AM - 6 AM" schedule tab actually belong to the next calendar day.
     */
    fun resolveSlotCoordinates(
        selectedDateIso: String,
        startHour: Int,
        durationHours: Int
    ): List<SlotCoordinate> {
        val baseNextDate = nextIsoDate(selectedDateIso)
        val baseIndex = if (startHour >= 6) startHour else startHour + 24
        return (0 until durationHours).map { offset ->
            val hAbs = baseIndex + offset
            if (hAbs < 24) {
                SlotCoordinate(dateIso = selectedDateIso, hour = hAbs)
            } else if (hAbs < 48) {
                SlotCoordinate(dateIso = baseNextDate, hour = hAbs - 24)
            } else {
                SlotCoordinate(dateIso = baseNextDate, hour = hAbs % 24)
            }
        }
    }

    fun calculateQuote(
        selectedDateIso: String,
        startHour: Int,
        durationHours: Int
    ): TariffQuote {
        val safeDuration = durationHours.coerceIn(1, 3)
        val weekend = isWeekendIsoDate(selectedDateIso)

        val (total, tierLabel) = if (weekend) {
            val price = when (safeDuration) {
                1 -> 800L
                2 -> 1400L
                else -> 2000L
            }
            price to "Sat & Sun (All Hours)"
        } else {
            val coords = resolveSlotCoordinates(selectedDateIso, startHour, safeDuration)
            val allDay = coords.all { it.hour in 6..17 }
            val allNight = coords.all { it.hour >= 18 || it.hour < 6 }
            when {
                allDay -> {
                    val price = when (safeDuration) {
                        1 -> 500L
                        2 -> 800L
                        else -> 1100L
                    }
                    price to "Mon-Fri Day (6 AM - 6 PM)"
                }
                allNight -> {
                    val price = when (safeDuration) {
                        1 -> 700L
                        2 -> 1300L
                        else -> 1800L
                    }
                    price to "Mon-Fri Night (6 PM - 6 AM)"
                }
                else -> {
                    val dayCount = coords.count { it.hour in 6..17 }
                    val nightCount = safeDuration - dayCount
                    val price = (dayCount * 500L) + (nightCount * 700L)
                    price to "Mon-Fri Day + Night Mix"
                }
            }
        }

        val advance = total / 2L
        val balance = total - advance
        val endHour = (startHour + safeDuration) % 24
        val timeLabel = "${formatHour(startHour)} - ${formatHour(endHour)}"

        return TariffQuote(
            totalAmount = total,
            advanceAmount = advance,
            balanceAmount = balance,
            tierLabel = tierLabel,
            timeLabel = timeLabel
        )
    }

    fun bookingTimeWindowMillis(
        dateIso: String,
        startHour: Int,
        durationHours: Int
    ): Pair<Long, Long>? {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val parsed = runCatching { isoFormat.parse(dateIso) }.getOrNull() ?: return null
        val cal = Calendar.getInstance().apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, startHour.coerceIn(0, 23))
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMillis = cal.timeInMillis
        val endMillis = startMillis + (durationHours.coerceIn(1, 3) * 3_600_000L)
        return startMillis to endMillis
    }

    fun isBookingPast(
        booking: Booking,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val window = bookingTimeWindowMillis(booking.date, booking.startHour, booking.durationHours)
            ?: return false
        return window.second <= nowMillis
    }

    fun isBookingUpcoming(
        booking: Booking,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val status = booking.statusEnum()
        if (status == BookingStatus.CANCELLED || status == BookingStatus.REJECTED) return false
        return !isBookingPast(booking, nowMillis)
    }

    fun formatHour(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        return when (h) {
            0 -> "12:00 AM"
            12 -> "12:00 PM"
            in 1..11 -> String.format(Locale.US, "%02d:00 AM", h)
            else -> String.format(Locale.US, "%02d:00 PM", h - 12)
        }
    }

    fun formatHourShort(hour: Int): String {
        val h = ((hour % 24) + 24) % 24
        return when (h) {
            0 -> "12 AM"
            12 -> "12 PM"
            in 1..11 -> "$h AM"
            else -> "${h - 12} PM"
        }
    }

    fun formatTimeRangeShort(startHour: Int, durationHours: Int): String {
        val safeDuration = durationHours.coerceIn(1, 3)
        val endHour = (startHour + safeDuration) % 24
        return "${formatHourShort(startHour)} to ${formatHourShort(endHour)}"
    }
}
