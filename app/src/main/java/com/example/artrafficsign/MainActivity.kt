package com.example.artrafficsign

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.artrafficsign.ui.screens.AppNavigation
import com.example.artrafficsign.ui.theme.ARTrafficSignTheme
import com.example.artrafficsign.viewmodel.AppViewModel
import com.example.artrafficsign.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request camera permission on launch
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)

        setContent {
            ARTrafficSignTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                
                AppNavigation(
                    appViewModel = appViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
