package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AdminPinHasher
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.ui.ImageCompressionHelper
import com.example.ui.UiState
import com.example.ui.components.BoxTypeBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatInr
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg
import kotlinx.coroutines.delay

private enum class AdminFilter(val label: String) {
    PENDING("Pending Review"),
    VERIFIED("Verified"),
    REJECTED("Rejected / Cancelled"),
    ALL("All")
}

private data class VerificationAction(
    val booking: Booking,
    val approve: Boolean
)

@Composable
fun AdminScreen(
    hasAdminPin: Boolean?,
    isAdminUnlocked: Boolean,
    isSubmitting: Boolean,
    adminAuthError: String?,
    pinLockoutUntilMillis: Long,
    adminBookingsState: UiState<List<Booking>>,
    onSetupAdminPin: (pin: String, confirmPin: String) -> Unit,
    onUnlockAdminPin: (pin: String) -> Unit,
    onLockAdminPanel: () -> Unit,
    onChangeAdminPin: (
        currentPin: String,
        newPin: String,
        confirmNewPin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit,
    onClearAdminAuthError: () -> Unit,
    onVerifyBooking: (booking: Booking, approve: Boolean, adminNote: String?) -> Unit,
    onOpenPass: (Booking) -> Unit,
    onPreviewScreenshot: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var activeFilter by remember { mutableStateOf(AdminFilter.PENDING) }
    var pendingAction by remember { mutableStateOf<VerificationAction?>(null) }
    var noteInput by remember { mutableStateOf("") }
    var showChangePinDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        if (hasAdminPin == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RoyalBlue)
            }
            return@Box
        }

        if (!isAdminUnlocked) {
            AdminPinGateScreen(
                hasExistingPin = hasAdminPin,
                isSubmitting = isSubmitting,
                authError = adminAuthError,
                pinLockoutUntilMillis = pinLockoutUntilMillis,
                onClearError = onClearAdminAuthError,
                onSetupPin = onSetupAdminPin,
                onUnlockWithPin = onUnlockAdminPin
            )
            return@Box
        }

        when (adminBookingsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RoyalBlue)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = adminBookingsState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = RejectedRed,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is UiState.Success -> {
                val allBookings = adminBookingsState.data
                val pendingCount = allBookings.count { it.statusEnum() == BookingStatus.PENDING_VERIFICATION }
                val verifiedCount = allBookings.count { it.statusEnum() == BookingStatus.VERIFIED }
                val totalAdvance = allBookings
                    .filter { it.statusEnum() == BookingStatus.VERIFIED }
                    .sumOf { it.advanceAmount }

                val filtered = remember(allBookings, activeFilter) {
                    when (activeFilter) {
                        AdminFilter.PENDING -> allBookings.filter { it.statusEnum() == BookingStatus.PENDING_VERIFICATION }
                        AdminFilter.VERIFIED -> allBookings.filter { it.statusEnum() == BookingStatus.VERIFIED }
                        AdminFilter.REJECTED -> allBookings.filter {
                            it.statusEnum() == BookingStatus.REJECTED || it.statusEnum() == BookingStatus.CANCELLED
                        }
                        AdminFilter.ALL -> allBookings
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = RoyalBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Admin Payment Verification",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Verify UPI UTR & screenshots for Somnath Cricket Box & Cafe",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    border = BorderStroke(1.dp, BorderBlue),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("admin_change_pin_button")
                                ) {
                                    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change PIN")
                                }
                                OutlinedButton(
                                    onClick = onLockAdminPanel,
                                    border = BorderStroke(1.dp, RoyalBlue),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoyalBlueDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("admin_lock_button")
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lock Admin")
                                }
                            }
                        }
                    }

                    // Admin KPI strip
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AdminStatCard("Pending", pendingCount.toString(), PendingAmberBg, PendingAmber, Modifier.weight(1f))
                            AdminStatCard("Verified", verifiedCount.toString(), VerifiedGreenBg, VerifiedGreen, Modifier.weight(1f))
                            AdminStatCard("Advance", formatInr(totalAdvance), RoyalBlueLight, RoyalBlueDark, Modifier.weight(1f))
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AdminFilter.entries) { filter ->
                                val selected = filter == activeFilter
                                FilterChip(
                                    selected = selected,
                                    onClick = { activeFilter = filter },
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("admin_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, BorderBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No bookings in '${activeFilter.label}'.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(28.dp)
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { booking ->
                            val status = booking.statusEnum()
                            val bitmap = remember(booking.screenshotBase64) {
                                ImageCompressionHelper.decodeBase64ToBitmap(booking.screenshotBase64)
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, BorderBlue),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_booking_${booking.passCode}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BoxTypeBadge(boxType = booking.boxTypeEnum())
                                        StatusBadge(status = status)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${booking.playerName} • ${booking.passCode}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "${booking.date} | ${booking.timeLabel} (${booking.durationHours} hr)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${booking.playerMobile}"))
                                                runCatching { context.startActivity(intent) }
                                            },
                                            border = BorderStroke(1.dp, BorderBlue),
                                            modifier = Modifier.height(40.dp)
                                        ) {
                                            Icon(Icons.Filled.Call, contentDescription = "Call player", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(booking.playerMobile, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    Surface(
                                        color = RoyalBlueTint,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("UTR / Txn Reference", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                                Text(
                                                    text = booking.utrNumber,
                                                    fontFamily = JetBrainsMonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "50% Adv: ${formatInr(booking.advanceAmount)}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = RoyalBlue
                                                )
                                                Text(
                                                    text = "Total: ${formatInr(booking.totalAmount)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    if (bitmap != null) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, BorderBlue),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onPreviewScreenshot(bitmap) }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(10.dp)
                                            ) {
                                                Image(
                                                    bitmap = bitmap.asImageBitmap(),
                                                    contentDescription = "Uploaded payment screenshot",
                                                    contentScale = ContentScale.Fit,
                                                    modifier = Modifier
                                                        .size(58.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(SurfaceMuted)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Payment Screenshot Attached", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                    Text("Tap to inspect full receipt", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Image, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("No screenshot uploaded (verify via UTR)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                    }

                                    HorizontalDivider(color = BorderBlue)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onOpenPass(booking) },
                                            border = BorderStroke(1.dp, BorderBlue),
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.ConfirmationNumber, contentDescription = "View Pass", modifier = Modifier.size(18.dp))
                                        }
                                        if (status == BookingStatus.PENDING_VERIFICATION) {
                                            Button(
                                                onClick = {
                                                    noteInput = "Payment verified via Axis UPI"
                                                    pendingAction = VerificationAction(booking, approve = true)
                                                },
                                                enabled = !isSubmitting,
                                                colors = ButtonDefaults.buttonColors(containerColor = VerifiedGreen),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("admin_verify_${booking.passCode}")
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Verify")
                                            }
                                        }
                                        if (status == BookingStatus.PENDING_VERIFICATION || status == BookingStatus.VERIFIED) {
                                            OutlinedButton(
                                                onClick = {
                                                    noteInput = "UTR not matched"
                                                    pendingAction = VerificationAction(booking, approve = false)
                                                },
                                                enabled = !isSubmitting,
                                                border = BorderStroke(1.dp, RejectedRed),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectedRed),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("admin_reject_${booking.passCode}")
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Reject")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = {
                Text(
                    if (action.approve) "Verify Booking ${action.booking.passCode}?"
                    else "Reject Booking ${action.booking.passCode}?"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (action.approve) {
                            "Confirming UTR ${action.booking.utrNumber} (${formatInr(action.booking.advanceAmount)}) marks this slot as Verified."
                        } else {
                            "Rejecting this booking frees the turf slot for other players."
                        }
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it.take(200) },
                        label = { Text("Admin Note (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onVerifyBooking(action.booking, action.approve, noteInput)
                        pendingAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (action.approve) VerifiedGreen else RejectedRed
                    )
                ) {
                    Text(if (action.approve) "Verify & Confirm" else "Reject & Release")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showChangePinDialog) {
        ChangeAdminPinDialog(
            isSubmitting = isSubmitting,
            onDismiss = { showChangePinDialog = false },
            onSubmit = { currentPin, newPin, confirmNewPin, onError ->
                onChangeAdminPin(
                    currentPin,
                    newPin,
                    confirmNewPin,
                    { showChangePinDialog = false },
                    onError
                )
            }
        )
    }
}

@Composable
private fun AdminPinGateScreen(
    hasExistingPin: Boolean,
    isSubmitting: Boolean,
    authError: String?,
    pinLockoutUntilMillis: Long,
    onClearError: () -> Unit,
    onSetupPin: (pin: String, confirmPin: String) -> Unit,
    onUnlockWithPin: (pin: String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var pinInput by rememberSaveable { mutableStateOf("") }
    var confirmPinInput by rememberSaveable { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pinLockoutUntilMillis) {
        while (System.currentTimeMillis() < pinLockoutUntilMillis) {
            nowMillis = System.currentTimeMillis()
            delay(500L)
        }
        nowMillis = System.currentTimeMillis()
    }
    val remainingLockSec = ((pinLockoutUntilMillis - nowMillis + 999L) / 1000L).coerceAtLeast(0L)
    val isLockedOut = remainingLockSec > 0L

    fun handleSubmit() {
        focusManager.clearFocus()
        if (hasExistingPin) {
            if (!AdminPinHasher.isValidPinFormat(pinInput)) {
                localError = "Enter your 4 to 6 digit Admin PIN."
            } else {
                localError = null
                onClearError()
                onUnlockWithPin(pinInput)
            }
        } else {
            when {
                !AdminPinHasher.isValidPinFormat(pinInput) -> {
                    localError = "Admin PIN must be 4 to 6 digits."
                }
                pinInput != confirmPinInput -> {
                    localError = "PIN and Confirm PIN do not match."
                }
                else -> {
                    localError = null
                    onClearError()
                    onSetupPin(pinInput, confirmPinInput)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, BorderBlue),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RoyalBlueLight,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasExistingPin) Icons.Filled.Lock else Icons.Filled.Key,
                            contentDescription = null,
                            tint = RoyalBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = if (hasExistingPin) "Admin Verification Locked" else "Set Venue Owner Admin PIN",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (hasExistingPin) {
                        "Enter your Venue Owner Admin PIN to view player payment proofs and approve or reject bookings."
                    } else {
                        "Create a private 4–6 digit Admin PIN so only the Somnath Cricket Box & Cafe owner can access payment proofs and verification controls."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { raw ->
                        pinInput = raw.filter { it.isDigit() }.take(AdminPinHasher.MAX_PIN_LENGTH)
                        localError = null
                        if (!authError.isNullOrBlank()) onClearError()
                    },
                    enabled = !isSubmitting && !isLockedOut,
                    label = {
                        Text(if (hasExistingPin) "Admin PIN *" else "Create 4–6 Digit Admin PIN *")
                    },
                    placeholder = { Text("••••") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = RoyalBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPin) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = if (hasExistingPin) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (hasExistingPin) handleSubmit() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        unfocusedBorderColor = BorderBlue
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (hasExistingPin) "admin_pin_input" else "create_admin_pin_input")
                )

                if (!hasExistingPin) {
                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { raw ->
                            confirmPinInput = raw.filter { it.isDigit() }.take(AdminPinHasher.MAX_PIN_LENGTH)
                            localError = null
                            if (!authError.isNullOrBlank()) onClearError()
                        },
                        enabled = !isSubmitting,
                        label = { Text("Confirm Admin PIN *") },
                        placeholder = { Text("••••") },
                        leadingIcon = {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = RoyalBlue)
                        },
                        visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSubmit() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_admin_pin_input")
                    )
                }

                val displayError = localError ?: authError
                if (!displayError.isNullOrBlank() || isLockedOut) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isLockedOut) {
                                "Too many incorrect attempts. Try again in ${remainingLockSec}s."
                            } else {
                                displayError.orEmpty()
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = { handleSubmit() },
                    enabled = !isSubmitting && !isLockedOut,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(if (hasExistingPin) "unlock_admin_button" else "save_admin_pin_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasExistingPin) "Unlock Admin Panel" else "Save Admin PIN & Unlock",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeAdminPinDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        currentPin: String,
        newPin: String,
        confirmNewPin: String,
        onError: (String) -> Unit
    ) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Change Venue Owner PIN", color = RoyalBlueDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = {
                        currentPin = it.filter { ch -> ch.isDigit() }.take(AdminPinHasher.MAX_PIN_LENGTH)
                        errorMsg = null
                    },
                    label = { Text("Current Admin PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it.filter { ch -> ch.isDigit() }.take(AdminPinHasher.MAX_PIN_LENGTH)
                        errorMsg = null
                    },
                    label = { Text("New 4–6 Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = {
                        confirmNewPin = it.filter { ch -> ch.isDigit() }.take(AdminPinHasher.MAX_PIN_LENGTH)
                        errorMsg = null
                    },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text = errorMsg.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = RejectedRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        !AdminPinHasher.isValidPinFormat(currentPin) ->
                            errorMsg = "Enter your current 4–6 digit PIN."
                        !AdminPinHasher.isValidPinFormat(newPin) ->
                            errorMsg = "New PIN must be 4 to 6 digits."
                        newPin != confirmNewPin ->
                            errorMsg = "New PIN and confirmation do not match."
                        else -> {
                            onSubmit(currentPin, newPin, confirmNewPin) { msg -> errorMsg = msg }
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
            ) {
                Text("Update PIN")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminStatCard(
    label: String,
    value: String,
    bgColor: androidx.compose.ui.graphics.Color,
    fgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = fgColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                maxLines = 1
            )
        }
    }
}
