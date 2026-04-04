package com.solomonprime.tricorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solomonprime.tricorder.data.ConnectedDeviceManager
import com.solomonprime.tricorder.ui.DashboardScreen
import com.solomonprime.tricorder.ui.theme.TricorderPrimeTheme
import com.solomonprime.tricorder.viewmodel.AcousticViewModel

/**
 * Main entry point for the Tricorder app.
 * Initializes the ConnectedDeviceManager and passes it to ViewModels.
 */
class MainActivity : ComponentActivity() {

    private lateinit var deviceManager: ConnectedDeviceManager
    private lateinit var acousticViewModel: AcousticViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize connected device manager
        deviceManager = ConnectedDeviceManager(this)
        deviceManager.start()
        deviceManager.registerAvrcp()

        // Initialize ViewModels with device manager
        acousticViewModel = AcousticViewModel(application, deviceManager)

        setContent {
            TricorderPrimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(
                        deviceManager = deviceManager
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        deviceManager.release()
    }
}
