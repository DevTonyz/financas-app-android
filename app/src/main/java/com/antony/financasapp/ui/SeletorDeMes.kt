package com.antony.financasapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// "< novembro de 2026 >". Usado no painel e na tela do grupo
@Composable
fun SeletorDeMes(texto: String, aoVoltar: () -> Unit, aoAvancar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = aoVoltar) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(texto, style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = aoAvancar) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }
}

// Soma (ou subtrai) meses cuidando da virada de ano: (12, 2026) + 1 = (1, 2027)
fun deslocarMes(mes: Int, ano: Int, delta: Int): Pair<Int, Int> {
    // Converte para "meses desde o ano 0" para a virada de ano sair de graça
    val total = ano * 12 + (mes - 1) + delta
    return (total % 12 + 1) to (total / 12)
}
