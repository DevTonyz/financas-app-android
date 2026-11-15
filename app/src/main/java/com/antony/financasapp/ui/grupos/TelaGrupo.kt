package com.antony.financasapp.ui.grupos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.data.DadosDoGrupoNoMes
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.dto.PagamentoResponse
import com.antony.financasapp.dto.SaldoDoMembro
import com.antony.financasapp.dto.SaldosResponse
import com.antony.financasapp.dto.SugestaoDePagamento
import com.antony.financasapp.ui.SeletorDeMes
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.dataIsoParaExibir
import com.antony.financasapp.ui.formatarReais
import com.antony.financasapp.ui.isoParaMillisUtc
import com.antony.financasapp.ui.millisUtcParaIso
import com.antony.financasapp.ui.mesPorExtenso
import com.antony.financasapp.ui.transacoes.ItemDeTransacao

// Verde para quem tem a receber. O vermelho (deve) vem do tema: colorScheme.error
private val VerdeReceber = Color(0xFF2E7D32)

// Detalhe do grupo: membros, saldo acumulado (com "Registrar pagamento") e o mês (total, gastos, pagamentos)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaGrupo(
    id: Long,
    onVoltar: () -> Unit,
    onNovoGasto: (grupoId: Long) -> Unit,
    onEditarTransacao: (Long) -> Unit,
    viewModel: GrupoViewModel = viewModel {
        GrupoViewModel(id, app().grupoRepository, app().transacaoRepository)
    }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    val contexto = LocalContext.current

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            avisos.showSnackbar(it)
            viewModel.avisoMostrado()
        }
    }
    // Saiu do grupo: Toast (sobrevive ao fechar a tela) e volta para a lista
    LaunchedEffect(estado.saiu) {
        if (estado.saiu) {
            Toast.makeText(contexto, "Você saiu do grupo", Toast.LENGTH_LONG).show()
            onVoltar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.grupo?.nome ?: "Grupo", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (estado.grupo != null) {
                FloatingActionButton(onClick = { onNovoGasto(id) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Novo gasto no grupo")
                }
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { espacoDasBarras ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(espacoDasBarras)
        ) {
            val grupo = estado.grupo
            when {
                estado.carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                estado.erro != null || grupo == null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(estado.erro ?: "Grupo não encontrado.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::carregar) { Text("Tentar de novo") }
                }

                else -> ConteudoDoGrupo(estado, grupo, viewModel, onEditarTransacao)
            }
        }
    }

    Dialogos(estado, viewModel)
}

@Composable
private fun ConteudoDoGrupo(
    estado: GrupoUiState,
    grupo: GrupoDetalheResponse,
    viewModel: GrupoViewModel,
    onEditarTransacao: (Long) -> Unit
) {
    // Uma LazyColumn só para a tela inteira rolar junto (cartões + listas)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grupo.descricao?.let { descricao ->
            item { Text(descricao, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item { CardDeMembros(grupo, aoAdicionar = viewModel::abrirAdicionar) }

        // O saldo vem antes do seletor de mês porque ele não depende do mês: soma tudo até hoje
        item {
            when {
                estado.saldos != null -> CardDoSaldo(estado, estado.saldos, aoRegistrar = viewModel::abrirRegistro)
                estado.erroSaldos != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(estado.erroSaldos, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::carregarSaldos) { Text("Tentar de novo") }
                }
                else -> LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }

        item {
            SeletorDeMes(
                texto = mesPorExtenso(estado.mes, estado.ano),
                aoVoltar = viewModel::mesAnterior,
                aoAvancar = viewModel::proximoMes
            )
        }

        val dados = estado.dadosDoMes
        when {
            estado.carregandoMes -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            estado.erroMes != null || dados == null -> item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(estado.erroMes ?: "Erro ao carregar o mês.", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::carregarMes) { Text("Tentar de novo") }
                }
            }

            else -> {
                item { CardDoMes(estado, dados) }

                item { Titulo("Gastos do grupo") }

                if (dados.transacoes.isEmpty()) {
                    item { Text("Nenhum gasto do grupo neste mês. Toque em + para lançar.") }
                } else {
                    items(dados.transacoes, key = { "t${it.id}" }) { t ->
                        val minha = t.usuarioId != null && t.usuarioId == estado.meuId
                        ItemDeTransacao(
                            transacao = t,
                            // Só quem lançou pode editar: nos gastos dos outros o toque fica desligado
                            aoTocar = if (minha) ({ onEditarTransacao(t.id) }) else null,
                            autor = if (minha) "você" else t.loginUsuario
                        )
                    }
                }

                if (dados.pagamentos.isNotEmpty()) {
                    item { Titulo("Pagamentos entre membros") }
                    items(dados.pagamentos, key = { "p${it.id}" }) { p ->
                        ItemDePagamento(estado, p, aoExcluir = { viewModel.pedirExclusaoDePagamento(p) })
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = viewModel::pedirSaida,
                enabled = !estado.salvando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) { Text("Sair do grupo") }
        }
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun CardDeMembros(grupo: GrupoDetalheResponse, aoAdicionar: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Membros (${grupo.membros.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = aoAdicionar) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Adicionar por e-mail")
                }
            }
            grupo.membros.forEach { membro ->
                Column(Modifier.padding(vertical = 2.dp)) {
                    Text(if (membro.voce) "${membro.login} (você)" else membro.login)
                    // Mostrar a data deixa claro por que alguém não divide os gastos mais antigos
                    membro.dataEntrada?.let {
                        Text(
                            "Entrou em ${dataIsoParaExibir(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Saldo acumulado: quem deve e quem recebe, e os pagamentos sugeridos para todos ficarem quites
@Composable
private fun CardDoSaldo(
    estado: GrupoUiState,
    saldos: SaldosResponse,
    aoRegistrar: (SugestaoDePagamento) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Acerto de contas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Soma de todos os meses até hoje, já descontados os pagamentos registrados.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            saldos.membros.forEach { LinhaDoSaldo(it) }

            if (saldos.sugestoes.isEmpty()) {
                Text("Todos estão quites.", fontWeight = FontWeight.SemiBold)
            } else {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Para acertar", style = MaterialTheme.typography.titleSmall)
                saldos.sugestoes.forEach { s ->
                    // A API só aceita o registro de quem está no pagamento (quem paga ou quem recebe)
                    val envolveMim = s.deUsuarioId == estado.meuId || s.paraUsuarioId == estado.meuId
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${estado.nomeNoInicio(s.deUsuarioId, s.deLogin)} " +
                                "paga ${formatarReais(s.valor)} " +
                                "a ${estado.nome(s.paraUsuarioId, s.paraLogin)}",
                            modifier = Modifier.weight(1f)
                        )
                        if (envolveMim) {
                            TextButton(onClick = { aoRegistrar(s) }) { Text("Registrar") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaDoSaldo(membro: SaldoDoMembro) {
    val sinal = membro.saldo.signum()
    val cor = when {
        sinal > 0 -> VerdeReceber
        sinal < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val situacao = when {
        sinal > 0 -> "recebe ${formatarReais(membro.saldo)}"
        // abs(): mostramos "deve R$ 30,00" em vez de "R$ -30,00"
        sinal < 0 -> "deve ${formatarReais(membro.saldo.abs())}"
        else -> "está quite"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (membro.voce) "Você" else membro.login,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(situacao, color = cor, fontWeight = FontWeight.SemiBold)
    }
}

// O mês: total do grupo, a parte de cada um e quanto cada membro pagou
@Composable
private fun CardDoMes(estado: GrupoUiState, dados: DadosDoGrupoNoMes) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Total do grupo no mês", style = MaterialTheme.typography.labelLarge)
            Text(
                formatarReais(dados.total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Parte de cada um: ${formatarReais(dados.acerto.parteJusta)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            dados.acerto.membros.forEach { m ->
                Row {
                    Text(
                        estado.nomeNoInicio(m.usuarioId, m.login) + " pagou",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(formatarReais(m.totalPago))
                }
            }
        }
    }
}

@Composable
private fun ItemDePagamento(estado: GrupoUiState, p: PagamentoResponse, aoExcluir: () -> Unit) {
    val envolveMim = p.deUsuarioId == estado.meuId || p.paraUsuarioId == estado.meuId
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${estado.nomeNoInicio(p.deUsuarioId, p.deLogin)} pagou " +
                        "a ${estado.nome(p.paraUsuarioId, p.paraLogin)}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Acerto • ${p.data}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(formatarReais(p.valor), fontWeight = FontWeight.SemiBold, color = VerdeReceber)
            if (envolveMim) {
                IconButton(onClick = aoExcluir) {
                    Icon(Icons.Filled.Delete, contentDescription = "Apagar pagamento")
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

// OptIn: o DatePicker do Material 3 ainda é marcado como experimental nesta versão
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dialogos(estado: GrupoUiState, viewModel: GrupoViewModel) {
    if (estado.adicionando) {
        AlertDialog(
            onDismissRequest = viewModel::fecharAdicionar,
            title = { Text("Adicionar membro") },
            text = {
                Column {
                    Text("A pessoa precisa ter uma conta no app.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = estado.email,
                        onValueChange = viewModel::aoMudarEmail,
                        label = { Text("E-mail") },
                        singleLine = true,
                        isError = estado.erroEmail != null,
                        supportingText = estado.erroEmail?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = viewModel::abrirDataEntrada, modifier = Modifier.fillMaxWidth()) {
                        Text("Entrou em ${dataIsoParaExibir(estado.dataEntrada)}")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Gastos de antes dessa data não serão divididos com essa pessoa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::adicionar, enabled = estado.email.isNotBlank() && !estado.salvando) {
                    Text("Adicionar")
                }
            },
            dismissButton = { TextButton(onClick = viewModel::fecharAdicionar) { Text("Cancelar") } }
        )
    }

    // Calendário da data de entrada (o mesmo do formulário de gasto; trabalha em milissegundos UTC)
    if (estado.escolhendoDataEntrada) {
        val calendario = rememberDatePickerState(initialSelectedDateMillis = isoParaMillisUtc(estado.dataEntrada))
        DatePickerDialog(
            onDismissRequest = viewModel::fecharDataEntrada,
            confirmButton = {
                TextButton(onClick = {
                    calendario.selectedDateMillis?.let { viewModel.aoMudarDataEntrada(millisUtcParaIso(it)) }
                        ?: viewModel.fecharDataEntrada()
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = viewModel::fecharDataEntrada) { Text("Cancelar") } }
        ) {
            DatePicker(state = calendario)
        }
    }

    estado.registrando?.let { r ->
        AlertDialog(
            onDismissRequest = viewModel::fecharRegistro,
            title = { Text("Registrar pagamento") },
            text = {
                Column {
                    Text("${r.deNome} pagou a ${r.paraNome}:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = r.valor,
                        onValueChange = viewModel::aoMudarValorDoPagamento,
                        label = { Text("Valor") },
                        prefix = { Text("R$ ") },
                        singleLine = true,
                        isError = r.erro != null,
                        supportingText = { Text(r.erro ?: "Se foi só uma parte, mude o valor") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarRegistro, enabled = !estado.salvando) { Text("Registrar") }
            },
            dismissButton = { TextButton(onClick = viewModel::fecharRegistro) { Text("Cancelar") } }
        )
    }

    estado.excluindoPagamento?.let { p ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarExclusaoDePagamento,
            title = { Text("Apagar pagamento?") },
            text = { Text("O pagamento de ${formatarReais(p.valor)} deixa de contar e os saldos voltam ao que eram.") },
            confirmButton = { TextButton(onClick = viewModel::confirmarExclusaoDePagamento) { Text("Apagar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarExclusaoDePagamento) { Text("Cancelar") } }
        )
    }

    if (estado.confirmandoSaida) {
        val ultimo = (estado.grupo?.membros?.size ?: 0) <= 1
        AlertDialog(
            onDismissRequest = viewModel::cancelarSaida,
            title = { Text("Sair do grupo?") },
            text = {
                Text(
                    if (ultimo) "Você é o último membro: o grupo será apagado. Seus gastos continuam na sua conta."
                    else "Seus gastos deixam de fazer parte do grupo, mas continuam na sua conta. " +
                        "Só dá para sair com as contas acertadas."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarSaida) { Text("Sair") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarSaida) { Text("Cancelar") } }
        )
    }
}
