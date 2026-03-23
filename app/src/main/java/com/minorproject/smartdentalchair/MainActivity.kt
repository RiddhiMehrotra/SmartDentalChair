package com.minorproject.smartdentalchair

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val MedicalBlue = Color(0xFF0066FF)
val MedicalBlueLight = Color(0xFFE6F0FF)
val SurfaceBackground = Color(0xFFF8FAFC)
val CardBorder = Color(0xFFE2E8F0)
val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val EmergencyRed = Color(0xFFDC2626)
val SuccessGreen = Color(0xFF16A34A)

// Emulator -> 10.0.2.2, Physical phone -> your laptop IP
private const val BACKEND_BASE_URL = "http://172.20.10.4:5000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = SurfaceBackground) {
                    AppNavigation()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "CHAIR_NOTIF",
                "Chair Commands",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for dental chair actions" }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val chairViewModel: ChairViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("signup") { SignUpScreen(navController) }
        composable("dashboard") { DentalChairDashboard(navController, chairViewModel) }
    }
}

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.HealthAndSafety,
            null,
            tint = MedicalBlue,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "SmartDental Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text("Login to your account", color = TextSecondary)

        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                loginUser(
                    email = email.trim(),
                    password = password,
                    context = context,
                    navController = navController,
                    onLoadingChange = { isLoading = it }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
        ) {
            Text(if (isLoading) "Logging in..." else "Login", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = { navController.navigate("signup") }) {
            Text("Create an account")
        }
    }
}

@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Sign Up",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MedicalBlue
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                signupUser(
                    email = email.trim(),
                    password = password,
                    context = context,
                    navController = navController,
                    onLoadingChange = { isLoading = it }
                )
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
        ) {
            Text(if (isLoading) "Creating..." else "Create Account", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Back to Login")
        }
    }
}

fun loginUser(
    email: String,
    password: String,
    context: Context,
    navController: NavController,
    onLoadingChange: (Boolean) -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Email and password are required", Toast.LENGTH_SHORT).show()
        return
    }

    onLoadingChange(true)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("$BACKEND_BASE_URL/api/auth/login")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val jsonInput = """{"email":"$email","password":"$password"}"""
            conn.outputStream.use { it.write(jsonInput.toByteArray()) }

            val responseCode = conn.responseCode
            val responseText = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            }
            conn.disconnect()

            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                if (responseCode == 200) {
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Login failed: $responseText", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

fun signupUser(
    email: String,
    password: String,
    context: Context,
    navController: NavController,
    onLoadingChange: (Boolean) -> Unit
) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Email and password are required", Toast.LENGTH_SHORT).show()
        return
    }

    onLoadingChange(true)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL("$BACKEND_BASE_URL/api/auth/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val jsonInput = """{"email":"$email","password":"$password"}"""
            conn.outputStream.use { it.write(jsonInput.toByteArray()) }

            val responseCode = conn.responseCode
            val responseText = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
            }
            conn.disconnect()

            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                if (responseCode == 201) {
                    Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show()
                    navController.navigate("login")
                } else {
                    Toast.makeText(context, "Signup failed: $responseText", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onLoadingChange(false)
                Toast.makeText(context, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentalChairDashboard(navController: NavController, viewModel: ChairViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        viewModel.loadSettings(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val recognizedText =
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!recognizedText.isNullOrEmpty()) {
            viewModel.spokenText = recognizedText
            processVoice(recognizedText, viewModel, context)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Dashboard, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.showSettings = true
                    },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Logout, null) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("SmartDental Pro", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.showSettings = true }) {
                            Icon(Icons.Default.Settings, "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            if (viewModel.showSettings) {
                SettingsDialog(
                    viewModel.esp32Ip,
                    { viewModel.showSettings = false },
                    { viewModel.saveSettings(context, it) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            listOf(MedicalBlueLight.copy(0.3f), SurfaceBackground)
                        )
                    )
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hello Doctor 👋",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StatusCard(viewModel.connectionStatus, viewModel.isSending)

                Spacer(Modifier.height(24.dp))
                SectionLabel("Primary Controls")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlCard(Modifier.weight(1f), "Chair Up", Icons.Outlined.ArrowUpward) {
                        viewModel.sendCommand("up", "Lift Up", context)
                    }
                    ControlCard(Modifier.weight(1f), "Chair Down", Icons.Outlined.ArrowDownward) {
                        viewModel.sendCommand("down", "Lower Down", context)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlCard(
                        Modifier.weight(1f),
                        "Chair Forward",
                        Icons.AutoMirrored.Outlined.ArrowForward
                    ) {
                        viewModel.sendCommand("forward", "Move Forward", context)
                    }
                    ControlCard(Modifier.weight(1f), "Recline Back", Icons.Outlined.Refresh) {
                        viewModel.sendCommand("recline", "Recline Back", context)
                    }
                }

                Spacer(Modifier.height(20.dp))
                PresetButton("Preset Treatment Position", Icons.Default.Adjust) {
                    viewModel.sendCommand("preset", "Treatment Position", context)
                }

                Spacer(Modifier.height(24.dp))
                SectionLabel("Voice Assistant")
                VoiceCard(viewModel.spokenText) {
                    startVoiceCapture(context, speechLauncher)
                }

                Spacer(Modifier.height(24.dp))
                SectionLabel("Safety")
                EmergencyButton {
                    viewModel.sendCommand("emergency", "EMERGENCY STOP", context)
                }

                Spacer(Modifier.height(24.dp))
                SectionLabel("Activity Log")
                LogPanel(viewModel.logList)

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val action: String,
    val isSuccess: Boolean
)

class ChairViewModel : ViewModel() {
    var connectionStatus by mutableStateOf("System Ready")
    val logList = mutableStateListOf<LogEntry>()
    var isSending by mutableStateOf(false)
    var esp32Ip by mutableStateOf("192.168.4.1")
    var showSettings by mutableStateOf(false)
    var spokenText by mutableStateOf("Waiting for voice command...")

    fun loadSettings(context: Context) {
        val prefs = context.getSharedPreferences("chair_prefs", Context.MODE_PRIVATE)
        esp32Ip = prefs.getString("esp32_ip", "192.168.4.1") ?: "192.168.4.1"
    }

    fun saveSettings(context: Context, newIp: String) {
        esp32Ip = newIp
        context.getSharedPreferences("chair_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("esp32_ip", newIp)
            .apply()
        showSettings = false
    }

    fun sendCommand(commandPath: String, label: String, context: Context) {
        if (isSending) return
        isSending = true

        CoroutineScope(Dispatchers.IO).launch {
            val success = try {
                val url = URL("http://$esp32Ip/$commandPath")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 2000
                val code = conn.responseCode
                conn.disconnect()
                code == 200
            } catch (_: Exception) {
                false
            }

            withContext(Dispatchers.Main) {
                isSending = false
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                logList.add(0, LogEntry(time = time, action = label, isSuccess = success))
                connectionStatus = if (success) "Success: $label" else "Connection Failed"
                sendNotification(
                    context,
                    if (success) "Command Executed" else "Command Failed",
                    "$label: ${if (success) "Success" else "Fail"}"
                )
                logToBackend(label, if (success) "Success" else "Failed")
            }
        }
    }

    fun logToBackend(action: String, status: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BACKEND_BASE_URL/api/logs")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val jsonInputString = """{"action":"$action","status":"$status"}"""
                conn.outputStream.use { it.write(jsonInputString.toByteArray()) }

                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotification(context: Context, title: String, msg: String) {
        val builder = NotificationCompat.Builder(context, "CHAIR_NOTIF")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

@Composable
fun StatusCard(status: String, isWorking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isWorking) MedicalBlue else SuccessGreen)
                    .then(if (!isWorking) Modifier.graphicsLayer(alpha = alpha) else Modifier)
            )
            Spacer(Modifier.width(12.dp))
            Text(status, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ControlCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MedicalBlue, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun PresetButton(title: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun EmergencyButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, EmergencyRed),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
    ) {
        Icon(Icons.Default.Warning, null)
        Spacer(Modifier.width(12.dp))
        Text("EMERGENCY STOP", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun LogPanel(logs: List<LogEntry>) {
    Card(
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        LazyColumn(modifier = Modifier.padding(8.dp)) {
            items(logs, key = { it.id }) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        log.time,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        log.action,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        null,
                        tint = if (log.isSuccess) SuccessGreen else EmergencyRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                HorizontalDivider(color = CardBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun VoiceCard(text: String, onMicClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MedicalBlue.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMicClick,
                modifier = Modifier.size(54.dp).background(MedicalBlueLight, CircleShape)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MedicalBlue)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Voice Assistant", fontSize = 12.sp, color = TextSecondary)
                Text(text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(currentIp: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var ipText by remember { mutableStateOf(currentIp) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chair Configuration", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter ESP32 IP Address:", fontSize = 14.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ipText,
                    onValueChange = { ipText = it },
                    placeholder = { Text("e.g. 192.168.4.1") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(ipText) },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalBlue)
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

fun startVoiceCapture(
    context: Context,
    launcher: androidx.activity.result.ActivityResultLauncher<Intent>
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        return
    }

    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Command the chair...")
    }

    try {
        launcher.launch(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Speech not supported", Toast.LENGTH_SHORT).show()
    }
}

fun processVoice(text: String, vm: ChairViewModel, context: Context) {
    val cmd = text.lowercase()
    when {
        "up" in cmd -> vm.sendCommand("up", "Voice: Up", context)
        "down" in cmd -> vm.sendCommand("down", "Voice: Down", context)
        "forward" in cmd -> vm.sendCommand("forward", "Voice: Forward", context)
        "recline" in cmd -> vm.sendCommand("recline", "Voice: Recline", context)
        "preset" in cmd || "treatment" in cmd -> vm.sendCommand("preset", "Voice: Preset", context)
        "stop" in cmd || "emergency" in cmd -> vm.sendCommand("emergency", "Voice: Stop", context)
    }
}