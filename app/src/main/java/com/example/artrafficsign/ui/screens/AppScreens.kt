package com.example.artrafficsign.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.artrafficsign.viewmodel.AppViewModel
import com.example.artrafficsign.viewmodel.CameraViewModel
import com.example.artrafficsign.viewmodel.SettingsViewModel
import com.example.domain.model.ActiveSign
import com.example.domain.model.AppSettings
import com.example.domain.model.SignEntity
import com.example.domain.model.YoloModelType
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest


sealed class AppScreen(val route: String, val label: String, val icon: ImageVector) {
    object Home : AppScreen("home", "Главная", Icons.Default.Home)
    object History : AppScreen("history", "История", Icons.Default.History)
    object Settings : AppScreen("settings", "Настройки", Icons.Default.Settings)
    object Detail : AppScreen("detail/{signId}", "Информация", Icons.Default.Info) {
        fun createRoute(signId: Int) = "detail/$signId"
    }
}

@Composable
fun AppNavigation(
    appViewModel: AppViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    cameraViewModel: CameraViewModel = hiltViewModel()
) {
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
                    cameraViewModel = cameraViewModel,
                    onSignSelected = { signId -> navController.navigate(AppScreen.Detail.createRoute(signId)) }
                )
            }
            composable(AppScreen.History.route) {
                HistoryScreen(
                    cameraViewModel = cameraViewModel,
                    onSignSelected = { signId -> navController.navigate(AppScreen.Detail.createRoute(signId)) }
                )
            }
            composable(AppScreen.Settings.route) {
                val settings by settingsViewModel.settingsState.collectAsState()

                SettingsScreen(
                    appSettingsState = settings,
                    onToggleVoice = { enabled -> settingsViewModel.onTtsToggled(enabled) },
                    onSelectModel = { model -> settingsViewModel.onModelSelected(model) },
                    onThresholdChange = { threshold -> settingsViewModel.onConfidenceChanged(threshold) }
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
fun HomeScreen(cameraViewModel: CameraViewModel, onSignSelected: (Int) -> Unit) {
    val uiState by cameraViewModel.uiState.collectAsState()

    DisposableEffect(Unit) {
        cameraViewModel.startDetection()
        onDispose { cameraViewModel.stopDetection() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DetectionOverlay(
            activeSigns = uiState,
            onSignClick = { signId ->
                onSignSelected(signId)
            }
        )
    }
}

@Composable
fun DetectionOverlay(
    activeSigns: List<ActiveSign>,
    onSignClick: (Int) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            activeSigns.forEach { sign ->
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

        activeSigns.forEach { sign ->
            val left = sign.xMin * maxWidth.value
            val top = sign.yMin * maxHeight.value
            val width = (sign.xMax - sign.xMin) * maxWidth.value
            val height = (sign.yMax - sign.yMin) * maxHeight.value

            Box(
                modifier = Modifier
                    .offset(x = left.dp, y = top.dp)
                    .size(width = width.dp, height = height.dp)
                    .clickable { onSignClick(sign.sign.id) }
            )
        }
    }
}

@Composable
fun HistoryScreen(cameraViewModel: CameraViewModel, onSignSelected: (Int) -> Unit) {
    val historyState by cameraViewModel.historyState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("История распознавания", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (historyState.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("История пока пуста", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historyState) { sign ->
                    SignItem(sign = sign, onClick = { onSignSelected(sign.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignDetailScreen(signId: Int?, appViewModel: AppViewModel, onBack: () -> Unit) {
    val selectedSign by appViewModel.selectedSign.collectAsState()
    val sign = if (selectedSign?.id == signId) selectedSign else null

    LaunchedEffect(signId) {
        if (signId != null) {
            appViewModel.loadSign(signId)
        }
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
                AssetSvgImage(
                    assetPath = sign.svgPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
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
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appSettingsState: AppSettings,
    onToggleVoice: (Boolean) -> Unit,
    onSelectModel: (YoloModelType) -> Unit,
    onThresholdChange: (Float) -> Unit
) {
    var localThreshold by remember(appSettingsState.yoloConfidenceThreshold) {
        mutableStateOf(appSettingsState.yoloConfidenceThreshold)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Голосовые уведомления", fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            Text("Включить озвучку", modifier = Modifier.weight(1f))
            Switch(
                checked = appSettingsState.isVoiceAlertsEnabled,
                onCheckedChange = { onToggleVoice(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Порог уверенности: ${(localThreshold * 100).toInt()}%", fontWeight = FontWeight.Bold)
        Slider(
            value = localThreshold,
            onValueChange = { localThreshold = it },
            onValueChangeFinished = { onThresholdChange(localThreshold) },
            valueRange = 0.1f..1.0f
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Модель YOLO", fontWeight = FontWeight.Bold)
        YoloModelType.entries.forEach { model ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectModel(model) }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = appSettingsState.selectedModel == model,
                    onClick = { onSelectModel(model) }
                )
                Text(model.uiName, modifier = Modifier.padding(start = 8.dp))
            }
        }
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
fun AssetSvgImage(assetPath: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val normalizedPath = remember(assetPath) {
        assetPath
            .trim()
            .replace('\\', '/')
            .removePrefix("/")
            .replace("sources/ signs/", "sources/signs/")
    }

    if (normalizedPath.isBlank()) {
        Box(modifier = modifier.background(Color.LightGray, RoundedCornerShape(4.dp)))
        return
    }

    val imageRequest = remember(context, normalizedPath) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$normalizedPath")
            .decoderFactory(SvgDecoder.Factory())
            .crossfade(false)
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
        alignment = Alignment.Center
    )
}

@Composable
fun SignItem(sign: SignEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetSvgImage(
                assetPath = sign.svgPath,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(sign.title, fontWeight = FontWeight.Bold)
                Text(sign.description, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontSize = 12.sp)
            }
        }
    }
}
