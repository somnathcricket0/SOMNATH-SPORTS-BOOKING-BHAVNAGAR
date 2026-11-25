package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.TariffCalculator
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

private enum class MyBookingFilter(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    PAST("Past"),
    PENDING("Pending"),
    VERIFIED("Verified"),
    CLOSED("Cancelled / Rejected")
}

@Composable
fun MyBookingsScreen(
    bookingsState: UiState<List<Booking>>,
    isSubmitting: Boolean,
    onOpenPass: (Booking) -> Unit,
    onCancelBooking: (Booking) -> Unit,
    onBookNow: () -> Unit,
    onRefresh: () -> Unit = {},
    onPreviewScreenshot: (Bitmap) -> Unit = {}
) {
    var activeFilter by remember { mutableStateOf(MyBookingFilter.ALL) }
    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (bookingsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = RoyalBlue)
                        Text(
                            text = "Loading your turf bookings...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxSize()
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
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Could Not Load Bookings",
                                style = MaterialTheme.typography.titleMedium,
                                color = RejectedRed
                            )
                            Text(
                                text = bookingsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("my_bookings_retry_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry Sync")
                            }
                        }
                    }
                }
            }
            is UiState.Success -> {
                val allBookings = bookingsState.data
                val nowMillis = remember(allBookings) { System.currentTimeMillis() }

                val upcomingBookings = remember(allBookings, nowMillis) {
                    allBookings
                        .filter { TariffCalculator.isBookingUpcoming(it, nowMillis) }
                        .sortedBy {
                            TariffCalculator.bookingTimeWindowMillis(it.date, it.startHour, it.durationHours)?.first
                                ?: Long.MAX_VALUE
                        }
                }
                val pastBookings = remember(allBookings, nowMillis) {
                    allBookings
                        .filter { TariffCalculator.isBookingPast(it, nowMillis) }
                        .sortedByDescending {
                            TariffCalculator.bookingTimeWindowMillis(it.date, it.startHour, it.durationHours)?.first
                                ?: 0L
                        }
                }
                val futureClosedBookings = remember(allBookings, nowMillis) {
                    allBookings.filter {
                        !TariffCalculator.isBookingPast(it, nowMillis) &&
                            (it.statusEnum() == BookingStatus.CANCELLED || it.statusEnum() == BookingStatus.REJECTED)
                    }
                }
                val pendingCount = remember(allBookings) {
                    allBookings.count { it.statusEnum() == BookingStatus.PENDING_VERIFICATION }
                }

                val filteredList = remember(allBookings, upcomingBookings, pastBookings, activeFilter) {
                    when (activeFilter) {
                        MyBookingFilter.ALL -> allBookings
                        MyBookingFilter.UPCOMING -> upcomingBookings
                        MyBookingFilter.PAST -> pastBookings
                        MyBookingFilter.PENDING -> allBookings.filter { it.statusEnum() == BookingStatus.PENDING_VERIFICATION }
                        MyBookingFilter.VERIFIED -> allBookings.filter { it.statusEnum() == BookingStatus.VERIFIED }
                        MyBookingFilter.CLOSED -> allBookings.filter {
                            it.statusEnum() == BookingStatus.CANCELLED || it.statusEnum() == BookingStatus.REJECTED
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "My Bookings",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = VerifiedGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Upcoming & past turf passes saved on device",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("my_bookings_refresh_button")
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Refresh bookings",
                                    tint = RoyalBlue
                                )
                            }
                        }
                    }

                    // Summary count strip
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BookingSummaryStatCard(
                                label = "Upcoming",
                                count = upcomingBookings.size,
                                bgColor = RoyalBlueLight,
                                fgColor = RoyalBlueDark,
                                modifier = Modifier.weight(1f)
                            )
                            BookingSummaryStatCard(
                                label = "Past",
                                count = pastBookings.size,
                                bgColor = SurfaceMuted,
                                fgColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            BookingSummaryStatCard(
                                label = "Pending",
                                count = pendingCount,
                                bgColor = PendingAmberBg,
                                fgColor = PendingAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Filter chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MyBookingFilter.entries) { filter ->
                                val selected = filter == activeFilter
                                FilterChip(
                                    selected = selected,
                                    onClick = { activeFilter = filter },
                                    label = { Text(filter.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RoyalBlue,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("my_bookings_filter_${filter.name.lowercase()}")
                                )
                            }
                        }
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            EmptyBookingsCard(
                                filter = activeFilter,
                                hasAnyBookings = allBookings.isNotEmpty(),
                                onBookNow = onBookNow
                            )
                        }
                    } else if (activeFilter == MyBookingFilter.ALL) {
                        if (upcomingBookings.isNotEmpty()) {
                            item {
                                BookingSectionHeader(
                                    icon = Icons.Filled.Schedule,
                                    title = "Upcoming Bookings (${upcomingBookings.size})",
                                    tint = RoyalBlue
                                )
                            }
                            items(upcomingBookings, key = { "up_${it.id}" }) { booking ->
                                MyBookingCard(
                                    booking = booking,
                                    isPast = false,
                                    isSubmitting = isSubmitting,
                                    onOpenPass = { onOpenPass(booking) },
                                    onRequestCancel = { bookingToCancel = booking },
                                    onPreviewScreenshot = onPreviewScreenshot
                                )
                            }
                        }

                        if (pastBookings.isNotEmpty()) {
                            item {
                                BookingSectionHeader(
                                    icon = Icons.Filled.History,
                                    title = "Past Bookings (${pastBookings.size})",
                                    tint = TextSecondary
                                )
                            }
                            items(pastBookings, key = { "past_${it.id}" }) { booking ->
                                MyBookingCard(
                                    booking = booking,
                                    isPast = true,
                                    isSubmitting = isSubmitting,
                                    onOpenPass = { onOpenPass(booking) },
                                    onRequestCancel = { bookingToCancel = booking },
                                    onPreviewScreenshot = onPreviewScreenshot
                                )
                            }
                        }

                        if (futureClosedBookings.isNotEmpty()) {
                            item {
                                BookingSectionHeader(
                                    icon = Icons.Filled.Cancel,
                                    title = "Cancelled / Rejected (${futureClosedBookings.size})",
                                    tint = RejectedRed
                                )
                            }
                            items(futureClosedBookings, key = { "closed_${it.id}" }) { booking ->
                                MyBookingCard(
                                    booking = booking,
                                    isPast = false,
                                    isSubmitting = isSubmitting,
                                    onOpenPass = { onOpenPass(booking) },
                                    onRequestCancel = { bookingToCancel = booking },
                                    onPreviewScreenshot = onPreviewScreenshot
                                )
                            }
                        }
                    } else {
                        items(filteredList, key = { it.id }) { booking ->
                            val isPast = TariffCalculator.isBookingPast(booking, nowMillis)
                            MyBookingCard(
                                booking = booking,
                                isPast = isPast,
                                isSubmitting = isSubmitting,
                                onOpenPass = { onOpenPass(booking) },
                                onRequestCancel = { bookingToCancel = booking },
                                onPreviewScreenshot = onPreviewScreenshot
                            )
                        }
                    }
                }
            }
        }
    }

    bookingToCancel?.let { target ->
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            title = { Text("Cancel Booking ${target.passCode}?") },
            text = {
                Text("This will cancel your pending booking for ${target.date} (${target.timeLabel}) and release the turf slot for other players.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(target)
                        bookingToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RejectedRed)
                ) {
                    Text("Cancel Booking")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("Keep Slot")
                }
            }
        )
    }
}

@Composable
private fun BookingSummaryStatCard(
    label: String,
    count: Int,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = fgColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fgColor
            )
        }
    }
}

@Composable
private fun BookingSectionHeader(
    icon: ImageVector,
    title: String,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun EmptyBookingsCard(
    filter: MyBookingFilter,
    hasAnyBookings: Boolean,
    onBookNow: () -> Unit
) {
    val heading = when {
        !hasAnyBookings -> "No Turf Bookings Yet"
        filter == MyBookingFilter.UPCOMING -> "No Upcoming Bookings"
        filter == MyBookingFilter.PAST -> "No Past Bookings Yet"
        else -> "No Bookings in '${filter.label}'"
    }
    val subtext = when {
        filter == MyBookingFilter.UPCOMING ->
            "Book Cricket Box or Volleyball Box for today or the next 14 days with 50% UPI advance."
        filter == MyBookingFilter.PAST ->
            "Completed turf sessions will appear here after their scheduled slot ends."
        else ->
            "Reserve your Cricket Box or Volleyball Box slot in seconds with 50% UPI advance."
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderBlue),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RoyalBlueLight,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.EventAvailable,
                        contentDescription = null,
                        tint = RoyalBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(text = heading, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                text = subtext,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onBookNow,
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("empty_bookings_book_button")
            ) {
                Text("Book a Slot Now")
            }
        }
    }
}

@Composable
private fun MyBookingCard(
    booking: Booking,
    isPast: Boolean,
    isSubmitting: Boolean,
    onOpenPass: () -> Unit,
    onRequestCancel: () -> Unit,
    onPreviewScreenshot: (Bitmap) -> Unit
) {
    val status = booking.statusEnum()
    val isActiveUpcoming = !isPast && (status == BookingStatus.VERIFIED || status == BookingStatus.PENDING_VERIFICATION)
    val screenshotBitmap = remember(booking.screenshotBase64) {
        ImageCompressionHelper.decodeBase64ToBitmap(booking.screenshotBase64)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isActiveUpcoming) RoyalBlue.copy(alpha = 0.45f) else BorderBlue),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_card_${booking.passCode}")
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxTypeBadge(boxType = booking.boxTypeEnum())
                    Surface(
                        color = if (isPast) SurfaceMuted else if (isActiveUpcoming) VerifiedGreenBg else SurfaceMuted,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isPast) "Past" else if (isActiveUpcoming) "Upcoming" else "Closed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPast) TextSecondary else if (isActiveUpcoming) VerifiedGreen else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                StatusBadge(status = status)
            }

            Column {
                Text(
                    text = "${booking.date} • ${booking.timeLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "${booking.durationHours} hr • ${booking.tierLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Surface(
                color = RoyalBlueTint,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pass Code", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = booking.passCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlueDark
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Adv: ${formatInr(booking.advanceAmount)} / ${formatInr(booking.totalAmount)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = RoyalBlue
                        )
                        Text(
                            text = "UTR: ${booking.utrNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (screenshotBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPreviewScreenshot(screenshotBitmap) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Image(
                            bitmap = screenshotBitmap.asImageBitmap(),
                            contentDescription = "Payment screenshot thumbnail",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("UPI Payment Screenshot Attached", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                            Text("Tap to view receipt", style = MaterialTheme.typography.bodySmall, color = RoyalBlue)
                        }
                    }
                }
            }

            if (!booking.adminNote.isNullOrBlank()) {
                Text(
                    text = "Admin Note: ${booking.adminNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = BorderBlue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenPass,
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("view_pass_${booking.passCode}")
                ) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Pass")
                }
                if (status == BookingStatus.PENDING_VERIFICATION && !isPast) {
                    OutlinedButton(
                        onClick = onRequestCancel,
                        enabled = !isSubmitting,
                        border = BorderStroke(1.dp, RejectedRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectedRed),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("cancel_booking_${booking.passCode}")
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
