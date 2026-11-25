package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.BoxType
import com.example.data.DateOption
import com.example.data.PlayerProfile
import com.example.data.SlotLock
import com.example.data.TariffCalculator
import com.example.ui.ImageCompressionHelper
import com.example.ui.UiState
import com.example.ui.components.copyToClipboard
import com.example.ui.components.formatInr
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun BookScreen(
    playerProfile: PlayerProfile,
    selectedBox: BoxType,
    upcomingDates: List<DateOption>,
    selectedDate: DateOption,
    selectedDuration: Int,
    selectedStartHour: Int?,
    scheduleLocksState: UiState<List<SlotLock>>,
    isSubmitting: Boolean,
    onSelectBox: (BoxType) -> Unit,
    onSelectDate: (DateOption) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectStartHour: (Int?) -> Unit,
    onSubmitBooking: (
        playerName: String,
        playerMobile: String,
        utrNumber: String,
        screenshotBase64: String?
    ) -> Unit,
    onShowMessage: (String) -> Unit,
    onPreviewScreenshot: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var utrNumber by rememberSaveable { mutableStateOf("") }
    var screenshotBase64 by rememberSaveable { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }

    val screenshotBitmap = remember(screenshotBase64) {
        ImageCompressionHelper.decodeBase64ToBitmap(screenshotBase64)
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isProcessingImage = true
            scope.launch {
                val encoded = ImageCompressionHelper.compressUriToBase64(context, uri)
                isProcessingImage = false
                if (encoded != null) {
                    screenshotBase64 = encoded
                    formError = null
                } else {
                    onShowMessage("Could not read selected image. Please try another screenshot.")
                }
            }
        }
    }

    val lockedCoordinateSet = remember(scheduleLocksState) {
        when (scheduleLocksState) {
            is UiState.Success -> scheduleLocksState.data.map { "${it.date}_${it.hour}" }.toSet()
            else -> emptySet()
        }
    }

    val todayIso = upcomingDates.firstOrNull()?.dateIso
    val currentHourNow = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    var selectedShiftDay by rememberSaveable { mutableStateOf(true) }
    val dayHours = TariffCalculator.DAY_SHIFT_HOURS
    val nightHours = TariffCalculator.NIGHT_SHIFT_HOURS
    val baseShiftHours = if (selectedShiftDay) dayHours else nightHours
    val visibleHours = remember(selectedShiftDay, selectedDuration) {
        baseShiftHours.filter { TariffCalculator.isWithinShift(it, selectedDuration, selectedShiftDay) }
    }

    fun isSingleHourOpen(hour: Int): Pair<Boolean, String?> {
        val singleCoord = TariffCalculator.resolveSlotCoordinates(selectedDate.dateIso, hour, 1).first()
        if ("${singleCoord.dateIso}_${singleCoord.hour}" in lockedCoordinateSet) {
            return false to "Booked"
        }
        if (todayIso != null && singleCoord.dateIso == todayIso && singleCoord.hour <= currentHourNow) {
            return false to "Past"
        }
        return true to null
    }

    fun isSlotAvailable(startHour: Int): Boolean {
        if (scheduleLocksState !is UiState.Success) return false
        if (!TariffCalculator.isWithinShift(startHour, selectedDuration, selectedShiftDay)) return false
        val coords = TariffCalculator.resolveSlotCoordinates(selectedDate.dateIso, startHour, selectedDuration)
        for (coord in coords) {
            if ("${coord.dateIso}_${coord.hour}" in lockedCoordinateSet) return false
            if (todayIso != null && coord.dateIso == todayIso && coord.hour <= currentHourNow) {
                return false
            }
        }
        return true
    }

    fun disabledSlotReason(startHour: Int): String {
        val (singleOpen, singleReason) = isSingleHourOpen(startHour)
        if (!singleOpen && singleReason != null) return singleReason
        if (!TariffCalculator.isWithinShift(startHour, selectedDuration, selectedShiftDay)) {
            return if (selectedShiftDay) "Ends >6PM" else "Ends >6AM"
        }
        return "Overlap"
    }

    val highlightedHoursList = remember(selectedStartHour, selectedDuration) {
        selectedStartHour?.let { TariffCalculator.consecutiveHours(it, selectedDuration) } ?: emptyList()
    }

    LaunchedEffect(selectedStartHour, selectedDuration, scheduleLocksState) {
        val currentStart = selectedStartHour
        if (currentStart != null && scheduleLocksState is UiState.Success && !isSlotAvailable(currentStart)) {
            onSelectStartHour(null)
            onShowMessage("Selected time cannot fit $selectedDuration hr (booked slot or shift limit).")
        }
    }

    val selectedQuote = remember(selectedDate, selectedStartHour, selectedDuration) {
        selectedStartHour?.let { TariffCalculator.calculateQuote(selectedDate.dateIso, it, selectedDuration) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Box Selection
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Select Arena", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BoxType.entries.forEach { box ->
                            val selected = box == selectedBox
                            Surface(
                                onClick = { onSelectBox(box) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) RoyalBlue else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                border = BorderStroke(1.5.dp, if (selected) RoyalBlue else BorderBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("book_box_${box.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (box == BoxType.CRICKET) Icons.Filled.SportsCricket else Icons.Filled.SportsVolleyball,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = box.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Date Selection
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("2. Select Date", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Surface(
                            color = if (selectedDate.isWeekend) GoldLight else RoyalBlueLight,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (selectedDate.isWeekend) "Weekend Rates (Sat/Sun)" else "Weekday Rates (Mon–Fri)",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedDate.isWeekend) TextPrimary else RoyalBlueDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(upcomingDates, key = { it.dateIso }) { dateOpt ->
                            val isSelected = dateOpt.dateIso == selectedDate.dateIso
                            Surface(
                                onClick = { onSelectDate(dateOpt) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) RoyalBlue else MaterialTheme.colorScheme.surface,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                border = BorderStroke(1.dp, if (isSelected) RoyalBlue else BorderBlue),
                                modifier = Modifier
                                    .width(74.dp)
                                    .testTag("book_date_${dateOpt.dateIso}")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                ) {
                                    Text(
                                        text = dateOpt.dayOfWeekLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) GoldAccent else TextSecondary
                                    )
                                    Text(
                                        text = dateOpt.dayNumberLabel,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = dateOpt.monthLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Shift Selection (Day 6 AM-6 PM vs Night 6 PM-6 AM)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Select Shift", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            onClick = {
                                selectedShiftDay = true
                                onSelectStartHour(null)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedShiftDay) RoyalBlue else MaterialTheme.colorScheme.surface,
                            contentColor = if (selectedShiftDay) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                            border = BorderStroke(1.5.dp, if (selectedShiftDay) RoyalBlue else BorderBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("book_shift_day")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Day (6 AM – 6 PM)", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        Surface(
                            onClick = {
                                selectedShiftDay = false
                                onSelectStartHour(null)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (!selectedShiftDay) RoyalBlue else MaterialTheme.colorScheme.surface,
                            contentColor = if (!selectedShiftDay) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                            border = BorderStroke(1.5.dp, if (!selectedShiftDay) RoyalBlue else BorderBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("book_shift_night")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Filled.NightsStay, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Night (6 PM – 6 AM)", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            // 4. Duration Selection
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. Select Duration", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(1, 2, 3).forEach { hrs ->
                            val selected = selectedDuration == hrs
                            Surface(
                                onClick = { onSelectDuration(hrs) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) RoyalBlue else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else TextPrimary,
                                border = BorderStroke(1.dp, if (selected) RoyalBlue else BorderBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("book_duration_${hrs}h")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (hrs == 1) "1 Hour" else "$hrs Hours",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Time Slot Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("5. Select Start Time", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            LegendDot(color = MaterialTheme.colorScheme.surface, border = RoyalBlue.copy(alpha = 0.55f), label = "Open")
                            LegendDot(color = RoyalBlue, border = RoyalBlueDark, label = "Selected")
                            LegendDot(color = SurfaceMuted, border = BorderBlue.copy(alpha = 0.7f), label = "Booked")
                        }
                    }

                    when (scheduleLocksState) {
                        is UiState.Loading -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, BorderBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = RoyalBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Checking live turf slots...", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        is UiState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = scheduleLocksState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is UiState.Success -> {
                            val firstRange = visibleHours.firstOrNull()?.let {
                                TariffCalculator.formatTimeRangeShort(it, selectedDuration)
                            }.orEmpty()
                            val lastRange = visibleHours.lastOrNull()?.let {
                                TariffCalculator.formatTimeRangeShort(it, selectedDuration)
                            }.orEmpty()
                            SlotGroupHeader(
                                icon = if (selectedShiftDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                title = if (selectedShiftDay) {
                                    "Day Shift ($firstRange – $lastRange)"
                                } else {
                                    "Night Shift ($firstRange – $lastRange)"
                                },
                                rateHint = when {
                                    selectedDate.isWeekend -> "Weekend: 1h ₹800 • 2h ₹1,400 • 3h ₹2,000"
                                    selectedShiftDay -> "Mon–Fri Day: 1h ₹500 • 2h ₹800 • 3h ₹1,100"
                                    else -> "Mon–Fri Night: 1h ₹700 • 2h ₹1,300 • 3h ₹1,800"
                                }
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                visibleHours.chunked(2).forEach { rowHours ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        rowHours.forEach { hour ->
                                            val available = isSlotAvailable(hour)
                                            val isStartSelected = selectedStartHour == hour
                                            val quote = TariffCalculator.calculateQuote(selectedDate.dateIso, hour, selectedDuration)
                                            val rangeLabel = TariffCalculator.formatTimeRangeShort(hour, selectedDuration)
                                            val slotSublabel = when {
                                                isStartSelected || available -> formatInr(quote.totalAmount)
                                                else -> "${disabledSlotReason(hour)} • ${formatInr(quote.totalAmount)}"
                                            }
                                            SlotTile(
                                                hour = hour,
                                                rangeLabel = rangeLabel,
                                                sublabel = slotSublabel,
                                                available = available,
                                                isStartSelected = isStartSelected,
                                                onClick = { onSelectStartHour(if (isStartSelected) null else hour) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowHours.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            if (selectedQuote != null && highlightedHoursList.isNotEmpty()) {
                                val hourChain = highlightedHoursList.joinToString(" + ") {
                                    TariffCalculator.formatHourShort(it)
                                }
                                Surface(
                                    color = RoyalBlueLight,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Selected Window: ${selectedQuote.timeLabel} ($selectedDuration hr)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = RoyalBlueDark
                                        )
                                        Text(
                                            text = "Hours covered: $hourChain • ${selectedQuote.tierLabel}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RoyalBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. 50% Advance UPI Payment & Confirmation Card
            if (selectedQuote != null && selectedStartHour != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.5.dp, RoyalBlue),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "6. 50% Advance UPI Payment",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = RoyalBlueDark
                                )
                                Surface(color = RoyalBlueLight, shape = RoundedCornerShape(50)) {
                                    Text(
                                        text = selectedBox.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RoyalBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Surface(
                                color = RoyalBlueTint,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${selectedDate.fullDisplay} • ${selectedQuote.timeLabel} ($selectedDuration hr)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = selectedQuote.tierLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    HorizontalDivider(color = BorderBlue, modifier = Modifier.padding(vertical = 4.dp))
                                    SummaryAmountRow("Total Turf Tariff", formatInr(selectedQuote.totalAmount), TextPrimary)
                                    SummaryAmountRow("50% Advance Payable Now", formatInr(selectedQuote.advanceAmount), RoyalBlue, bold = true)
                                    SummaryAmountRow("Balance Payable at Venue", formatInr(selectedQuote.balanceAmount), VerifiedGreen)
                                }
                            }

                            // UPI ID & Intent Launch
                            Surface(
                                color = GoldLight.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Pay 50% Advance to UPI ID", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text(
                                                text = TariffCalculator.UPI_ID,
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = RoyalBlueDark
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                copyToClipboard(context, "Somnath UPI ID", TariffCalculator.UPI_ID)
                                                onShowMessage("UPI ID ${TariffCalculator.UPI_ID} copied!")
                                            },
                                            border = BorderStroke(1.dp, RoyalBlue),
                                            modifier = Modifier
                                                .height(42.dp)
                                                .testTag("copy_upi_button")
                                        ) {
                                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy")
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val note = Uri.encode("Somnath ${selectedBox.displayName} ${selectedDate.dateIso}")
                                            val payee = Uri.encode(TariffCalculator.UPI_PAYEE_NAME)
                                            val upiUri = Uri.parse(
                                                "upi://pay?pa=${TariffCalculator.UPI_ID}&pn=$payee&am=${selectedQuote.advanceAmount}&cu=INR&tn=$note"
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW, upiUri)
                                            runCatching {
                                                context.startActivity(intent)
                                            }.onFailure {
                                                copyToClipboard(context, "Somnath UPI ID", TariffCalculator.UPI_ID)
                                                onShowMessage("No UPI app detected. UPI ID copied—pay ${formatInr(selectedQuote.advanceAmount)} & enter UTR below.")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("pay_upi_intent_button")
                                    ) {
                                        Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pay ${formatInr(selectedQuote.advanceAmount)} via UPI App")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // UTR Input
                            OutlinedTextField(
                                value = utrNumber,
                                onValueChange = { input ->
                                    utrNumber = input.filter { it.isLetterOrDigit() || it == '-' }.take(30)
                                    formError = null
                                },
                                label = { Text("UPI Transaction / UTR Number *") },
                                placeholder = { Text("e.g., 432918273645") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = RoyalBlue)
                                },
                                supportingText = {
                                    Text("Enter 8–30 character UTR from GPay / PhonePe / Paytm")
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RoyalBlue,
                                    unfocusedBorderColor = BorderBlue
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("utr_input")
                            )

                            // Payment Screenshot Upload via Zero-Permission Photo Picker
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Payment Screenshot (Recommended)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                if (screenshotBitmap != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = VerifiedGreenBg.copy(alpha = 0.45f),
                                        border = BorderStroke(1.dp, VerifiedGreen.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Image(
                                                bitmap = screenshotBitmap.asImageBitmap(),
                                                contentDescription = "Uploaded payment screenshot",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(64.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(SurfaceMuted)
                                                    .clickable { onPreviewScreenshot(screenshotBitmap) }
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = VerifiedGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Screenshot Ready", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                }
                                                Text(
                                                    text = "Tap thumbnail to preview",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = RoyalBlue
                                                )
                                            }
                                            IconButton(
                                                onClick = { screenshotBase64 = null },
                                                modifier = Modifier.testTag("remove_screenshot_button")
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Remove screenshot")
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isProcessingImage,
                                        border = BorderStroke(1.dp, RoyalBlue),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("upload_screenshot_button")
                                    ) {
                                        if (isProcessingImage) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Compressing screenshot...")
                                        } else {
                                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Upload UPI Payment Screenshot")
                                        }
                                    }
                                }
                            }

                            if (!formError.isNullOrBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = formError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val cleanUtr = utrNumber.trim()
                                    if (cleanUtr.length < 8) {
                                        formError = "Please enter a valid UTR / Transaction ID (minimum 8 characters)."
                                    } else {
                                        formError = null
                                        onSubmitBooking(
                                            playerProfile.playerName,
                                            playerProfile.mobileNumber,
                                            cleanUtr,
                                            screenshotBase64
                                        )
                                    }
                                },
                                enabled = !isSubmitting && !isProcessingImage,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("submit_booking_button")
                            ) {
                                if (isSubmitting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = "Confirm Booking & Get Pass",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotGroupHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    rateHint: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = RoyalBlueDark)
        }
        Text(text = rateHint, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SlotTile(
    hour: Int,
    rangeLabel: String,
    sublabel: String,
    available: Boolean,
    isStartSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isStartSelected -> RoyalBlue
        available -> MaterialTheme.colorScheme.surface
        else -> SurfaceMuted
    }
    val borderColor = when {
        isStartSelected -> RoyalBlueDark
        available -> RoyalBlue.copy(alpha = 0.55f)
        else -> BorderBlue.copy(alpha = 0.7f)
    }
    val textColor = when {
        isStartSelected -> MaterialTheme.colorScheme.onPrimary
        available -> TextPrimary
        else -> TextSecondary.copy(alpha = 0.65f)
    }
    val subColor = when {
        isStartSelected -> MaterialTheme.colorScheme.onPrimary
        available -> RoyalBlueDark
        else -> TextSecondary.copy(alpha = 0.7f)
    }

    Surface(
        onClick = onClick,
        enabled = available || isStartSelected,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isStartSelected) 2.dp else 1.5.dp, borderColor),
        shadowElevation = if (isStartSelected) 3.dp else 0.dp,
        modifier = modifier
            .height(64.dp)
            .testTag("slot_hour_$hour")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = rangeLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sublabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isStartSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = subColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = color,
            border = BorderStroke(1.dp, border),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.size(12.dp)
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun SummaryAmountRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}
