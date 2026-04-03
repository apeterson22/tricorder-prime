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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solomonprime.tricorder.ui.AcousticScannerScreen
import com.solomonprime.tricorder.ui.EmSpectrumScannerScreen
import com.solomonprime.tricorder.ui.EnvironmentalScannerScreen
import com.solomonprime.tricorder.ui.GeophysicalScannerScreen
import com.solomonprime.tricorder.ui.LcarsNavigationBar
import com.solomonprime.tricorder.ui.LcarsButton
import com.solomonprime.tricorder.ui.theme.LcarsBlack
import com.solomonprime.tricorder.ui.theme.LcarsOrange
import com.solomonprime.tricorder.ui.theme.TricorderPrimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TricorderPrimeTheme {
                PermissionWrapper {
                    TricorderApp()
                }
            }
        }
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
fun TricorderApp() {
    val navController = rememberNavController()
    val navItems = listOf("ENV", "GEO", "EM", "ACO")
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "ENV"

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
            }
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
        startDestination = "ENV",
        modifier = modifier
    ) {
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
    }
}
