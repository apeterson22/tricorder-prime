package com.solomonprime.tricorder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solomonprime.tricorder.data.AlertMonitorService
import com.solomonprime.tricorder.data.VoiceAlertManager
import com.solomonprime.tricorder.ui.AcousticScannerScreen
import com.solomonprime.tricorder.ui.DashboardScreen
import com.solomonprime.tricorder.ui.BioScannerScreen
import com.solomonprime.tricorder.ui.Obd2Screen
import com.solomonprime.tricorder.ui.PipeLeakScreen
import com.solomonprime.tricorder.ui.RadiationScannerScreen
import com.solomonprime.tricorder.ui.RfSecurityScreen
import com.solomonprime.tricorder.ui.WifiMapScreen
import com.solomonprime.tricorder.ui.EmSpectrumScannerScreen
import com.solomonprime.tricorder.ui.EnvironmentalScannerScreen
import com.solomonprime.tricorder.ui.GeophysicalScannerScreen
import com.solomonprime.tricorder.ui.LcarsNavigationBar
import com.solomonprime.tricorder.ui.LcarsButton
import com.solomonprime.tricorder.ui.theme.LcarsBlack
import com.solomonprime.tricorder.ui.theme.LcarsOrange
import com.solomonprime.tricorder.ui.theme.TricorderPrimeTheme
import com.solomonprime.tricorder.viewmodel.BioScannerViewModel
import com.solomonprime.tricorder.viewmodel.RadiationViewModel

class MainActivity : ComponentActivity() {

    private var voiceAlertManager: VoiceAlertManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize VoiceAlertManager
        voiceAlertManager = VoiceAlertManager(this)

        setContent {
            TricorderPrimeTheme {
                PermissionWrapper {
                    TricorderApp(voiceAlertManager = voiceAlertManager!!)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceAlertManager?.shutdown()
        voiceAlertManager = null
    }
}

@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    var hasPermissions by remember { mutableStateOf(false) }

    val requiredPermissions = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        hasPermissions = allGranted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    if (hasPermissions) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LcarsBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "SENSORS OFFLINE", color = LcarsOrange)
                Spacer(modifier = Modifier.height(16.dp))
                LcarsButton(
                    text = "AUTHORIZE",
                    onClick = { permissionLauncher.launch(requiredPermissions) }
                )
            }
        }
    }
}

@Composable
fun TricorderApp(voiceAlertManager: VoiceAlertManager) {
    val navController = rememberNavController()
    val navItems = listOf("HOME", "ENV", "GEO", "EM", "ACO", "BIO", "RAD", "MAP", "PIPE", "RF", "OBD")
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "ENV"

    // Get ViewModels for alert monitoring
    val radViewModel: RadiationViewModel = viewModel()
    val bioViewModel: BioScannerViewModel = viewModel()

    // Initialize AlertMonitorService
    val coroutineScope = rememberCoroutineScope()
    val alertMonitor = remember {
        AlertMonitorService(radViewModel, bioViewModel, voiceAlertManager)
    }

    // Start monitoring when composition enters, stop when it leaves
    LaunchedEffect(alertMonitor) {
        alertMonitor.start(coroutineScope)
    }

    DisposableEffect(alertMonitor) {
        onDispose {
            alertMonitor.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LcarsBlack)
            .systemBarsPadding()
    ) {
        LcarsNavigationBar(
            items = navItems,
            selectedItem = currentRoute,
            onItemSelected = { route ->
                if (route != currentRoute) {
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            activeTabIds = setOf("HOME", "ENV", "GEO", "EM"),
            hasAlert = false
        )
        
        TricorderNavHost(
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}

@Composable
fun TricorderNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "HOME",
        modifier = modifier
    ) {
        composable("HOME") {
            DashboardScreen()
        }
        composable("ENV") {
            EnvironmentalScannerScreen()
        }
        composable("GEO") {
            GeophysicalScannerScreen()
        }
        composable("EM") {
            EmSpectrumScannerScreen()
        }
        composable("ACO") {
            AcousticScannerScreen()
        }
        composable("BIO") {
            BioScannerScreen()
        }
        composable("RAD") {
            RadiationScannerScreen()
        }
        composable("MAP") {
            WifiMapScreen()
        }
        composable("PIPE") {
            PipeLeakScreen()
        }
        composable("RF") {
            RfSecurityScreen()
        }
        composable("OBD") {
            Obd2Screen()
        }
    }
}
