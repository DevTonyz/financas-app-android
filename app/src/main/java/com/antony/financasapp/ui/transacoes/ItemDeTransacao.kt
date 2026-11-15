package com.antony.financasapp.ui.transacoes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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

/**
 * Uma linha de transação. Usada no painel, na lista completa e no grupo.
 * [aoTocar] nulo = não dá para tocar (gasto de outro membro do grupo, que só o dono pode editar).
 * [autor] aparece embaixo, no grupo, para mostrar quem pagou.
 */
@Composable
fun ItemDeTransacao(transacao: TransacaoResponse, aoTocar: (() -> Unit)?, autor: String? = null) {
    val conteudo: @Composable RowScope.() -> Unit = {
        Column(Modifier.weight(1f)) {
            Text(transacao.descricao, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${transacao.nomeCategoria ?: "Sem categoria"} • ${transacao.data}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            autor?.let {
                Text(
                    "Pago por $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(formatarReais(transacao.valor), fontWeight = FontWeight.SemiBold)
    }
    // O OutlinedCard com onClick mostra o efeito de toque; sem onClick, é só um cartão
    if (aoTocar != null) {
        OutlinedCard(onClick = aoTocar, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, content = conteudo)
        }
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, content = conteudo)
        }
    }
}
