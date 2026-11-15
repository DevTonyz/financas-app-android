package com.antony.financasapp.ui.transacoes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.ui.formatarReais

// Uma linha de transação. Usada no painel e na lista completa; tocar abre a edição
@Composable
fun ItemDeTransacao(transacao: TransacaoResponse, aoTocar: () -> Unit) {
    OutlinedCard(onClick = aoTocar, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(transacao.descricao, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${transacao.nomeCategoria ?: "Sem categoria"} • ${transacao.data}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatarReais(transacao.valor), fontWeight = FontWeight.SemiBold)
        }
    }
}
