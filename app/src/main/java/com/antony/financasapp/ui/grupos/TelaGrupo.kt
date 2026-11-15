package com.antony.financasapp.ui.grupos

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import com.antony.financasapp.dto.AcertoResponse
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.dto.MembroAcertoResponse
import com.antony.financasapp.ui.SeletorDeMes
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.formatarReais
import com.antony.financasapp.ui.mesPorExtenso
import com.antony.financasapp.ui.transacoes.ItemDeTransacao

// Verde para quem tem a receber. O vermelho (deve) vem do tema: colorScheme.error
private val VerdeReceber = Color(0xFF2E7D32)

// Detalhe do grupo: membros, mês, total, acerto de contas e gastos do grupo
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

    if (estado.confirmandoSaida) {
        val ultimo = (estado.grupo?.membros?.size ?: 0) <= 1
        AlertDialog(
            onDismissRequest = viewModel::cancelarSaida,
            title = { Text("Sair do grupo?") },
            text = {
                Text(
                    if (ultimo) "Você é o último membro: o grupo será apagado. Seus gastos continuam na sua conta."
                    else "Seus gastos deixam de fazer parte do grupo, mas continuam na sua conta."
                )
            },
            confirmButton = { TextButton(onClick = viewModel::confirmarSaida) { Text("Sair") } },
            dismissButton = { TextButton(onClick = viewModel::cancelarSaida) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun ConteudoDoGrupo(
    estado: GrupoUiState,
    grupo: GrupoDetalheResponse,
    viewModel: GrupoViewModel,
    onEditarTransacao: (Long) -> Unit
) {
    // Uma LazyColumn só para a tela inteira rolar junto (cartões + lista de gastos)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grupo.descricao?.let { descricao ->
            item { Text(descricao, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        item { CardDeMembros(grupo, aoAdicionar = viewModel::abrirAdicionar) }

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
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Total do grupo no mês", style = MaterialTheme.typography.labelLarge)
                            Text(
                                formatarReais(dados.total),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item { CardDoAcerto(dados.acerto) }

                item {
                    Text(
                        "Gastos do grupo",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (dados.transacoes.isEmpty()) {
                    item { Text("Nenhum gasto do grupo neste mês. Toque em + para lançar.") }
                } else {
                    items(dados.transacoes, key = { it.id }) { t ->
                        val minha = t.usuarioId != null && t.usuarioId == estado.meuId
                        ItemDeTransacao(
                            transacao = t,
                            // Só quem lançou pode editar: nos gastos dos outros o toque fica desligado
                            aoTocar = if (minha) ({ onEditarTransacao(t.id) }) else null,
                            autor = if (minha) "você" else t.loginUsuario
                        )
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
                Text(if (membro.voce) "${membro.login} (você)" else membro.login)
            }
        }
    }
}

@Composable
private fun CardDoAcerto(acerto: AcertoResponse) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Acerto de contas", style = MaterialTheme.typography.titleMedium)
            Text(
                "Parte de cada um: ${formatarReais(acerto.parteJusta)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            acerto.membros.forEach { LinhaDoAcerto(it) }
        }
    }
}

@Composable
private fun LinhaDoAcerto(membro: MembroAcertoResponse) {
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
        Column(Modifier.weight(1f)) {
            Text(
                if (membro.voce) "Você" else membro.login,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "pagou ${formatarReais(membro.totalPago)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(situacao, color = cor, fontWeight = FontWeight.SemiBold)
    }
}
