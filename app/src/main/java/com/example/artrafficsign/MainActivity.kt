package com.example.artrafficsign

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.artrafficsign.ui.screens.AppNavigation
import com.example.artrafficsign.ui.theme.ARTrafficSignTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ARTrafficSignTheme {
                AppNavigation()
            }
        }
    }
}
