package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Booking
import com.example.data.BookingRepository
import com.example.data.BoxType
import com.example.data.DateOption
import com.example.data.PlayerProfile
import com.example.data.SlotLock
import com.example.data.TariffCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainTab(val title: String) {
    HOME("Home"),
    BOOK("Book"),
    RATES("Rates"),
    MY_BOOKINGS("Bookings"),
    ADMIN("Admin")
}

@OptIn(ExperimentalCoroutinesApi::class)
class SomnathViewModel(
    private val repository: BookingRepository,
    val currentMobileId: String
) : ViewModel() {

    val upcomingDates: List<DateOption> = TariffCalculator.generateUpcomingDates(14)

    private val _selectedTab = MutableStateFlow(MainTab.HOME)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedBox = MutableStateFlow(BoxType.CRICKET)
    val selectedBox: StateFlow<BoxType> = _selectedBox.asStateFlow()

    private val _selectedDate = MutableStateFlow(upcomingDates.first())
    val selectedDate: StateFlow<DateOption> = _selectedDate.asStateFlow()

    private val _selectedDuration = MutableStateFlow(1)
    val selectedDuration: StateFlow<Int> = _selectedDuration.asStateFlow()

    private val _selectedStartHour = MutableStateFlow<Int?>(null)
    val selectedStartHour: StateFlow<Int?> = _selectedStartHour.asStateFlow()

    private val _activePassBooking = MutableStateFlow<Booking?>(null)
    val activePassBooking: StateFlow<Booking?> = _activePassBooking.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private val _myBookingsRetryTrigger = MutableStateFlow(0)

    // In-memory Admin PIN gate state: locked by default whenever a new session starts
    val hasAdminPin: StateFlow<Boolean?> = repository.hasAdminPinFlow
        .map<Boolean, Boolean?> { it }
        .catch { emit(false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    private val _isAdminUnlocked = MutableStateFlow(false)
    val isAdminUnlocked: StateFlow<Boolean> = _isAdminUnlocked.asStateFlow()

    private val _adminAuthError = MutableStateFlow<String?>(null)
    val adminAuthError: StateFlow<String?> = _adminAuthError.asStateFlow()

    private var failedPinAttempts: Int = 0
    private val _pinLockoutUntilMillis = MutableStateFlow(0L)
    val pinLockoutUntilMillis: StateFlow<Long> = _pinLockoutUntilMillis.asStateFlow()

    val playerProfileState: StateFlow<UiState<PlayerProfile?>> =
        repository.observePlayerProfile(currentMobileId)
            .map<PlayerProfile?, UiState<PlayerProfile?>> { UiState.Success(it) }
            .catch { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "Error observing player profile", error)
                emit(UiState.Error(error.localizedMessage ?: "Failed to load player profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val scheduleLocksState: StateFlow<UiState<List<SlotLock>>> =
        combine(_selectedDate, _selectedBox) { dateOpt, box -> dateOpt.dateIso to box }
            .flatMapLatest { (dateIso, box) ->
                repository.observeScheduleLocks(dateIso, box)
                    .map<List<SlotLock>, UiState<List<SlotLock>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w(TAG, "Error observing slot locks", error)
                        emit(UiState.Error(error.localizedMessage ?: "Failed to load slot availability"))
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myBookingsState: StateFlow<UiState<List<Booking>>> =
        _myBookingsRetryTrigger.flatMapLatest {
            repository.observeMyBookings(currentMobileId)
                .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Error observing my bookings", error)
                    emit(UiState.Error(error.localizedMessage ?: "Failed to load your bookings"))
                }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun refreshMyBookings() {
        _myBookingsRetryTrigger.value += 1
    }

    val adminBookingsState: StateFlow<UiState<List<Booking>>> =
        isAdminUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllBookingsForAdmin()
                    .map<List<Booking>, UiState<List<Booking>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w(TAG, "Error observing admin bookings", error)
                        emit(UiState.Error(error.localizedMessage ?: "Failed to load admin bookings"))
                    }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun navigateToBook(boxType: BoxType) {
        _selectedBox.value = boxType
        _selectedStartHour.value = null
        _selectedTab.value = MainTab.BOOK
    }

    fun selectBox(boxType: BoxType) {
        _selectedBox.value = boxType
        _selectedStartHour.value = null
    }

    fun selectDate(dateOption: DateOption) {
        _selectedDate.value = dateOption
        _selectedStartHour.value = null
    }

    fun selectDuration(hours: Int) {
        val newDuration = hours.coerceIn(1, 3)
        _selectedDuration.value = newDuration
        val currentStart = _selectedStartHour.value
        if (currentStart != null) {
            val isDayShift = currentStart in TariffCalculator.DAY_SHIFT_HOURS
            if (!TariffCalculator.isWithinShift(currentStart, newDuration, isDayShift)) {
                _selectedStartHour.value = null
            }
        }
    }

    fun selectStartHour(hour: Int?) {
        _selectedStartHour.value = hour
    }

    fun openBookingPass(booking: Booking) {
        _activePassBooking.value = booking
    }

    fun dismissBookingPass() {
        _activePassBooking.value = null
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun clearAdminAuthError() {
        _adminAuthError.value = null
    }

    fun setupAdminPin(pin: String, confirmPin: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _adminAuthError.value = null
            val result = repository.setupAdminPin(pin, confirmPin)
            _isSubmitting.value = false
            result.onSuccess {
                failedPinAttempts = 0
                _pinLockoutUntilMillis.value = 0L
                _isAdminUnlocked.value = true
                _feedbackMessage.value = "Admin PIN configured and verification panel unlocked."
            }.onFailure { err ->
                _adminAuthError.value = err.localizedMessage ?: "Could not save Admin PIN."
            }
        }
    }

    fun unlockAdminWithPin(pin: String) {
        val now = System.currentTimeMillis()
        val lockoutUntil = _pinLockoutUntilMillis.value
        if (now < lockoutUntil) {
            val remainingSec = ((lockoutUntil - now + 999L) / 1000L).coerceAtLeast(1L)
            _adminAuthError.value = "Too many wrong PIN attempts. Wait ${remainingSec}s before trying again."
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _adminAuthError.value = null
            val result = repository.verifyAdminPin(pin)
            _isSubmitting.value = false
            result.onSuccess { matched ->
                if (matched) {
                    failedPinAttempts = 0
                    _pinLockoutUntilMillis.value = 0L
                    _isAdminUnlocked.value = true
                    _feedbackMessage.value = "Admin panel unlocked."
                } else {
                    failedPinAttempts += 1
                    if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
                        failedPinAttempts = 0
                        _pinLockoutUntilMillis.value = System.currentTimeMillis() + LOCKOUT_DURATION_MILLIS
                        _adminAuthError.value = "Too many incorrect attempts. Locked for 30 seconds."
                    } else {
                        val left = MAX_PIN_ATTEMPTS - failedPinAttempts
                        _adminAuthError.value = "Incorrect Admin PIN ($left attempt${if (left == 1) "" else "s"} left)."
                    }
                }
            }.onFailure { err ->
                _adminAuthError.value = err.localizedMessage ?: "Please enter a valid 4 to 6 digit Admin PIN."
            }
        }
    }

    fun lockAdminPanel() {
        _isAdminUnlocked.value = false
        _adminAuthError.value = null
        _feedbackMessage.value = "Admin panel locked."
    }

    fun changeAdminPin(
        currentPin: String,
        newPin: String,
        confirmNewPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!_isAdminUnlocked.value) {
            onError("Unlock the Admin panel before changing your PIN.")
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.changeAdminPin(currentPin, newPin, confirmNewPin)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Admin PIN updated successfully."
                onSuccess()
            }.onFailure { err ->
                onError(err.localizedMessage ?: "Could not update Admin PIN.")
            }
        }
    }

    fun savePlayerProfile(playerName: String, mobileNumber: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.savePlayerProfile(playerName, mobileNumber)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Player profile saved!"
                onSaved()
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not save profile."
            }
        }
    }

    fun submitBooking(
        playerName: String,
        playerMobile: String,
        utrNumber: String,
        screenshotBase64: String?,
        onSuccess: (Booking) -> Unit
    ) {
        val startHour = _selectedStartHour.value
        if (startHour == null) {
            _feedbackMessage.value = "Please select an available time slot first."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.createBookingWithLocks(
                currentMobileId = currentMobileId,
                playerName = playerName,
                playerMobile = playerMobile,
                boxType = _selectedBox.value,
                selectedScheduleDateIso = _selectedDate.value.dateIso,
                startHour = startHour,
                durationHours = _selectedDuration.value,
                utrNumber = utrNumber,
                screenshotBase64 = screenshotBase64
            )
            _isSubmitting.value = false
            result.onSuccess { createdBooking ->
                _selectedStartHour.value = null
                _activePassBooking.value = createdBooking
                _feedbackMessage.value = "Booking submitted! Pass ${createdBooking.passCode} generated."
                onSuccess(createdBooking)
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Slot could not be booked. Please pick another slot."
            }
        }
    }

    fun cancelMyBooking(booking: Booking) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.cancelMyBooking(booking, currentMobileId)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = "Booking ${booking.passCode} cancelled and slot released."
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Could not cancel booking."
            }
        }
    }

    fun verifyBookingByAdmin(booking: Booking, approve: Boolean, adminNote: String?) {
        if (!_isAdminUnlocked.value) {
            _feedbackMessage.value = "Unlock the Admin panel with your PIN before verifying bookings."
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.verifyBookingByAdmin(booking, approve, adminNote)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackMessage.value = if (approve) {
                    "Pass ${booking.passCode} verified & confirmed!"
                } else {
                    "Pass ${booking.passCode} rejected and slot released."
                }
            }.onFailure { err ->
                _feedbackMessage.value = err.localizedMessage ?: "Admin verification failed."
            }
        }
    }

    private companion object {
        const val TAG = "SomnathVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
        const val MAX_PIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MILLIS = 30_000L
    }
}
