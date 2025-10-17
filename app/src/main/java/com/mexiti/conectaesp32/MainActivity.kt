package com.mexiti.conectaesp32

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mexiti.conectaesp32.ui.Screens.MainNavigationScreen
import com.mexiti.conectaesp32.ui.theme.ConectaESP32Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ConectaESP32Theme {
              MainNavigationScreen()
            }
        }
    }
}

