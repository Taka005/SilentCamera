package com.taka.silentcamera.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.taka.silentcamera.ui.screens.MainScreen
import com.taka.silentcamera.ui.theme.SilentCameraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilentCameraTheme {
                MainScreen()
            }
        }
    }
}