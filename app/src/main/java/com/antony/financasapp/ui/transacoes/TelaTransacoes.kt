package com.antony.financasapp.ui.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.data.FiltroTransacoes
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.dto.TransacaoResponse
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.dataIsoParaExibir
import com.antony.financasapp.ui.millisUtcParaIso
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

// Todas as transações, com filtros e "carregar mais" ao rolar até o fim
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaTransacoes(
    onVoltar: () -> Unit,
    onNova: () -> Unit,
    onEditar: (Long) -> Unit,
    viewModel: TransacoesViewModel = viewModel {
        TransacoesViewModel(app().transacaoRepository, app().categoriaRepository)
    }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            avisos.showSnackbar(it)
            viewModel.avisoMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transações") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNova) {
                Icon(Icons.Filled.Add, contentDescription = "Nova transação")
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { espacoDasBarras ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacoDasBarras)
                .padding(horizontal = 16.dp)
        ) {
            BarraDeFiltros(estado.filtro, estado.categorias, viewModel::filtrar)

            Box(Modifier.fillMaxSize()) {
                when {
                    estado.carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                    estado.erro != null -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(estado.erro!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.carregar() }) { Text("Tentar de novo") }
                    }

                    estado.transacoes.isEmpty() -> Text(
                        if (estado.filtro == FiltroTransacoes.Todas) "Nenhuma transação ainda"
                        else "Nenhuma transação com este filtro",
                        modifier = Modifier.align(Alignment.Center)
                    )

                    else -> ListaDeTransacoes(
                        transacoes = estado.transacoes,
                        carregandoMais = estado.carregandoMais,
                        aoChegarNoFim = viewModel::carregarMais,
                        aoTocar = { onEditar(it.id) },
                        aoArrastar = viewModel::pedirExclusao
                    )
                }
            }
        }
    }

    estado.paraExcluir?.let { transacao ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusao,
            title = { Text("Excluir transação?") },
            text = { Text("\"${transacao.descricao}\" será apagada.") },
            confirmButton = { TextButton(onClick = viewModel::confirmarExclusao) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarExclusao) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListaDeTransacoes(
    transacoes: List<TransacaoResponse>,
    carregandoMais: Boolean,
    aoChegarNoFim: () -> Unit,
    aoTocar: (TransacaoResponse) -> Unit,
    aoArrastar: (TransacaoResponse) -> Unit
) {
    val estadoDaLista = rememberLazyListState()

    // snapshotFlow transforma o estado da rolagem num fluxo. Quando o último item visível
    // está a 2 do fim, pedimos a próxima página. distinctUntilChanged evita pedir várias vezes
    LaunchedEffect(estadoDaLista) {
        snapshotFlow {
            val info = estadoDaLista.layoutInfo
            val ultimoVisivel = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            ultimoVisivel >= info.totalItemsCount - 2
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { aoChegarNoFim() }
    }

    LazyColumn(
        state = estadoDaLista,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 88.dp) // espaço para o botão "+" não cobrir o último item
    ) {
        items(transacoes, key = { it.id }) { transacao ->
            // Arrastar para a esquerda pede a exclusão. Devolvemos false para o item voltar
            // ao lugar: quem decide se apaga é o diálogo de confirmação
            val estadoDoArraste = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) aoArrastar(transacao)
                    false
                }
            )
            SwipeToDismissBox(
                state = estadoDoArraste,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                }
            ) {
                ItemDeTransacao(transacao, aoTocar = { aoTocar(transacao) })
            }
        }
        if (carregandoMais) {
            item {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }
    }
}

// Chips de filtro: categoria (menu) e período (calendário de intervalo). Só um vale por vez
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraDeFiltros(
    filtro: FiltroTransacoes,
    categorias: List<CategoriaResponse>,
    aoFiltrar: (FiltroTransacoes) -> Unit
) {
    var menuAberto by remember { mutableStateOf(false) }
    var calendarioAberto by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            FilterChip(
                selected = filtro is FiltroTransacoes.PorCategoria,
                onClick = { menuAberto = true },
                label = { Text((filtro as? FiltroTransacoes.PorCategoria)?.nome ?: "Categoria") }
            )
            DropdownMenu(expanded = menuAberto, onDismissRequest = { menuAberto = false }) {
                categorias.forEach { categoria ->
                    DropdownMenuItem(
                        text = { Text(categoria.nome) },
                        onClick = {
                            menuAberto = false
                            aoFiltrar(FiltroTransacoes.PorCategoria(categoria.id, categoria.nome))
                        }
                    )
                }
            }
        }

        FilterChip(
            selected = filtro is FiltroTransacoes.PorPeriodo,
            onClick = { calendarioAberto = true },
            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(18.dp)) },
            label = {
                Text(
                    (filtro as? FiltroTransacoes.PorPeriodo)
                        ?.let { "${dataIsoParaExibir(it.inicio)} a ${dataIsoParaExibir(it.fim)}" }
                        ?: "Período"
                )
            }
        )

        if (filtro != FiltroTransacoes.Todas) {
            AssistChip(
                onClick = { aoFiltrar(FiltroTransacoes.Todas) },
                label = { Text("Limpar") },
                leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null, Modifier.size(18.dp)) }
            )
        }
    }

    if (calendarioAberto) {
        val estadoDoCalendario = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { calendarioAberto = false },
            confirmButton = {
                val inicio = estadoDoCalendario.selectedStartDateMillis
                // Se escolheu só um dia, o período é aquele dia
                val fim = estadoDoCalendario.selectedEndDateMillis ?: inicio
                TextButton(
                    enabled = inicio != null,
                    onClick = {
                        calendarioAberto = false
                        aoFiltrar(FiltroTransacoes.PorPeriodo(millisUtcParaIso(inicio!!), millisUtcParaIso(fim!!)))
                    }
                ) { Text("Filtrar") }
            },
            dismissButton = { TextButton(onClick = { calendarioAberto = false }) { Text("Cancelar") } }
        ) {
            DateRangePicker(state = estadoDoCalendario, modifier = Modifier.weight(1f))
        }
    }
}
