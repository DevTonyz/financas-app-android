package com.antony.financasapp.ui.painel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.formatarReais
import com.antony.financasapp.ui.mesPorExtenso

// Tela inicial: o mês selecionado, o total gasto e as últimas transações
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPainel(
    onSair: () -> Unit,
    viewModel: PainelViewModel = viewModel { PainelViewModel(app().transacaoRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas finanças") },
                actions = {
                    IconButton(onClick = onSair) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { espacoDasBarras ->
        // espacoDasBarras = o quanto a TopAppBar ocupa, para o conteúdo não ficar embaixo dela
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacoDasBarras)
                .padding(horizontal = 16.dp)
        ) {
            SeletorDeMes(
                texto = mesPorExtenso(estado.mes, estado.ano),
                aoVoltar = viewModel::mesAnterior,
                aoAvancar = viewModel::proximoMes
            )

            when {
                estado.carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                estado.erro != null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(estado.erro!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::carregar) { Text("Tentar de novo") }
                }

                else -> {
                    CardDoTotal(formatarReais(estado.total))
                    Spacer(Modifier.height(16.dp))
                    Text("Últimas transações", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (estado.transacoes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum gasto neste mês")
                        }
                    } else {
                        // LazyColumn só desenha os itens visíveis na tela (como o RecyclerView)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(estado.transacoes, key = { it.id }) { ItemDeTransacao(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeletorDeMes(texto: String, aoVoltar: () -> Unit, aoAvancar: () -> Unit) {
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

@Composable
private fun CardDoTotal(total: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Total gasto no mês", style = MaterialTheme.typography.labelLarge)
            Text(total, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ItemDeTransacao(transacao: TransacaoResponse) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
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
