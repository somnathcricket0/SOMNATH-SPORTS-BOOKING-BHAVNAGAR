package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.Booking
import com.example.data.BookingStatus
import com.example.data.BoxType
import com.example.data.TariffCalculator
import com.example.ui.ImageCompressionHelper
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PendingAmber
import com.example.ui.theme.PendingAmberBg
import com.example.ui.theme.RejectedRed
import com.example.ui.theme.RejectedRedBg
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.SurfaceMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenBg

fun formatInr(amount: Long): String {
    val s = amount.toString()
    if (s.length <= 3) return "₹$s"
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
    return "₹$grouped,$last3"
}

fun dialHelpdesk(context: Context) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${TariffCalculator.HELPDESK_RAW}"))
    runCatching { context.startActivity(intent) }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

@Composable
fun UncroppedSomnathLogo(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.somnath_logo),
        contentDescription = stringResource(id = R.string.logo_content_desc),
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
fun StatusBadge(
    status: BookingStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, icon) = when (status) {
        BookingStatus.VERIFIED -> Triple(VerifiedGreenBg, VerifiedGreen, Icons.Filled.Verified)
        BookingStatus.PENDING_VERIFICATION -> Triple(PendingAmberBg, PendingAmber, Icons.Filled.HourglassTop)
        BookingStatus.REJECTED -> Triple(RejectedRedBg, RejectedRed, Icons.Filled.Close)
        BookingStatus.CANCELLED -> Triple(SurfaceMuted, TextSecondary, Icons.Filled.Close)
    }
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BoxTypeBadge(boxType: BoxType, modifier: Modifier = Modifier) {
    val icon = if (boxType == BoxType.CRICKET) Icons.Filled.SportsCricket else Icons.Filled.SportsVolleyball
    Surface(
        color = RoyalBlueLight,
        contentColor = RoyalBlueDark,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = boxType.displayName, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BookingPassModal(
    booking: Booking,
    onDismiss: () -> Unit,
    onPreviewScreenshot: (Bitmap) -> Unit = {}
) {
    val context = LocalContext.current
    val status = booking.statusEnum()
    val boxType = booking.boxTypeEnum()
    val screenshotBitmap = remember(booking.screenshotBase64) {
        ImageCompressionHelper.decodeBase64ToBitmap(booking.screenshotBase64)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    // Royal Blue header with uncropped Somnath Logo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(RoyalBlueDark, RoyalBlue)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "OFFICIAL BOOKING PASS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GoldAccent
                                )
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("close_pass_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close booking pass",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            ) {
                                UncroppedSomnathLogo(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(82.dp)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SOMNATH SPORTS BOOKING – BHAVNAGAR",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Pass Body
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PASS CODE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = booking.passCode,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = RoyalBlue
                                )
                            }
                            StatusBadge(status = status)
                        }

                        HorizontalDivider(color = BorderBlue)

                        PassDetailRow("Player Name", booking.playerName)
                        PassDetailRow("Mobile Number", "+91 ${booking.playerMobile}")
                        PassDetailRow("Turf Box", boxType.displayName)
                        PassDetailRow("Date", booking.date)
                        PassDetailRow("Slot Time", "${booking.timeLabel} (${booking.durationHours} hr)")
                        PassDetailRow("Tariff Tier", booking.tierLabel)

                        Surface(
                            color = RoyalBlueTint,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BorderBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PassAmountRow("Total Tariff", formatInr(booking.totalAmount), bold = false)
                                PassAmountRow("50% Advance (UPI)", formatInr(booking.advanceAmount), bold = true, highlight = RoyalBlue)
                                PassAmountRow("Balance at Venue", formatInr(booking.balanceAmount), bold = true, highlight = VerifiedGreen)
                                HorizontalDivider(color = BorderBlue, modifier = Modifier.padding(vertical = 4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("UTR Reference", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text(
                                        text = booking.utrNumber,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        if (!booking.adminNote.isNullOrBlank()) {
                            Surface(
                                color = SurfaceMuted,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Admin Note: ${booking.adminNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
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
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Image(
                                        bitmap = screenshotBitmap.asImageBitmap(),
                                        contentDescription = "Payment screenshot thumbnail",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SurfaceMuted)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Payment Proof Attached",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Tap to view full payment screenshot",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RoyalBlue
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = VerifiedGreen
                                    )
                                }
                            }
                        }

                        // Deterministic Pass Barcode Strip
                        PassVisualBarcode(passCode = booking.passCode)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { dialHelpdesk(context) },
                                border = BorderStroke(1.dp, RoyalBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pass_helpdesk_button")
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Helpdesk", maxLines = 1)
                            }
                            Button(
                                onClick = {
                                    val shareText = buildString {
                                        appendLine("SOMNATH SPORTS BOOKING – BHAVNAGAR")
                                        appendLine("Pass: ${booking.passCode} (${status.label})")
                                        appendLine("Player: ${booking.playerName} (${booking.playerMobile})")
                                        appendLine("Turf: ${boxType.displayName}")
                                        appendLine("Date: ${booking.date} | ${booking.timeLabel}")
                                        appendLine("Total: ${formatInr(booking.totalAmount)} | 50% Advance: ${formatInr(booking.advanceAmount)} | Due at Venue: ${formatInr(booking.balanceAmount)}")
                                        appendLine("UTR: ${booking.utrNumber}")
                                        append("Helpdesk: ${TariffCalculator.HELPDESK_DISPLAY}")
                                    }
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Booking Pass"))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pass_share_button")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Pass", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PassAmountRow(
    label: String,
    value: String,
    bold: Boolean,
    highlight: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = highlight
        )
    }
}

@Composable
private fun PassVisualBarcode(passCode: String) {
    val seed = remember(passCode) { passCode.hashCode() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceMuted, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            val barCount = 36
            val slotW = size.width / barCount
            for (i in 0 until barCount) {
                val hashBit = ((seed ushr (i % 24)) xor (i * 31)) and 0x3
                if (hashBit != 0) {
                    val barW = if (hashBit == 3) slotW * 0.72f else slotW * 0.42f
                    drawRect(
                        color = RoyalBlueDark,
                        topLeft = Offset(i * slotW, 0f),
                        size = Size(barW, size.height)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Present this pass at Somnath Cricket Box & Cafe, Bhavnagar",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ScreenshotPreviewDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable(onClick = onDismiss)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "UPI Payment Screenshot",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close screenshot preview")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Full payment screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(
                                (bitmap.width.toFloat() / bitmap.height.toFloat()).coerceIn(0.45f, 1.8f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceMuted)
                    )
                }
            }
        }
    }
}
