package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "player_profiles")
data class PlayerProfileEntity(
    @PrimaryKey val mobileNumber: String,
    val playerName: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): PlayerProfile = PlayerProfile(
        id = mobileNumber,
        userId = mobileNumber,
        playerName = playerName,
        mobileNumber = mobileNumber,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val passCode: String,
    val playerName: String,
    val playerMobile: String,
    val boxType: String,
    val date: String,
    val startHour: Int,
    val durationHours: Int,
    val timeLabel: String,
    val tierLabel: String,
    val totalAmount: Long,
    val advanceAmount: Long,
    val balanceAmount: Long,
    val utrNumber: String,
    val screenshotBase64: String?,
    val status: String,
    val adminNote: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): Booking = Booking(
        id = id,
        userId = userId,
        passCode = passCode,
        playerName = playerName,
        playerMobile = playerMobile,
        boxType = boxType,
        date = date,
        startHour = startHour,
        durationHours = durationHours,
        timeLabel = timeLabel,
        tierLabel = tierLabel,
        totalAmount = totalAmount,
        advanceAmount = advanceAmount,
        balanceAmount = balanceAmount,
        utrNumber = utrNumber,
        screenshotBase64 = screenshotBase64,
        status = status,
        adminNote = adminNote,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "slot_locks")
data class SlotLockEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookingId: String,
    val boxType: String,
    val date: String,
    val hour: Int,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): SlotLock = SlotLock(
        id = id,
        userId = userId,
        bookingId = bookingId,
        boxType = boxType,
        date = date,
        hour = hour,
        status = status,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Dao
interface SomnathDao {

    @Query("SELECT * FROM player_profiles WHERE mobileNumber = :mobile LIMIT 1")
    fun observePlayerProfile(mobile: String): Flow<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profiles WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getPlayerProfile(mobile: String): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerProfile(profile: PlayerProfileEntity)

    @Query(
        "SELECT * FROM slot_locks WHERE boxType = :boxType AND date IN (:dateA, :dateB) AND status IN ('PENDING_VERIFICATION', 'VERIFIED')"
    )
    fun observeSlotLocksForDates(dateA: String, dateB: String, boxType: String): Flow<List<SlotLockEntity>>

    @Query("SELECT * FROM slot_locks WHERE id IN (:lockIds)")
    suspend fun findLocksByIds(lockIds: List<String>): List<SlotLockEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSlotLocks(locks: List<SlotLockEntity>)

    @Query("UPDATE slot_locks SET status = :status, updatedAtMillis = :updatedAt WHERE id IN (:lockIds)")
    suspend fun updateSlotLockStatus(lockIds: List<String>, status: String, updatedAt: Long)

    @Query("DELETE FROM slot_locks WHERE id IN (:lockIds)")
    suspend fun deleteSlotLocksByIds(lockIds: List<String>)

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun observeMyBookings(userId: String): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings ORDER BY createdAtMillis DESC")
    fun observeAllBookings(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = :status, adminNote = :adminNote, updatedAtMillis = :updatedAt WHERE id = :bookingId")
    suspend fun updateBookingStatus(bookingId: String, status: String, adminNote: String?, updatedAt: Long)
}

@Database(
    entities = [PlayerProfileEntity::class, BookingEntity::class, SlotLockEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SomnathDatabase : RoomDatabase() {
    abstract fun somnathDao(): SomnathDao

    companion object {
        @Volatile
        private var INSTANCE: SomnathDatabase? = null

        fun getInstance(context: Context): SomnathDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SomnathDatabase::class.java,
                    "somnath_sports_booking.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
