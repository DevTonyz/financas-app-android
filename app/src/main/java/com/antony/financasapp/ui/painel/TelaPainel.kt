package com.antony.financasapp.ui.painel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.ui.SeletorDeMes
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.formatarReais
import com.antony.financasapp.ui.mesPorExtenso
import com.antony.financasapp.ui.transacoes.ItemDeTransacao

// Tela inicial: o mês selecionado, o total gasto e as últimas transações
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPainel(
    onAbrirCategorias: () -> Unit,
    onAbrirGrupos: () -> Unit,
    onVerTodas: () -> Unit,
    onNovaTransacao: () -> Unit,
    onEditarTransacao: (Long) -> Unit,
    onSair: () -> Unit,
    viewModel: PainelViewModel = viewModel { PainelViewModel(app().transacaoRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas finanças") },
                actions = { MenuDoPainel(onAbrirCategorias, onAbrirGrupos, onSair) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNovaTransacao) {
                Icon(Icons.Filled.Add, contentDescription = "Nova transação")
            }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Últimas transações", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onVerTodas) { Text("Ver todas") }
                    }

                    if (estado.transacoes.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhum gasto neste mês")
                        }
                    } else {
                        // LazyColumn só desenha os itens visíveis na tela (como o RecyclerView)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            // Espaço no fim para o botão "+" não cobrir o último item
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(estado.transacoes, key = { it.id }) {
                                ItemDeTransacao(it, aoTocar = { onEditarTransacao(it.id) })
                            }
                        }
                    }
                }
            }
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

// Menu dos três pontinhos na barra superior. Aberto/fechado é só visual, por isso fica na tela
@Composable
private fun MenuDoPainel(onAbrirCategorias: () -> Unit, onAbrirGrupos: () -> Unit, onSair: () -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { aberto = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
        }
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            DropdownMenuItem(
                text = { Text("Categorias") },
                onClick = { aberto = false; onAbrirCategorias() }
            )
            DropdownMenuItem(
                text = { Text("Grupos") },
                onClick = { aberto = false; onAbrirGrupos() }
            )
            DropdownMenuItem(
                text = { Text("Sair") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                onClick = { aberto = false; onSair() }
            )
        }
    }
}
