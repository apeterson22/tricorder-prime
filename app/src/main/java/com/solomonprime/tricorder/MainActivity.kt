package com.solomonprime.tricorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solomonprime.tricorder.ui.EmSpectrumScannerScreen
import com.solomonprime.tricorder.ui.EnvironmentalScannerScreen
import com.solomonprime.tricorder.ui.GeophysicalScannerScreen
import com.solomonprime.tricorder.ui.LcarsNavigationBar
import com.solomonprime.tricorder.ui.theme.LcarsBlack
import com.solomonprime.tricorder.ui.theme.TricorderPrimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TricorderPrimeTheme {
                TricorderApp()
            }
        }
    }
}

@Composable
fun TricorderApp() {
    val navController = rememberNavController()
    val navItems = listOf("ENV", "GEO", "EM")
    
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
    }
}
