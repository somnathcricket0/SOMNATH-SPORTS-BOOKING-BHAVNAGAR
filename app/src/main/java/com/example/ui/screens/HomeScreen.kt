package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.BoxType
import com.example.data.PlayerProfile
import com.example.data.TariffCalculator
import com.example.ui.components.dialHelpdesk
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueMid
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TurfGreen
import com.example.ui.theme.TurfGreenBg

@Composable
fun HomeScreen(
    playerProfile: PlayerProfile?,
    myBookings: List<Booking>,
    onBookBox: (BoxType) -> Unit,
    onOpenRates: () -> Unit,
    onOpenMyBookings: () -> Unit,
    onOpenPass: (Booking) -> Unit
) {
    val context = LocalContext.current
    val nextBooking = myBookings
        .filter { TariffCalculator.isBookingUpcoming(it) }
        .minByOrNull {
            TariffCalculator.bookingTimeWindowMillis(it.date, it.startHour, it.durationHours)?.first ?: Long.MAX_VALUE
        }
        ?: myBookings.firstOrNull {
            it.statusEnum() == BookingStatus.VERIFIED || it.statusEnum() == BookingStatus.PENDING_VERIFICATION
        }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Royal Blue Hero Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(RoyalBlueDark, RoyalBlue, RoyalBlueMid)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    color = GoldAccent,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "BHAVNAGAR'S PREMIER ARENA",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlueDark,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = GoldAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bhavnagar",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Text(
                                 text = "Welcome, ${playerProfile?.playerName ?: "Player"}!",
                                 style = MaterialTheme.typography.headlineMedium,
                                 color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Identical tariffs for Cricket Box & Volleyball Box. 50% advance via UPI required to hold your slot.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { onBookBox(BoxType.CRICKET) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = RoyalBlue
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_book_cricket_button")
                                ) {
                                    Text("Book Cricket", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onBookBox(BoxType.VOLLEYBALL) },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("home_book_volleyball_button")
                                ) {
                                    Text("Book Volleyball", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // Active Pass Quick Shortcut (if user has a pending/verified booking)
            if (nextBooking != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = RoyalBlueTint,
                        border = BorderStroke(1.5.dp, RoyalBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = RoyalBlue,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Latest Pass • ${nextBooking.passCode}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RoyalBlueDark
                                )
                                Text(
                                    text = "${nextBooking.boxTypeEnum().displayName} • ${nextBooking.date} (${nextBooking.timeLabel})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Button(
                                onClick = { onOpenPass(nextBooking) },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("home_view_pass_button")
                            ) {
                                Text("Pass")
                            }
                        }
                    }
                }
            }

            // Choose Your Arena Section
            item {
                Text(
                    text = "Select Sports Box",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SportBoxCard(
                        boxType = BoxType.CRICKET,
                        startingRate = "From ₹500/hr",
                        icon = Icons.Filled.SportsCricket,
                        testTag = "home_cricket_card",
                        onClick = { onBookBox(BoxType.CRICKET) },
                        modifier = Modifier.weight(1f)
                    )
                    SportBoxCard(
                        boxType = BoxType.VOLLEYBALL,
                        startingRate = "From ₹500/hr",
                        icon = Icons.Filled.SportsVolleyball,
                        testTag = "home_volleyball_card",
                        onClick = { onBookBox(BoxType.VOLLEYBALL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Tariff Strip
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, BorderBlue),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "Approved Tariff (Both Boxes)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            androidx.compose.material3.TextButton(
                                onClick = onOpenRates,
                                modifier = Modifier.testTag("home_full_rates_button")
                            ) {
                                Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoyalBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Full Rates", color = RoyalBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                        QuickRateLine("Mon–Fri Day (6 AM – 6 PM)", "1h ₹500 • 2h ₹800 • 3h ₹1,100")
                        QuickRateLine("Mon–Fri Night (6 PM – 6 AM)", "1h ₹700 • 2h ₹1,300 • 3h ₹1,800")
                        QuickRateLine("Sat & Sun (All Hours)", "1h ₹800 • 2h ₹1,400 • 3h ₹2,000")
                    }
                }
            }

            // Venue Amenities & UPI Policy
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, BorderBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Arena & Cafe Highlights",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AmenityChip(Icons.Filled.Lightbulb, "LED Floodlights", Modifier.weight(1f))
                            AmenityChip(Icons.Filled.Coffee, "On-site Cafe", Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AmenityChip(Icons.Filled.QrCode2, "50% UPI Advance", Modifier.weight(1f))
                            AmenityChip(Icons.Filled.WbSunny, "24-Hr Booking", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Helpdesk Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TurfGreenBg.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, TurfGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Somnath Cricket Box & Cafe Helpdesk",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "Bhavnagar • Call ${TariffCalculator.HELPDESK_DISPLAY} for tournaments & group queries",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { dialHelpdesk(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("home_helpdesk_call_button")
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call")
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenMyBookings,
                    border = BorderStroke(1.dp, RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("home_my_bookings_button")
                ) {
                    Text("View My Bookings & Passes", color = RoyalBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SportBoxCard(
    boxType: BoxType,
    startingRate: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, BorderBlue),
        shadowElevation = 2.dp,
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RoyalBlueLight,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = boxType.displayName,
                        tint = RoyalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = boxType.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = boxType.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Surface(
                color = RoyalBlueTint,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = startingRate,
                    style = MaterialTheme.typography.labelMedium,
                    color = RoyalBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickRateLine(tier: String, prices: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = tier, style = MaterialTheme.typography.labelMedium, color = RoyalBlueDark)
        Text(text = prices, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AmenityChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = RoyalBlueTint,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
        }
    }
}
