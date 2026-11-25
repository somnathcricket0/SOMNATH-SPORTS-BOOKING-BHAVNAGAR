package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PhoneNumberHelper
import com.example.data.TariffCalculator
import com.example.ui.components.UncroppedSomnathLogo
import com.example.ui.components.dialHelpdesk
import com.example.ui.theme.BorderBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueLight
import com.example.ui.theme.RoyalBlueTint
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VerifiedGreen

/**
 * Instant Mobile-Number-Only Player Sign-In screen.
 * Validates a 10-digit Indian mobile number (with or without +91 / spaces)
 * plus optional Player Full Name and signs the player in immediately with zero OTP or Google prompt.
 */
@Composable
fun AuthScreen(
    isSubmitting: Boolean,
    authErrorMessage: String?,
    onClearError: () -> Unit,
    onContinueWithMobile: (mobileTenDigits: String, playerName: String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var mobileInput by rememberSaveable { mutableStateOf("") }
    var playerName by rememberSaveable { mutableStateOf("") }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    val normalizedPreview = PhoneNumberHelper.normalizeIndianMobile(mobileInput)

    fun submitLogin() {
        focusManager.clearFocus()
        val normalized = PhoneNumberHelper.normalizeIndianMobile(mobileInput)
        val trimmedName = playerName.trim()
        when {
            normalized == null -> {
                validationError = "Please enter a valid 10-digit mobile number (with or without +91)."
            }
            trimmedName.isNotEmpty() && trimmedName.length < 2 -> {
                validationError = "Player name must be at least 2 characters or left blank."
            }
            trimmedName.length > 60 -> {
                validationError = "Player name must be 60 characters or fewer."
            }
            else -> {
                validationError = null
                onClearError()
                onContinueWithMobile(normalized, trimmedName)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RoyalBlueDark, RoyalBlue, RoyalBlueTint),
                    startY = 0f,
                    endY = 1200f
                )
            )
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent Uncropped Somnath Logo Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    UncroppedSomnathLogo(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .testTag("login_somnath_logo")
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f)),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "OFFICIAL VENUE BOOKING APP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = stringResource(id = R.string.brand_full_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Cricket Box & Volleyball Box • Bhavnagar • Helpdesk: ${TariffCalculator.HELPDESK_DISPLAY}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }

            // Player Sign-In Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = RoyalBlueLight,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.SportsCricket,
                                    contentDescription = null,
                                    tint = RoyalBlue
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Player Sign In",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enter your 10-digit mobile number to start booking",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = mobileInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '+' || it == ' ' || it == '-' }.take(18)
                            mobileInput = filtered
                            validationError = null
                            if (!authErrorMessage.isNullOrBlank()) onClearError()
                        },
                        enabled = !isSubmitting,
                        label = { Text("10-Digit Mobile Number *") },
                        placeholder = { Text("e.g., 9876543210 or +91 98765 43210") },
                        leadingIcon = {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = RoyalBlue)
                        },
                        supportingText = {
                            if (normalizedPreview != null) {
                                Text(
                                    text = "Ready: +91 $normalizedPreview",
                                    color = VerifiedGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text("Supports numbers with or without +91 / spaces")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_mobile_input")
                    )

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = {
                            playerName = it.take(60)
                            validationError = null
                            if (!authErrorMessage.isNullOrBlank()) onClearError()
                        },
                        enabled = !isSubmitting,
                        label = { Text("Player Full Name (Optional)") },
                        placeholder = { Text("e.g., Vishal Gohil") },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = RoyalBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitLogin() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = BorderBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_name_input")
                    )

                    val displayError = validationError ?: authErrorMessage
                    if (!displayError.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { submitLogin() },
                        enabled = !isSubmitting,
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("mobile_login_button")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continue with Mobile Number", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Text(
                        text = "Instant login — no OTP or password required. Your profile & bookings are saved directly on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Helpdesk Bar
            OutlinedButton(
                onClick = { dialHelpdesk(context) },
                border = BorderStroke(1.dp, RoyalBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = RoyalBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("login_helpdesk_button")
            ) {
                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Need Help? Call Helpdesk: ${TariffCalculator.HELPDESK_DISPLAY}")
            }
        }
    }
}
