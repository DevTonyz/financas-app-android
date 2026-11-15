package com.antony.financasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import com.antony.financasapp.ui.theme.FinancasTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antony.financasapp.ui.navigation.AppNavHost

// A Activity agora só abre o app; telas e navegação ficam na pasta ui/
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // FinancasTheme: cores (claro e escuro) e tipografia do app, em ui/theme/
            FinancasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}
