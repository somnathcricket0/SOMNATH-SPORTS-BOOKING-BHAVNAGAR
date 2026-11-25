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
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.BoxType
import com.example.data.TariffCalculator
import com.example.ui.components.dialHelpdesk
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg

@Composable
fun RatesScreen(
    onBookBox: (BoxType) -> Unit
) {
    val context = LocalContext.current

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
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(RoyalBlueDark, RoyalBlue)))
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Rates & Combo Offers",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "Identical transparent tariffs for both Cricket Box and Volleyball Box. Book 2 or 3 hours to unlock multi-hour savings!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Filled.SportsCricket, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cricket Box", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                    }
                                }
                                Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(50)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(Icons.Filled.SportsVolleyball, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Volleyball Box", style = MaterialTheme.typography.labelMedium, color = RoyalBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                TariffTierCard(
                    title = "Mon – Fri Day",
                    window = "6:00 AM – 6:00 PM (Slots 6 AM – 5 PM, ends by 6 PM)",
                    icon = Icons.Filled.WbSunny,
                    oneHr = "₹500",
                    twoHr = "₹800",
                    threeHr = "₹1,100",
                    offerBadge = "Save ₹200 on 2h • Save ₹400 on 3h"
                )
            }

            item {
                TariffTierCard(
                    title = "Mon – Fri Night",
                    window = "6:00 PM – 6:00 AM (Slots 6 PM – 5 AM, ends by 6 AM)",
                    icon = Icons.Filled.NightsStay,
                    oneHr = "₹700",
                    twoHr = "₹1,300",
                    threeHr = "₹1,800",
                    offerBadge = "Save ₹100 on 2h • Save ₹300 on 3h"
                )
            }

            item {
                TariffTierCard(
                    title = "Sat & Sun Weekend",
                    window = "All Hours (24x7)",
                    icon = Icons.Filled.Weekend,
                    oneHr = "₹800",
                    twoHr = "₹1,400",
                    threeHr = "₹2,000",
                    offerBadge = "Save ₹200 on 2h • Save ₹400 on 3h"
                )
            }

            // 50% Advance Policy Card
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RoyalBlueTint,
                    border = BorderStroke(1.dp, BorderBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payments, contentDescription = null, tint = RoyalBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "50% Advance UPI Policy",
                                style = MaterialTheme.typography.titleMedium,
                                color = RoyalBlueDark
                            )
                        }
                        Text(
                            text = "• Pay only 50% advance via UPI (${TariffCalculator.UPI_ID}) to lock your slot.\n• Upload your UTR reference and payment screenshot for fast admin verification.\n• Pay the remaining 50% balance at Somnath Cricket Box & Cafe before kick-off.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Book CTAs + Helpdesk
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onBookBox(BoxType.CRICKET) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("rates_book_cricket_button")
                    ) {
                        Text("Book Cricket")
                    }
                    Button(
                        onClick = { onBookBox(BoxType.VOLLEYBALL) },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlueDark),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("rates_book_volleyball_button")
                    ) {
                        Text("Book Volleyball")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { dialHelpdesk(context) },
                    border = BorderStroke(1.dp, RoyalBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("rates_helpdesk_button")
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Corporate / Tournament Bookings: ${TariffCalculator.HELPDESK_DISPLAY}")
                }
            }
        }
    }
}

@Composable
private fun TariffTierCard(
    title: String,
    window: String,
    icon: ImageVector,
    oneHr: String,
    twoHr: String,
    threeHr: String,
    offerBadge: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BorderBlue),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = RoyalBlueLight,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = RoyalBlue)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(text = window, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }

            HorizontalDivider(color = BorderBlue)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TariffColumn("1 Hour", oneHr, Modifier.weight(1f))
                TariffColumn("2 Hours", twoHr, Modifier.weight(1f))
                TariffColumn("3 Hours", threeHr, Modifier.weight(1f))
            }

            Surface(
                color = VerifiedGreenBg,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = offerBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = VerifiedGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun TariffColumn(duration: String, price: String, modifier: Modifier = Modifier) {
    Surface(
        color = RoyalBlueTint,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(text = duration, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = price,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RoyalBlue
            )
        }
    }
}
