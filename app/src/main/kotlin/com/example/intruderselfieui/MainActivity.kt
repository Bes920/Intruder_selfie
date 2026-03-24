@file:OptIn(ExperimentalLayoutApi::class)

package com.example.intruderselfieui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private const val ownerPassword = "2468"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntruderSelfieApp()
                }
            }
        }
    }
}

private data class IntruderCapture(
    val timeLabel: String,
    val attemptNumber: Int,
    val status: String,
    val location: String
)

private data class AccessPreview(
    val enteredPassword: String,
    val failedAttempts: Int,
    val threshold: Int,
    val ownerNote: String?,
    val accessState: AccessState
) {
    val isThresholdReached: Boolean
        get() = failedAttempts >= threshold
}

private sealed class AccessState {
    data object Locked : AccessState()
    data object Granted : AccessState()
    data class Error(val message: String) : AccessState()
}

private sealed class PreviewScreen(val title: String) {
    data object LockScreen : PreviewScreen("Lock Screen")
    data object Gallery : PreviewScreen("Gallery")
}

private val sampleCaptures = listOf(
    IntruderCapture("Today, 08:14", 2, "Encrypted", "Front door"),
    IntruderCapture("Today, 10:42", 3, "Pending owner review", "Study room"),
    IntruderCapture("Yesterday, 21:05", 4, "Archived", "Living room")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun IntruderSelfieApp() {
    var selectedScreen: PreviewScreen by remember { mutableStateOf(PreviewScreen.LockScreen) }
    var passwordInput by remember { mutableStateOf("") }
    var ownerNote by remember { mutableStateOf("Owner password is required before the evidence gallery opens.") }
    var accessState: AccessState by remember { mutableStateOf(AccessState.Locked) }

    val attempts = sampleCaptures.maxOf { it.attemptNumber }
    val threshold = 2
    val accessPreview = AccessPreview(
        enteredPassword = passwordInput,
        failedAttempts = attempts,
        threshold = threshold,
        ownerNote = ownerNote.ifBlank { null },
        accessState = accessState
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Intruder Selfie")
                        Text(
                            text = "UI prototype built in Kotlin + Compose",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF3F7F6), Color(0xFFE3ECE8))
                    )
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                end = 16.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCard(accessPreview = accessPreview)
            }
            item {
                TabRow(
                    selectedTabIndex = if (selectedScreen is PreviewScreen.LockScreen) 0 else 1,
                    containerColor = Color.Transparent
                ) {
                    listOf(PreviewScreen.LockScreen, PreviewScreen.Gallery).forEach { screen ->
                        Tab(
                            selected = selectedScreen::class == screen::class,
                            onClick = {
                                selectedScreen = if (screen is PreviewScreen.Gallery && accessState !is AccessState.Granted) {
                                    accessState = AccessState.Error("Enter the correct owner password to open the gallery.")
                                    PreviewScreen.LockScreen
                                } else {
                                    screen
                                }
                            },
                            text = { Text(screen.title) }
                        )
                    }
                }
            }
            item {
                SecurityOverview(accessPreview = accessPreview)
            }
            item {
                PasswordEntryCard(
                    passwordInput = passwordInput,
                    onPasswordChange = {
                        passwordInput = it.take(6).filter(Char::isDigit)
                        if (accessState is AccessState.Error) {
                            accessState = AccessState.Locked
                        }
                    },
                    onDigitPressed = { digit ->
                        if (passwordInput.length < 6) {
                            passwordInput += digit
                            if (accessState is AccessState.Error) {
                                accessState = AccessState.Locked
                            }
                        }
                    },
                    onBackspace = {
                        passwordInput = passwordInput.dropLast(1)
                        if (accessState is AccessState.Error) {
                            accessState = AccessState.Locked
                        }
                    },
                    onClear = {
                        passwordInput = ""
                        accessState = AccessState.Locked
                    },
                    onUnlock = {
                        accessState = verifyOwnerPassword(passwordInput)
                        if (accessState is AccessState.Granted) {
                            selectedScreen = PreviewScreen.Gallery
                        }
                    },
                    accessState = accessState,
                    ownerNote = ownerNote,
                    onOwnerNoteChange = { ownerNote = it }
                )
            }
            when (selectedScreen) {
                PreviewScreen.LockScreen -> {
                    item {
                        LockScreenPreview(accessPreview = accessPreview)
                    }
                }

                PreviewScreen.Gallery -> {
                    item {
                        GalleryPreview(captures = sampleCaptures)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(accessPreview: AccessPreview) {
    val threatColor = when {
        accessPreview.failedAttempts == 0 -> Color(0xFF2D6A4F)
        accessPreview.isThresholdReached -> Color(0xFF9A3412)
        else -> Color(0xFF1D4ED8)
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102A43))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Owner-facing preview of the security app experience",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "The UI shows the lock screen, capture trigger logic, and a protected evidence gallery without implementing the real camera flow yet.",
                color = Color(0xFFD9E2EC)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryChip(
                    label = "Failed attempts",
                    value = accessPreview.failedAttempts.toString(),
                    accent = threatColor,
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "Capture threshold",
                    value = accessPreview.threshold.toString(),
                    accent = Color(0xFF0F766E),
                    modifier = Modifier.weight(1f)
                )
                SummaryChip(
                    label = "Access",
                    value = accessLabel(accessPreview.accessState),
                    accent = Color(0xFFF4D35E),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color(0xFFBCCCDC), style = MaterialTheme.typography.labelMedium)
            Text(value, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SecurityOverview(accessPreview: AccessPreview) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Security Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "This first version focuses on UI only. The password field works locally so you can preview the flow before adding camera, Room, biometrics, or encryption.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("Configured capture threshold: ${accessPreview.threshold} failed attempts")
            Text("Latest simulated attempt count: ${accessPreview.failedAttempts}")
            Text("Next behavior: ${captureStatus(accessPreview)}")
        }
    }
}

@Composable
private fun PasswordEntryCard(
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    onDigitPressed: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onUnlock: () -> Unit,
    accessState: AccessState,
    ownerNote: String,
    onOwnerNoteChange: (String) -> Unit
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Owner Password Entry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Use the text field or keypad to enter a password. For this UI prototype, the sample owner password is `2468`.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = passwordInput,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Owner password") },
                supportingText = { Text(accessMessage(accessState)) },
                singleLine = true
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                (1..9).forEach { digit ->
                    KeypadButton(label = digit.toString()) { onDigitPressed(digit.toString()) }
                }
                KeypadButton(label = "Clear") { onClear() }
                KeypadButton(label = "0") { onDigitPressed("0") }
                KeypadButton(label = "Del") { onBackspace() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Unlock Gallery")
                }
            }
            OutlinedTextField(
                value = ownerNote,
                onValueChange = onOwnerNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Owner note") },
                minLines = 2
            )
        }
    }
}

@Composable
private fun LockScreenPreview(accessPreview: AccessPreview) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Lock Screen Preview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This screen simulates the owner PIN entry flow. The capture action is represented visually only.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1F33)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Secure Access",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    PinDots(pinLength = accessPreview.enteredPassword.length)
                    Text(
                        text = if (accessPreview.enteredPassword.isBlank()) {
                            "Waiting for password input"
                        } else {
                            "Entered password: ${accessPreview.enteredPassword}"
                        },
                        color = Color(0xFFD9E2EC)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        (1..9).forEach { number ->
                            NumberBubble(number.toString())
                        }
                        NumberBubble("C")
                        NumberBubble("0")
                        NumberBubble("<")
                    }
                    TextButton(onClick = { }) {
                        Text("Use biometric unlock")
                    }
                }
            }

            StatusPanel(accessPreview = accessPreview)
        }
    }
}

@Composable
private fun PinDots(pinLength: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (index < pinLength) Color(0xFF0F766E) else Color(0xFFBCCCDC),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(width = 96.dp, height = 52.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(label)
    }
}

@Composable
private fun NumberBubble(label: String) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Color.White.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPanel(accessPreview: AccessPreview) {
    val stageColor = when {
        accessPreview.isThresholdReached -> Color(0xFFFDE68A)
        accessPreview.failedAttempts > 0 -> Color(0xFFBFDBFE)
        else -> Color(0xFFC7F9CC)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = stageColor.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Capture Flow State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("Access state: ${accessLabel(accessPreview.accessState)}")
            Text("Failed attempts: ${accessPreview.failedAttempts}")
            Text("Threshold: ${accessPreview.threshold}")
            Text("Current action: ${captureStatus(accessPreview)}")
            accessPreview.ownerNote?.let {
                Text("Owner note: $it")
            }
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Preview silent capture sequence")
            }
        }
    }
}

@Composable
private fun GalleryPreview(captures: List<IntruderCapture>) {
    val encryptedCount = captures.count { it.status == "Encrypted" }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Protected Gallery Preview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "A future implementation would unlock this screen only after valid PIN or biometric confirmation. For now, it demonstrates the visual structure of the evidence log.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("All captures") }
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Encrypted: $encryptedCount") }
                )
            }
            captures.forEach { capture ->
                CaptureCard(capture = capture)
            }
        }
    }
}

@Composable
private fun CaptureCard(capture: IntruderCapture) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = capture.timeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Attempt ${capture.attemptNumber}",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text("Status: ${capture.status}")
            Text("Location tag: ${capture.location}")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0B1F33), Color(0xFF355C7D))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Encrypted image placeholder",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun captureStatus(accessPreview: AccessPreview): String = when {
    accessPreview.failedAttempts <= 0 -> "Monitoring only"
    accessPreview.isThresholdReached -> "Capture armed after failed password entries"
    else -> "Warning state"
}

private fun verifyOwnerPassword(input: String): AccessState = when {
    input.isBlank() -> AccessState.Error("Enter the owner password first.")
    input == ownerPassword -> AccessState.Granted
    else -> AccessState.Error("Wrong password. Gallery remains locked.")
}

private fun accessLabel(accessState: AccessState): String = when (accessState) {
    AccessState.Granted -> "Unlocked"
    AccessState.Locked -> "Locked"
    is AccessState.Error -> "Denied"
}

private fun accessMessage(accessState: AccessState): String = when (accessState) {
    AccessState.Granted -> "Access granted. Gallery preview is available."
    AccessState.Locked -> "Enter the sample password 2468 to unlock."
    is AccessState.Error -> accessState.message
}

@Preview(showBackground = true)
@Composable
private fun IntruderSelfieAppPreview() {
    MaterialTheme {
        IntruderSelfieApp()
    }
}
