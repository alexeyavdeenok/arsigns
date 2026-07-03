package com.example.artrafficsign.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.artrafficsign.viewmodel.AppViewModel
import com.example.artrafficsign.viewmodel.SettingsViewModel
import com.example.domain.model.AppSettings
import com.example.domain.model.DetectedSign
import com.example.domain.model.SignEntity
import com.example.domain.model.YoloModelType
import kotlinx.coroutines.launch

sealed class AppScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : AppScreen("home", "Главная", Icons.Default.Home)
    object History : AppScreen("history", "История", Icons.Default.History)
    object Settings : AppScreen("settings", "Настройки", Icons.Default.Settings)
    object Detail : AppScreen("detail/{signId}", "Информация", Icons.Default.Info) {
        fun createRoute(signId: Int) = "detail/$signId"
    }
}

@Composable
fun AppNavigation(appViewModel: AppViewModel, settingsViewModel: SettingsViewModel) {
    val navController = rememberNavController()
    val items = listOf(AppScreen.Home, AppScreen.History, AppScreen.Settings)

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            if (currentRoute in items.map { it.route }) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppScreen.Home.route) {
                HomeScreen(
                    appViewModel = appViewModel,
                    onSignSelected = { signId -> navController.navigate(AppScreen.Detail.createRoute(signId)) }
                )
            }
            composable(AppScreen.History.route) {
                HistoryScreen(
                    appViewModel = appViewModel,
                    onSignSelected = { signId -> navController.navigate(AppScreen.Detail.createRoute(signId)) }
                )
            }
            composable(AppScreen.Settings.route) {
                val settings by settingsViewModel.settings.collectAsState()
                val isVoiceAvailable by appViewModel.isVoiceAvailable.collectAsState()
                val isCvAvailable by appViewModel.isCvAvailable.collectAsState()

                SettingsScreen(
                    appSettingsState = settings,
                    isVoiceAvailable = isVoiceAvailable,
                    isCvAvailable = isCvAvailable,
                    onToggleVoice = { enabled -> settingsViewModel.toggleTts(enabled) },
                    onSelectModel = { model -> settingsViewModel.updateSelectedModel(model) },
                    onThresholdChange = { threshold -> settingsViewModel.updateConfidenceThreshold(threshold) }
                )
            }
            composable(AppScreen.Detail.route) { backStackEntry ->
                val signId = backStackEntry.arguments?.getString("signId")?.toIntOrNull()
                SignDetailScreen(
                    signId = signId,
                    appViewModel = appViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(appViewModel: AppViewModel, onSignSelected: (Int) -> Unit) {
    val liveDetectedSigns by appViewModel.liveDetectedSigns.collectAsState()
    val isCvAvailable by appViewModel.isCvAvailable.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Camera Layer
        CameraPreview(modifier = Modifier.fillMaxSize())

        // 2. Detection Layer (Bounding Boxes)
        DetectionOverlay(
            detectedSigns = liveDetectedSigns,
            onSignClick = onSignSelected
        )

        // 3. UI Overlay for Status
        if (!isCvAvailable) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
                InfoMessage("Модуль CV не подключен. Камера работает в режиме превью.")
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier
    )
}

@Composable
fun DetectionOverlay(
    detectedSigns: List<DetectedSign>,
    onSignClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            detectedSigns.forEach { sign ->
                val left = sign.xMin * size.width
                val top = sign.yMin * size.height
                val width = (sign.xMax - sign.xMin) * size.width
                val height = (sign.yMax - sign.yMin) * size.height

                drawRect(
                    color = Color.Green,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        // Interaction layer
        detectedSigns.forEach { sign ->
            val left = sign.xMin * maxWidth.value
            val top = sign.yMin * maxHeight.value
            val width = (sign.xMax - sign.xMin) * maxWidth.value
            val height = (sign.yMax - sign.yMin) * maxHeight.value

            Box(
                modifier = Modifier
                    .offset(x = left.dp, y = top.dp)
                    .size(width = width.dp, height = height.dp)
                    .clickable { onSignClick(sign.id) }
            )
        }
    }
}

@Composable
fun HistoryScreen(appViewModel: AppViewModel, onSignSelected: (Int) -> Unit) {
    val historySigns by appViewModel.historySigns.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("История распознавания", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (historySigns.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История пока пуста", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historySigns) { sign ->
                    SignItem(sign = sign, onClick = { onSignSelected(sign.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignDetailScreen(signId: Int?, appViewModel: AppViewModel, onBack: () -> Unit) {
    val allSigns by appViewModel.allSigns.collectAsState()
    val isVoiceAvailable by appViewModel.isVoiceAvailable.collectAsState()
    val sign = remember(signId, allSigns) {
        if (signId == null) null else allSigns.firstOrNull { it.id == signId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sign?.title ?: "Информация") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Назад") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (sign == null) {
                InfoMessage("Знак не найден.")
            } else {
                Text(sign.title, style = MaterialTheme.typography.headlineMedium)
                Text("Код ПДД: ${sign.pddCode}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Описание", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(sign.description)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { appViewModel.speakText(sign.ttsTitle) },
                    enabled = isVoiceAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isVoiceAvailable) "Прослушать описание" else "Озвучка недоступна")
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appSettingsState: AppSettings,
    isVoiceAvailable: Boolean,
    isCvAvailable: Boolean,
    onToggleVoice: suspend (Boolean) -> Unit,
    onSelectModel: suspend (YoloModelType) -> Unit,
    onThresholdChange: suspend (Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    var localThreshold by remember { mutableStateOf(appSettingsState.yoloConfidenceThreshold) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        StatusCard(isCvAvailable, isVoiceAvailable)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Голосовые уведомления", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Включить озвучку", modifier = Modifier.weight(1f))
            Switch(
                checked = appSettingsState.isVoiceAlertsEnabled,
                onCheckedChange = { scope.launch { onToggleVoice(it) } },
                enabled = isVoiceAvailable
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Порог уверенности: ${(localThreshold * 100).toInt()}%", fontWeight = FontWeight.Bold)
        Slider(
            value = localThreshold,
            onValueChange = { localThreshold = it },
            onValueChangeFinished = { scope.launch { onThresholdChange(localThreshold) } },
            valueRange = 0.1f..1.0f
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Модель YOLO", fontWeight = FontWeight.Bold)
        YoloModelType.values().forEach { model ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { onSelectModel(model) } }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = appSettingsState.selectedModel == model,
                    onClick = { scope.launch { onSelectModel(model) } }
                )
                Text(model.uiName, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
fun StatusCard(isCvAvailable: Boolean, isVoiceAvailable: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Статус системы", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            StatusItem("Компьютерное зрение", isCvAvailable)
            StatusItem("Синтез речи (TTS)", isVoiceAvailable)
        }
    }
}

@Composable
fun StatusItem(label: String, isAvailable: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (isAvailable) Color.Green else Color.Red, RoundedCornerShape(5.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
        Spacer(modifier = Modifier.weight(1f))
        Text(if (isAvailable) "Доступен" else "Не готов", fontSize = 12.sp)
    }
}

@Composable
fun InfoMessage(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SignItem(sign: SignEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.LightGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(sign.pddCode, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(sign.title, fontWeight = FontWeight.Bold)
                Text(sign.description, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        }
    }
}
