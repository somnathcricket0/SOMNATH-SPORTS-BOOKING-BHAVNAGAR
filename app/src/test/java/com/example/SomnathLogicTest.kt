package com.example

import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.PhoneNumberHelper
import com.example.data.TariffCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SomnathLogicTest {

    @Test
    fun `mobile normalization handles plain, +91, 0 prefix, and spaces`() {
        assertEquals("9876543210", PhoneNumberHelper.normalizeIndianMobile("9876543210"))
        assertEquals("9876543210", PhoneNumberHelper.normalizeIndianMobile("+91 98765 43210"))
        assertEquals("9876543210", PhoneNumberHelper.normalizeIndianMobile("919876543210"))
        assertEquals("9876543210", PhoneNumberHelper.normalizeIndianMobile("09876543210"))
        assertEquals("9876543210", PhoneNumberHelper.normalizeIndianMobile(" 98765-43210 "))

        assertNull(PhoneNumberHelper.normalizeIndianMobile("12345"))
        assertNull(PhoneNumberHelper.normalizeIndianMobile("+91 987654321"))
        assertNull(PhoneNumberHelper.normalizeIndianMobile("abcdefghij"))
    }

    @Test
    fun `weekday day and night and weekend tariffs compute exact 50 percent advance`() {
        // 2026-11-24 is Tuesday (Weekday)
        val day1h = TariffCalculator.calculateQuote("2026-11-24", 10, 1)
        assertEquals(500L, day1h.totalAmount)
        assertEquals(250L, day1h.advanceAmount)

        val day2h = TariffCalculator.calculateQuote("2026-11-24", 10, 2)
        assertEquals(800L, day2h.totalAmount)
        assertEquals(400L, day2h.advanceAmount)

        val day3h = TariffCalculator.calculateQuote("2026-11-24", 10, 3)
        assertEquals(1100L, day3h.totalAmount)
        assertEquals(550L, day3h.advanceAmount)

        val night1h = TariffCalculator.calculateQuote("2026-11-24", 20, 1)
        assertEquals(700L, night1h.totalAmount)
        assertEquals(350L, night1h.advanceAmount)

        val night2h = TariffCalculator.calculateQuote("2026-11-24", 20, 2)
        assertEquals(1300L, night2h.totalAmount)
        assertEquals(650L, night2h.advanceAmount)

        val night3h = TariffCalculator.calculateQuote("2026-11-24", 20, 3)
        assertEquals(1800L, night3h.totalAmount)
        assertEquals(900L, night3h.advanceAmount)

        // 2026-11-28 is Saturday (Weekend)
        val wk1h = TariffCalculator.calculateQuote("2026-11-28", 10, 1)
        assertEquals(800L, wk1h.totalAmount)
        assertEquals(400L, wk1h.advanceAmount)

        val wk2h = TariffCalculator.calculateQuote("2026-11-28", 19, 2)
        assertEquals(1400L, wk2h.totalAmount)
        assertEquals(700L, wk2h.advanceAmount)

        val wk3h = TariffCalculator.calculateQuote("2026-11-28", 21, 3)
        assertEquals(2000L, wk3h.totalAmount)
        assertEquals(1000L, wk3h.advanceAmount)
        assertEquals(1000L, wk3h.balanceAmount)
    }

    @Test
    fun `shift boundaries and consecutive hours match 6 AM to 6 PM and 6 PM to 6 AM`() {
        assertEquals((6..17).toList(), TariffCalculator.DAY_SHIFT_HOURS)
        assertEquals((18..23).toList() + (0..5).toList(), TariffCalculator.NIGHT_SHIFT_HOURS)

        // Day shift ending by 6 PM
        assertEquals(true, TariffCalculator.isWithinShift(17, 1, isDayShift = true))
        assertEquals(false, TariffCalculator.isWithinShift(17, 2, isDayShift = true))
        assertEquals(true, TariffCalculator.isWithinShift(16, 2, isDayShift = true))
        assertEquals(false, TariffCalculator.isWithinShift(16, 3, isDayShift = true))
        assertEquals(true, TariffCalculator.isWithinShift(15, 3, isDayShift = true))

        // Night shift ending by 6 AM
        assertEquals(true, TariffCalculator.isWithinShift(5, 1, isDayShift = false))
        assertEquals(false, TariffCalculator.isWithinShift(5, 2, isDayShift = false))
        assertEquals(true, TariffCalculator.isWithinShift(4, 2, isDayShift = false))
        assertEquals(false, TariffCalculator.isWithinShift(4, 3, isDayShift = false))
        assertEquals(true, TariffCalculator.isWithinShift(3, 3, isDayShift = false))
        assertEquals(true, TariffCalculator.isWithinShift(23, 3, isDayShift = false))

        // Consecutive hours wrap smoothly across midnight
        assertEquals(listOf(23, 0, 1), TariffCalculator.consecutiveHours(23, 3))
        assertEquals(listOf(10, 11), TariffCalculator.consecutiveHours(10, 2))

        // Start-to-end time range labels match user specification for 1h, 2h, 3h Day & Night
        assertEquals("6 AM to 7 AM", TariffCalculator.formatTimeRangeShort(6, 1))
        assertEquals("5 PM to 6 PM", TariffCalculator.formatTimeRangeShort(17, 1))
        assertEquals("6 PM to 7 PM", TariffCalculator.formatTimeRangeShort(18, 1))
        assertEquals("5 AM to 6 AM", TariffCalculator.formatTimeRangeShort(5, 1))

        assertEquals("6 AM to 8 AM", TariffCalculator.formatTimeRangeShort(6, 2))
        assertEquals("4 PM to 6 PM", TariffCalculator.formatTimeRangeShort(16, 2))
        assertEquals("6 PM to 8 PM", TariffCalculator.formatTimeRangeShort(18, 2))
        assertEquals("4 AM to 6 AM", TariffCalculator.formatTimeRangeShort(4, 2))

        assertEquals("6 AM to 9 AM", TariffCalculator.formatTimeRangeShort(6, 3))
        assertEquals("3 PM to 6 PM", TariffCalculator.formatTimeRangeShort(15, 3))
        assertEquals("6 PM to 9 PM", TariffCalculator.formatTimeRangeShort(18, 3))
        assertEquals("3 AM to 6 AM", TariffCalculator.formatTimeRangeShort(3, 3))
        assertEquals("11 PM to 2 AM", TariffCalculator.formatTimeRangeShort(23, 3))
    }

    @Test
    fun `upcoming and past booking classification handles slot end times and statuses`() {
        val refTime = TariffCalculator.bookingTimeWindowMillis("2026-11-24", 12, 1)!!.first

        // Upcoming verified slot later same day (2 PM - 4 PM)
        val upcomingVerified = Booking(
            date = "2026-11-24",
            startHour = 14,
            durationHours = 2,
            status = BookingStatus.VERIFIED.name
        )
        assertEquals(false, TariffCalculator.isBookingPast(upcomingVerified, refTime))
        assertEquals(true, TariffCalculator.isBookingUpcoming(upcomingVerified, refTime))

        // In-progress slot (11 AM - 1 PM when now is 12 PM) is still upcoming/active until end time
        val inProgress = Booking(
            date = "2026-11-24",
            startHour = 11,
            durationHours = 2,
            status = BookingStatus.PENDING_VERIFICATION.name
        )
        assertEquals(false, TariffCalculator.isBookingPast(inProgress, refTime))
        assertEquals(true, TariffCalculator.isBookingUpcoming(inProgress, refTime))

        // Ended slot (10 AM - 12 PM when now is 12 PM) is past
        val justEnded = Booking(
            date = "2026-11-24",
            startHour = 10,
            durationHours = 2,
            status = BookingStatus.VERIFIED.name
        )
        assertEquals(true, TariffCalculator.isBookingPast(justEnded, refTime))
        assertEquals(false, TariffCalculator.isBookingUpcoming(justEnded, refTime))

        // Midnight-wrapping slot (11 PM - 1 AM) ending after midnight
        val midnightPastCheck = TariffCalculator.bookingTimeWindowMillis("2026-11-25", 0, 1)!!.second
        val lateNight = Booking(
            date = "2026-11-24",
            startHour = 23,
            durationHours = 2,
            status = BookingStatus.VERIFIED.name
        )
        assertEquals(false, TariffCalculator.isBookingPast(lateNight, midnightPastCheck - 1))
        assertEquals(true, TariffCalculator.isBookingPast(lateNight, midnightPastCheck))

        // Cancelled future booking is not upcoming
        val cancelledFuture = Booking(
            date = "2026-11-28",
            startHour = 18,
            durationHours = 2,
            status = BookingStatus.CANCELLED.name
        )
        assertEquals(false, TariffCalculator.isBookingPast(cancelledFuture, refTime))
        assertEquals(false, TariffCalculator.isBookingUpcoming(cancelledFuture, refTime))
    }

    @Test
    fun `admin pin hasher validates format, generates unique salts, and verifies matching pin`() {
        assertEquals(false, com.example.data.AdminPinHasher.isValidPinFormat("123"))
        assertEquals(false, com.example.data.AdminPinHasher.isValidPinFormat("1234567"))
        assertEquals(false, com.example.data.AdminPinHasher.isValidPinFormat("12ab"))
        assertEquals(true, com.example.data.AdminPinHasher.isValidPinFormat("5051"))
        assertEquals(true, com.example.data.AdminPinHasher.isValidPinFormat("123456"))

        val (salt1, hash1) = com.example.data.AdminPinHasher.createSaltAndHash("5051")
        val (salt2, hash2) = com.example.data.AdminPinHasher.createSaltAndHash("5051")

        // Random salts ensure identical PINs produce distinct hashes
        assertEquals(false, salt1 == salt2)
        assertEquals(false, hash1 == hash2)

        assertEquals(true, com.example.data.AdminPinHasher.verifyPin("5051", salt1, hash1))
        assertEquals(true, com.example.data.AdminPinHasher.verifyPin("5051", salt2, hash2))
        assertEquals(false, com.example.data.AdminPinHasher.verifyPin("5052", salt1, hash1))
        assertEquals(false, com.example.data.AdminPinHasher.verifyPin("123", salt1, hash1))
    }
}
