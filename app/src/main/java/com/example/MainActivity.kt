package com.example

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BookingRepository
import com.example.data.PlayerProfile
import com.example.data.TariffCalculator
import com.example.ui.MainTab
import com.example.ui.SomnathViewModel
import com.example.ui.UiState
import com.example.ui.components.BookingPassModal
import com.example.ui.components.ScreenshotPreviewDialog
import com.example.ui.components.UncroppedSomnathLogo
import com.example.ui.components.dialHelpdesk
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.screens.RatesScreen
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.SomnathSportsTheme
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val mobileTenDigits: String) : SessionState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SomnathSportsTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context.applicationContext) {
        BookingRepository(context.applicationContext)
    }

    val sessionFlow = remember(repository) {
        repository.activeMobileFlow.map { mobile ->
            if (mobile.isNullOrBlank()) SessionState.SignedOut
            else SessionState.SignedIn(mobile)
        }
    }
    val sessionState by sessionFlow.collectAsStateWithLifecycle(initialValue = SessionState.Loading)

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    when (val state = sessionState) {
        is SessionState.Loading -> {
            BrandedLoadingScreen("Opening Somnath Sports Booking...")
        }
        is SessionState.SignedOut -> {
            AuthScreen(
                isSubmitting = isSigningIn,
                authErrorMessage = authError,
                onClearError = { authError = null },
                onContinueWithMobile = { mobile, playerName ->
                    scope.launch {
                        isSigningIn = true
                        val result = repository.signInWithMobile(mobile, playerName)
                        isSigningIn = false
                        result.onFailure { err ->
                            authError = err.localizedMessage ?: "Unable to sign in. Please check your mobile number."
                        }
                    }
                }
            )
        }
        is SessionState.SignedIn -> {
            val mobileId = state.mobileTenDigits
            key(mobileId) {
                val viewModel: SomnathViewModel = viewModel(
                    key = mobileId,
                    factory = viewModelFactory {
                        initializer {
                            SomnathViewModel(
                                repository = repository,
                                currentMobileId = mobileId
                            )
                        }
                    }
                )
                AuthenticatedAppShell(
                    viewModel = viewModel,
                    onSignOut = {
                        scope.launch {
                            repository.signOut()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    viewModel: SomnathViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val playerProfileState by viewModel.playerProfileState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedBox by viewModel.selectedBox.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDuration by viewModel.selectedDuration.collectAsStateWithLifecycle()
    val selectedStartHour by viewModel.selectedStartHour.collectAsStateWithLifecycle()
    val scheduleLocksState by viewModel.scheduleLocksState.collectAsStateWithLifecycle()
    val myBookingsState by viewModel.myBookingsState.collectAsStateWithLifecycle()
    val adminBookingsState by viewModel.adminBookingsState.collectAsStateWithLifecycle()
    val hasAdminPin by viewModel.hasAdminPin.collectAsStateWithLifecycle()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsStateWithLifecycle()
    val adminAuthError by viewModel.adminAuthError.collectAsStateWithLifecycle()
    val pinLockoutUntilMillis by viewModel.pinLockoutUntilMillis.collectAsStateWithLifecycle()
    val activePassBooking by viewModel.activePassBooking.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    LaunchedEffect(playerProfileState) {
        if (playerProfileState is UiState.Success && (playerProfileState as UiState.Success).data == null) {
            viewModel.savePlayerProfile("Player", viewModel.currentMobileId)
        }
    }

    when (val profileRes = playerProfileState) {
        is UiState.Loading -> {
            BrandedLoadingScreen("Loading your player profile...")
        }
        is UiState.Error -> {
            BrandedErrorScreen(
                message = profileRes.message,
                onSignOut = onSignOut
            )
        }
        is UiState.Success -> {
            val profile = profileRes.data ?: PlayerProfile(
                id = viewModel.currentMobileId,
                userId = viewModel.currentMobileId,
                playerName = "Player",
                mobileNumber = viewModel.currentMobileId
            )

            val snackbarHostState = remember { SnackbarHostState() }
            var showProfileDialog by remember { mutableStateOf(false) }
            var previewScreenshotBitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(feedbackMessage) {
                val msg = feedbackMessage
                if (!msg.isNullOrBlank()) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearFeedback()
                }
            }

            BackHandler(enabled = selectedTab != MainTab.HOME) {
                viewModel.selectTab(MainTab.HOME)
            }

            val myBookingsList = (myBookingsState as? UiState.Success)?.data ?: emptyList()

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MainTab.entries.forEach { tab ->
                                val selected = tab == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { TabIcon(tab, selected) },
                                    label = { Text(tab.title, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = RoyalBlue,
                                        selectedTextColor = RoyalBlue,
                                        indicatorColor = RoyalBlueLight
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = RoyalBlue,
                                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .height(42.dp)
                                                .width(136.dp)
                                        ) {
                                            UncroppedSomnathLogo(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                                                    .testTag("topbar_somnath_logo")
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Bhavnagar",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = GoldAccent,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    Surface(
                                        onClick = { dialHelpdesk(context) },
                                        color = MaterialTheme.colorScheme.surface,
                                        contentColor = RoyalBlueDark,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .testTag("topbar_helpdesk_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Call,
                                                contentDescription = "Call Helpdesk ${TariffCalculator.HELPDESK_DISPLAY}",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = TariffCalculator.HELPDESK_DISPLAY,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { showProfileDialog = true },
                                        modifier = Modifier.testTag("topbar_profile_button")
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = "Player profile and sign out"
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (!useNavRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    MainTab.entries.forEach { tab ->
                                        val selected = tab == selectedTab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { viewModel.selectTab(tab) },
                                            icon = { TabIcon(tab, selected) },
                                            label = {
                                                Text(
                                                    text = tab.title,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = RoyalBlue,
                                                selectedTextColor = RoyalBlue,
                                                indicatorColor = RoyalBlueLight
                                            ),
                                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (selectedTab) {
                                MainTab.HOME -> HomeScreen(
                                    playerProfile = profile,
                                    myBookings = myBookingsList,
                                    onBookBox = { viewModel.navigateToBook(it) },
                                    onOpenRates = { viewModel.selectTab(MainTab.RATES) },
                                    onOpenMyBookings = { viewModel.selectTab(MainTab.MY_BOOKINGS) },
                                    onOpenPass = { viewModel.openBookingPass(it) }
                                )
                                MainTab.BOOK -> BookScreen(
                                    playerProfile = profile,
                                    selectedBox = selectedBox,
                                    upcomingDates = viewModel.upcomingDates,
                                    selectedDate = selectedDate,
                                    selectedDuration = selectedDuration,
                                    selectedStartHour = selectedStartHour,
                                    scheduleLocksState = scheduleLocksState,
                                    isSubmitting = isSubmitting,
                                    onSelectBox = { viewModel.selectBox(it) },
                                    onSelectDate = { viewModel.selectDate(it) },
                                    onSelectDuration = { viewModel.selectDuration(it) },
                                    onSelectStartHour = { viewModel.selectStartHour(it) },
                                    onSubmitBooking = { name, mobile, utr, screenshot ->
                                        viewModel.submitBooking(name, mobile, utr, screenshot) {
                                            // Pass modal opens via activePassBooking StateFlow
                                        }
                                    },
                                    onShowMessage = { viewModel.showFeedback(it) },
                                    onPreviewScreenshot = { previewScreenshotBitmap = it }
                                )
                                MainTab.RATES -> RatesScreen(
                                    onBookBox = { viewModel.navigateToBook(it) }
                                )
                                MainTab.MY_BOOKINGS -> MyBookingsScreen(
                                    bookingsState = myBookingsState,
                                    isSubmitting = isSubmitting,
                                    onOpenPass = { viewModel.openBookingPass(it) },
                                    onCancelBooking = { viewModel.cancelMyBooking(it) },
                                    onBookNow = { viewModel.selectTab(MainTab.BOOK) },
                                    onRefresh = { viewModel.refreshMyBookings() },
                                    onPreviewScreenshot = { previewScreenshotBitmap = it }
                                )
                                MainTab.ADMIN -> AdminScreen(
                                    hasAdminPin = hasAdminPin,
                                    isAdminUnlocked = isAdminUnlocked,
                                    isSubmitting = isSubmitting,
                                    adminAuthError = adminAuthError,
                                    pinLockoutUntilMillis = pinLockoutUntilMillis,
                                    adminBookingsState = adminBookingsState,
                                    onSetupAdminPin = { pin, confirmPin ->
                                        viewModel.setupAdminPin(pin, confirmPin)
                                    },
                                    onUnlockAdminPin = { pin ->
                                        viewModel.unlockAdminWithPin(pin)
                                    },
                                    onLockAdminPanel = {
                                        viewModel.lockAdminPanel()
                                    },
                                    onChangeAdminPin = { currentPin, newPin, confirmNewPin, onSuccess, onError ->
                                        viewModel.changeAdminPin(currentPin, newPin, confirmNewPin, onSuccess, onError)
                                    },
                                    onClearAdminAuthError = {
                                        viewModel.clearAdminAuthError()
                                    },
                                    onVerifyBooking = { booking, approve, note ->
                                        viewModel.verifyBookingByAdmin(booking, approve, note)
                                    },
                                    onOpenPass = { viewModel.openBookingPass(it) },
                                    onPreviewScreenshot = { previewScreenshotBitmap = it }
                                )
                            }
                        }
                    }
                }
            }

            activePassBooking?.let { passBooking ->
                BookingPassModal(
                    booking = passBooking,
                    onDismiss = { viewModel.dismissBookingPass() },
                    onPreviewScreenshot = { previewScreenshotBitmap = it }
                )
            }

            previewScreenshotBitmap?.let { bmp ->
                ScreenshotPreviewDialog(
                    bitmap = bmp,
                    onDismiss = { previewScreenshotBitmap = null }
                )
            }

            if (showProfileDialog) {
                AlertDialog(
                    onDismissRequest = { showProfileDialog = false },
                    title = { Text("Player Account", color = RoyalBlueDark) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = profile.playerName,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Mobile: +91 ${profile.mobileNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "Helpdesk: ${TariffCalculator.HELPDESK_DISPLAY}",
                                style = MaterialTheme.typography.bodySmall,
                                color = RoyalBlue
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showProfileDialog = false
                                onSignOut()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Out")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showProfileDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TabIcon(tab: MainTab, selected: Boolean) {
    val icon = when (tab) {
        MainTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
        MainTab.BOOK -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
        MainTab.RATES -> if (selected) Icons.Filled.LocalOffer else Icons.Outlined.LocalOffer
        MainTab.MY_BOOKINGS -> if (selected) Icons.Filled.ConfirmationNumber else Icons.Outlined.ConfirmationNumber
        MainTab.ADMIN -> if (selected) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings
    }
    Icon(imageVector = icon, contentDescription = tab.title)
}

@Composable
private fun BrandedLoadingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RoyalBlueDark),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, GoldAccent),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                UncroppedSomnathLogo(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
                CircularProgressIndicator(color = RoyalBlue)
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BrandedErrorScreen(message: String, onSignOut: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, BorderBlue),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.brand_full_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalBlue
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Return to Sign In")
                }
            }
        }
    }
}
