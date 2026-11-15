package com.antony.financasapp.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Tela inicial provisória: na etapa 4 vira o painel do mês
@Composable
fun TelaPrincipal(onSair: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bem-vindo às suas Finanças!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // A tela só avisa que o usuário quer sair; quem apaga o token e navega é o AppNavHost
        Button(onClick = onSair) {
            Text("Sair da Conta")
        }
    }
}
