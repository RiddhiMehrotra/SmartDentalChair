package com.minorproject.smartdentalchair

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF6F8FB)
                ) {
                    DentalChairDashboard()
                }
            }
        }
    }
}

data class LogItem(
    val time: String,
    val action: String,
    val status: String
)

@Composable
fun DentalChairDashboard() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    var spokenText by remember { mutableStateOf("No voice command yet") }
    var connectionStatus by remember { mutableStateOf("Ready") }

    val logList = remember { mutableStateListOf<LogItem>() }

    // Change this IP according to your ESP32
    val esp32BaseUrl = "http://192.168.4.1"

    fun addLog(action: String, status: String) {
        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        logList.add(
            0,
            LogItem(
                time = time,
                action = action,
                status = status
            )
        )
    }

    fun sendCommand(commandPath: String, label: String) {
        connectionStatus = "Sending $label..."

        CoroutineScope(Dispatchers.IO).launch {
            val result = sendEsp32Command("$esp32BaseUrl/$commandPath")

            withContext(Dispatchers.Main) {
                if (result) {
                    connectionStatus = "$label successful"
                    addLog(label, "Success")
                    Toast.makeText(context, "$label sent", Toast.LENGTH_SHORT).show()
                } else {
                    connectionStatus = "$label failed"
                    addLog(label, "Failed")
                    Toast.makeText(context, "$label failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun processVoiceCommand(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        spokenText = text

        when {
            "emergency" in lower || "emergency stop" in lower -> {
                sendCommand("emergency", "Voice: Emergency Stop")
            }
            "chair up" in lower || lower == "up" -> {
                sendCommand("up", "Voice: Chair Up")
            }
            "chair down" in lower || lower == "down" -> {
                sendCommand("down", "Voice: Chair Down")
            }
            "recline" in lower || "recline back" in lower -> {
                sendCommand("recline", "Voice: Recline Back")
            }
            "forward" in lower || "chair forward" in lower -> {
                sendCommand("forward", "Voice: Chair Forward")
            }
            "preset" in lower || "treatment position" in lower -> {
                sendCommand("preset", "Voice: Preset Position")
            }
            "stop" in lower -> {
                sendCommand("stop", "Voice: Stop")
            }
            else -> {
                connectionStatus = "Unknown voice command"
                addLog("Voice: $text", "Unknown")
                Toast.makeText(context, "Command not recognized", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val recognizedText = matches?.firstOrNull()

        if (!recognizedText.isNullOrEmpty()) {
            processVoiceCommand(recognizedText)
        } else {
            Toast.makeText(context, "No speech detected", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a chair command")
                }
                speechLauncher.launch(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceRecognition() {
        if (isPreview) return

        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                try {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a chair command")
                    }
                    speechLauncher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FB))
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        TopBar()

        Spacer(modifier = Modifier.height(20.dp))

        WelcomeCard()

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Status: $connectionStatus",
            fontSize = 14.sp,
            color = Color(0xFF6C7590),
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ControlCard(
                modifier = Modifier.weight(1f),
                title = "Chair Up",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = "Chair Up",
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(30.dp)
                    )
                },
                onClick = { sendCommand("up", "Manual: Chair Up") }
            )

            ControlCard(
                modifier = Modifier.weight(1f),
                title = "Chair Down",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = "Chair Down",
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(30.dp)
                    )
                },
                onClick = { sendCommand("down", "Manual: Chair Down") }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ControlCard(
                modifier = Modifier.weight(1f),
                title = "Chair Forward",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.ArrowForward,
                        contentDescription = "Chair Forward",
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(30.dp)
                    )
                },
                onClick = { sendCommand("forward", "Manual: Chair Forward") }
            )

            ControlCard(
                modifier = Modifier.weight(1f),
                title = "Recline Back",
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Recline Back",
                        tint = Color(0xFF6C63FF),
                        modifier = Modifier.size(30.dp)
                    )
                },
                onClick = { sendCommand("recline", "Manual: Recline Back") }
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        GradientActionCard(
            title = "Preset Treatment Position",
            onClick = { sendCommand("preset", "Manual: Preset Position") }
        )

        Spacer(modifier = Modifier.height(18.dp))

        SectionTitle("Voice Control")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEDEBFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardVoice,
                            contentDescription = "Voice",
                            tint = Color(0xFF6C63FF)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Start Voice Command",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1F2A44)
                        )
                        Text(
                            text = spokenText,
                            fontSize = 13.sp,
                            color = Color(0xFF7A8394)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { startVoiceRecognition() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C63FF)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardVoice,
                        contentDescription = "Mic",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tap to Speak", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionTitle("Emergency Control")

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { sendCommand("emergency", "Manual: Emergency Stop") },
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE84D5B)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "EMERGENCY STOP",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        SectionTitle("Usage Log")

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (logList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No commands yet",
                        color = Color(0xFF7A8394),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .padding(16.dp)
                ) {
                    items(logList) { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEDEBFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Log",
                                    tint = Color(0xFF6C63FF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "${log.time}  •  ${log.action}",
                                    fontSize = 15.sp,
                                    color = Color(0xFF1F2A44),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = log.status,
                                    fontSize = 13.sp,
                                    color = if (log.status == "Success") Color(0xFF2E7D32)
                                    else if (log.status == "Failed") Color(0xFFC62828)
                                    else Color(0xFF7A8394)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend fun sendEsp32Command(urlString: String): Boolean {
    return try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 3000
        connection.readTimeout = 3000
        connection.connect()

        val responseCode = connection.responseCode

        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        reader.readLine()
        reader.close()
        connection.disconnect()

        responseCode == 200
    } catch (e: Exception) {
        false
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color(0xFF1F2A44)
            )
        }

        Text(
            text = "Smart Dental Chair",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2A44)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDEBFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color(0xFF6C63FF)
            )
        }
    }
}

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF1FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hello Doctor 👋",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2A3760)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Chair Control Dashboard",
                fontSize = 18.sp,
                color = Color(0xFF6C7590)
            )
        }
    }
}

@Composable
fun ControlCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF25345A)
            )
        }
    }
}

@Composable
fun GradientActionCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7568F8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowForward,
                contentDescription = "Preset",
                tint = Color.White
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = "Next",
                tint = Color.White
            )
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF25345A)
    )
}