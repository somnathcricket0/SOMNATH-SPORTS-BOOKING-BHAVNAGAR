package com.example.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class BookingRepository(
    private val db: SomnathDatabase,
    private val sessionStore: SessionStore
) {
    constructor(context: Context) : this(
        db = SomnathDatabase.getInstance(context),
        sessionStore = SessionStore(context)
    )

    private val dao: SomnathDao = db.somnathDao()

    val activeMobileFlow: Flow<String?> = sessionStore.activeMobileFlow
    val hasAdminPinFlow: Flow<Boolean> = sessionStore.hasAdminPinFlow

    suspend fun signInWithMobile(
        mobileInput: String,
        playerNameInput: String
    ): Result<PlayerProfile> {
        return try {
            val normalizedMobile = PhoneNumberHelper.normalizeIndianMobile(mobileInput)
                ?: throw IllegalArgumentException("Please enter a valid 10-digit mobile number (with or without +91).")

            val trimmedName = playerNameInput.trim()
            val existingProfile = dao.getPlayerProfile(normalizedMobile)
            val resolvedName = when {
                trimmedName.isNotBlank() -> trimmedName
                existingProfile != null && existingProfile.playerName.isNotBlank() -> existingProfile.playerName
                else -> "Player"
            }

            require(resolvedName.length in 2..60) {
                "Player name must be between 2 and 60 characters."
            }

            val now = System.currentTimeMillis()
            val entity = PlayerProfileEntity(
                mobileNumber = normalizedMobile,
                playerName = resolvedName,
                createdAtMillis = existingProfile?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertPlayerProfile(entity)
            sessionStore.saveSession(normalizedMobile)
            Result.success(entity.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        sessionStore.clearSession()
    }

    suspend fun setupAdminPin(pin: String, confirmPin: String): Result<Unit> {
        return try {
            val cleanPin = pin.trim()
            val cleanConfirm = confirmPin.trim()
            require(AdminPinHasher.isValidPinFormat(cleanPin)) {
                "Admin PIN must be 4 to 6 digits."
            }
            require(cleanPin == cleanConfirm) {
                "PIN and Confirm PIN do not match."
            }
            val existing = sessionStore.getAdminPinCredentials()
            check(existing == null) {
                "Admin PIN is already configured. Unlock with your current PIN to change it."
            }
            val (saltHex, hashHex) = AdminPinHasher.createSaltAndHash(cleanPin)
            sessionStore.saveAdminPinCredentials(saltHex, hashHex)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAdminPin(pin: String): Result<Boolean> {
        return try {
            val cleanPin = pin.trim()
            require(AdminPinHasher.isValidPinFormat(cleanPin)) {
                "Enter a 4 to 6 digit Admin PIN."
            }
            val creds = sessionStore.getAdminPinCredentials()
                ?: throw IllegalStateException("Admin PIN is not configured yet.")
            val matched = AdminPinHasher.verifyPin(cleanPin, creds.first, creds.second)
            Result.success(matched)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeAdminPin(
        currentPin: String,
        newPin: String,
        confirmNewPin: String
    ): Result<Unit> {
        return try {
            val cleanCurrent = currentPin.trim()
            val cleanNew = newPin.trim()
            val cleanConfirm = confirmNewPin.trim()
            val creds = sessionStore.getAdminPinCredentials()
                ?: throw IllegalStateException("Admin PIN is not configured yet.")
            require(AdminPinHasher.verifyPin(cleanCurrent, creds.first, creds.second)) {
                "Current Admin PIN is incorrect."
            }
            require(AdminPinHasher.isValidPinFormat(cleanNew)) {
                "New Admin PIN must be 4 to 6 digits."
            }
            require(cleanNew == cleanConfirm) {
                "New PIN and confirmation do not match."
            }
            val (saltHex, hashHex) = AdminPinHasher.createSaltAndHash(cleanNew)
            sessionStore.saveAdminPinCredentials(saltHex, hashHex)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePlayerProfile(mobile: String): Flow<PlayerProfile?> {
        return dao.observePlayerProfile(mobile).map { it?.toModel() }
    }

    suspend fun savePlayerProfile(
        playerName: String,
        mobileNumber: String
    ): Result<PlayerProfile> {
        return try {
            val normalizedMobile = PhoneNumberHelper.normalizeIndianMobile(mobileNumber)
                ?: throw IllegalArgumentException("Enter a valid 10-digit mobile number (with or without +91).")
            val resolvedName = playerName.trim().ifBlank { "Player" }
            require(resolvedName.length in 2..60) {
                "Player name must be between 2 and 60 characters."
            }
            val now = System.currentTimeMillis()
            val existing = dao.getPlayerProfile(normalizedMobile)
            val entity = PlayerProfileEntity(
                mobileNumber = normalizedMobile,
                playerName = resolvedName,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertPlayerProfile(entity)
            Result.success(entity.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeScheduleLocks(selectedDateIso: String, boxType: BoxType): Flow<List<SlotLock>> {
        val nextDateIso = TariffCalculator.nextIsoDate(selectedDateIso)
        return dao.observeSlotLocksForDates(selectedDateIso, nextDateIso, boxType.name)
            .map { entities -> entities.map { it.toModel() } }
    }

    fun observeMyBookings(mobile: String): Flow<List<Booking>> {
        return dao.observeMyBookings(mobile).map { entities -> entities.map { it.toModel() } }
    }

    fun observeAllBookingsForAdmin(): Flow<List<Booking>> {
        return dao.observeAllBookings().map { entities -> entities.map { it.toModel() } }
    }

    suspend fun createBookingWithLocks(
        currentMobileId: String,
        playerName: String,
        playerMobile: String,
        boxType: BoxType,
        selectedScheduleDateIso: String,
        startHour: Int,
        durationHours: Int,
        utrNumber: String,
        screenshotBase64: String?
    ): Result<Booking> {
        return try {
            require(currentMobileId.isNotBlank()) { "Please sign in with your mobile number first." }
            val cleanName = playerName.trim().ifBlank { "Player" }
            val cleanMobile = PhoneNumberHelper.normalizeIndianMobile(playerMobile)
                ?: throw IllegalArgumentException("Valid 10-digit mobile number required.")
            val cleanUtr = utrNumber.trim().uppercase()

            require(cleanName.length in 2..60) { "Player name must be 2–60 characters." }
            require(durationHours in 1..3) { "Duration must be 1, 2, or 3 hours." }
            require(startHour in 0..23) { "Invalid start hour." }
            val isDayShift = startHour in TariffCalculator.DAY_SHIFT_HOURS
            require(TariffCalculator.isWithinShift(startHour, durationHours, isDayShift)) {
                if (isDayShift) "Day Shift bookings must end by 6:00 PM."
                else "Night Shift bookings must end by 6:00 AM."
            }
            require(cleanUtr.length in 8..30 && cleanUtr.matches(Regex("^[a-zA-Z0-9\\-]+$"))) {
                "Enter a valid 8–30 character alphanumeric UTR / Transaction ID."
            }
            val cleanScreenshot = screenshotBase64?.takeIf { it.isNotBlank() }
            if (cleanScreenshot != null) {
                require(cleanScreenshot.length <= 150_000) { "Payment screenshot size exceeds limit." }
            }

            val quote = TariffCalculator.calculateQuote(selectedScheduleDateIso, startHour, durationHours)
            val coords = TariffCalculator.resolveSlotCoordinates(selectedScheduleDateIso, startHour, durationHours)
            val actualStartDateIso = coords.first().dateIso
            val lockIds = coords.map { it.lockId(boxType) }

            val bookingId = UUID.randomUUID().toString()
            val passCode = "SSB-${UUID.randomUUID().toString().take(6).uppercase()}"
            val now = System.currentTimeMillis()

            val bookingEntity = BookingEntity(
                id = bookingId,
                userId = currentMobileId,
                passCode = passCode,
                playerName = cleanName,
                playerMobile = cleanMobile,
                boxType = boxType.name,
                date = actualStartDateIso,
                startHour = startHour,
                durationHours = durationHours,
                timeLabel = quote.timeLabel,
                tierLabel = quote.tierLabel,
                totalAmount = quote.totalAmount,
                advanceAmount = quote.advanceAmount,
                balanceAmount = quote.balanceAmount,
                utrNumber = cleanUtr,
                screenshotBase64 = cleanScreenshot,
                status = BookingStatus.PENDING_VERIFICATION.name,
                adminNote = null,
                createdAtMillis = now,
                updatedAtMillis = now
            )

            val lockEntities = coords.map { coord ->
                SlotLockEntity(
                    id = coord.lockId(boxType),
                    userId = currentMobileId,
                    bookingId = bookingId,
                    boxType = boxType.name,
                    date = coord.dateIso,
                    hour = coord.hour,
                    status = BookingStatus.PENDING_VERIFICATION.name,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }

            db.withTransaction {
                val existingLocks = dao.findLocksByIds(lockIds)
                if (existingLocks.isNotEmpty()) {
                    throw IllegalStateException("One or more selected slots are already booked. Please choose another slot.")
                }
                dao.insertBooking(bookingEntity)
                dao.insertSlotLocks(lockEntities)
            }

            Result.success(bookingEntity.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelMyBooking(booking: Booking, currentUserId: String): Result<Unit> {
        return try {
            check(booking.userId == currentUserId) { "Cannot cancel another player's booking." }
            check(booking.statusEnum() == BookingStatus.PENDING_VERIFICATION) {
                "Only pending bookings can be cancelled."
            }
            require(booking.id.isNotBlank()) { "Booking ID missing." }

            val boxType = booking.boxTypeEnum()
            val lockIds = resolveBookingCoordinatesForModification(booking).map { it.lockId(boxType) }
            val now = System.currentTimeMillis()

            db.withTransaction {
                dao.updateBookingStatus(
                    bookingId = booking.id,
                    status = BookingStatus.CANCELLED.name,
                    adminNote = booking.adminNote,
                    updatedAt = now
                )
                if (lockIds.isNotEmpty()) {
                    dao.deleteSlotLocksByIds(lockIds)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyBookingByAdmin(
        booking: Booking,
        approve: Boolean,
        adminNote: String?
    ): Result<Unit> {
        return try {
            require(booking.id.isNotBlank()) { "Booking ID missing." }
            val currentStatus = booking.statusEnum()
            check(currentStatus != BookingStatus.CANCELLED) {
                "Cancelled bookings cannot be modified."
            }
            if (approve) {
                check(currentStatus == BookingStatus.PENDING_VERIFICATION) {
                    "Only pending bookings can be verified."
                }
            }

            val targetStatus = if (approve) BookingStatus.VERIFIED else BookingStatus.REJECTED
            val cleanNote = adminNote?.trim()?.take(200)?.takeIf { it.isNotBlank() }
            val boxType = booking.boxTypeEnum()
            val lockIds = resolveBookingCoordinatesForModification(booking).map { it.lockId(boxType) }
            val now = System.currentTimeMillis()

            db.withTransaction {
                dao.updateBookingStatus(
                    bookingId = booking.id,
                    status = targetStatus.name,
                    adminNote = cleanNote ?: booking.adminNote,
                    updatedAt = now
                )
                if (lockIds.isNotEmpty()) {
                    if (approve) {
                        dao.updateSlotLockStatus(
                            lockIds = lockIds,
                            status = BookingStatus.VERIFIED.name,
                            updatedAt = now
                        )
                    } else {
                        dao.deleteSlotLocksByIds(lockIds)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveBookingCoordinatesForModification(booking: Booking): List<SlotCoordinate> {
        val nextDate = TariffCalculator.nextIsoDate(booking.date)
        return (0 until booking.durationHours).map { offset ->
            val h = booking.startHour + offset
            if (h < 24) {
                SlotCoordinate(dateIso = booking.date, hour = h)
            } else {
                SlotCoordinate(dateIso = nextDate, hour = h - 24)
            }
        }
    }
}
