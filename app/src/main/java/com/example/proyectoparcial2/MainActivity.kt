package com.example.proyectoparcial2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.proyectoparcial2.ui.theme.ProyectoParcial2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProyectoParcial2Theme(darkTheme = true, dynamicColor = false) {
                CalculatorScreen()
            }
        }
    }
}
