package com.antony.financasapp.ui.transacoes

import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.dto.FormaPagamento
import com.antony.financasapp.dto.GrupoResponse
import com.antony.financasapp.ui.app
import com.antony.financasapp.ui.dataIsoParaExibir
import com.antony.financasapp.ui.isoParaMillisUtc
import com.antony.financasapp.ui.millisUtcParaIso

// Mesma tela para criar (id nulo) e editar (com id)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaFormTransacao(
    id: Long?,
    grupoId: Long?,
    onVoltar: () -> Unit,
    viewModel: FormTransacaoViewModel = viewModel {
        FormTransacaoViewModel(id, grupoId, app().transacaoRepository, app().categoriaRepository, app().grupoRepository)
    }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    // Salvou ou excluiu: Toast (e não Snackbar) porque ele continua visível depois que a tela fecha
    LaunchedEffect(estado.concluido) {
        estado.concluido?.let {
            Toast.makeText(contexto, it, Toast.LENGTH_LONG).show()
            onVoltar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (estado.editando) "Editar transação" else "Nova transação") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (estado.editando && !estado.carregando && estado.erroAoCarregar == null) {
                        IconButton(onClick = viewModel::pedirExclusao, enabled = !estado.salvando) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir transação")
                        }
                    }
                }
            )
        }
    ) { espacoDasBarras ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(espacoDasBarras)
        ) {
            when {
                estado.carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                estado.erroAoCarregar != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(estado.erroAoCarregar!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::carregar) { Text("Tentar de novo") }
                }

                else -> Formulario(estado, viewModel)
            }
        }
    }

    if (estado.confirmandoExclusao) {
        DialogoExcluirTransacao(
            descricao = estado.descricao,
            parcela = estado.parcela,
            aoEscolher = viewModel::confirmarExclusao,
            aoCancelar = viewModel::cancelarExclusao
        )
    }

    if (estado.perguntandoEscopo) {
        estado.parcela?.let {
            DialogoEscopoDaEdicao(
                parcela = it,
                aoEscolher = viewModel::escolherEscopo,
                aoCancelar = viewModel::cancelarEscopo
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Formulario(estado: FormTransacaoUiState, viewModel: FormTransacaoViewModel) {
    var calendarioAberto by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        estado.parcela?.let { parcela ->
            // Deixa claro que é uma parcela: ao salvar, o app pergunta se a mudança vale para as próximas
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Parcela ${parcela.numero} de ${parcela.total} de uma compra no crédito",
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        OutlinedTextField(
            value = estado.descricao,
            onValueChange = viewModel::aoMudarDescricao,
            label = { Text("Descrição") },
            isError = estado.erroDescricao != null,
            supportingText = estado.erroDescricao?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = estado.valor,
            onValueChange = viewModel::aoMudarValor,
            label = { Text("Valor") },
            prefix = { Text("R$ ") },
            placeholder = { Text("0,00") },
            isError = estado.erroValor != null,
            supportingText = estado.erroValor?.let { { Text(it) } },
            singleLine = true,
            // Decimal mostra o teclado numérico com vírgula/ponto
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Campo só de leitura: tocar nele abre o calendário. Um TextField "engole" o toque,
        // então escutamos as interações dele e abrimos o calendário quando o dedo solta
        val toquesNaData = remember { MutableInteractionSource() }
        LaunchedEffect(toquesNaData) {
            toquesNaData.interactions.collect { if (it is PressInteraction.Release) calendarioAberto = true }
        }
        OutlinedTextField(
            value = dataIsoParaExibir(estado.data),
            onValueChange = {},
            readOnly = true,
            label = { Text("Data") },
            trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            interactionSource = toquesNaData,
            modifier = Modifier.fillMaxWidth()
        )

        CampoDeLista(
            rotulo = "Categoria",
            selecionado = estado.categorias.firstOrNull { it.id == estado.categoriaId }?.nome ?: "",
            opcoes = estado.categorias,
            textoDaOpcao = { it.nome },
            aoEscolher = { viewModel.aoMudarCategoria(it.id) },
            erro = estado.erroCategoria
        )

        CampoDeLista(
            rotulo = "Forma de pagamento",
            selecionado = estado.formaPagamento.rotulo,
            opcoes = FormaPagamento.entries,
            textoDaOpcao = { it.rotulo },
            aoEscolher = viewModel::aoMudarFormaPagamento,
            // Uma parcela continua sendo crédito: para mudar isso, exclua a compra e crie de novo
            habilitado = estado.parcela == null,
            ajuda = if (estado.parcela != null) "Parcelas são sempre no crédito" else null
        )

        // Só aparece se a pessoa está em algum grupo. "Nenhum" (null) = gasto só meu
        if (estado.grupos.isNotEmpty()) {
            CampoDeLista(
                rotulo = "Grupo (opcional)",
                selecionado = estado.grupos.firstOrNull { it.id == estado.grupoId }?.nome ?: "Nenhum",
                opcoes = listOf<GrupoResponse?>(null) + estado.grupos,
                textoDaOpcao = { it?.nome ?: "Nenhum" },
                aoEscolher = { viewModel.aoMudarGrupo(it?.id) },
                ajuda = if (estado.grupoId != null) "Entra no total e no acerto de contas do grupo" else null
            )
        }

        // Parcelas só existem no crédito. Na edição não aparecem: a API divide em parcelas
        // só ao criar, então mudar o número aqui não criaria nem apagaria parcelas
        if (estado.formaPagamento == FormaPagamento.CREDITO && !estado.editando) {
            OutlinedTextField(
                value = estado.parcelas,
                onValueChange = viewModel::aoMudarParcelas,
                label = { Text("Parcelas") },
                isError = estado.erroParcelas != null,
                supportingText = {
                    Text(estado.erroParcelas ?: "O valor é dividido em parcelas mensais")
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        estado.erro?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = viewModel::salvar,
            enabled = !estado.salvando,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.salvando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Salvar")
        }
    }

    if (calendarioAberto) {
        val estadoDoCalendario = rememberDatePickerState(initialSelectedDateMillis = isoParaMillisUtc(estado.data))
        DatePickerDialog(
            onDismissRequest = { calendarioAberto = false },
            confirmButton = {
                TextButton(onClick = {
                    estadoDoCalendario.selectedDateMillis?.let { viewModel.aoMudarData(millisUtcParaIso(it)) }
                    calendarioAberto = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { calendarioAberto = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = estadoDoCalendario)
        }
    }
}

// Campo com lista de opções (o "spinner" do Material 3)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CampoDeLista(
    rotulo: String,
    selecionado: String,
    opcoes: List<T>,
    textoDaOpcao: (T) -> String,
    aoEscolher: (T) -> Unit,
    erro: String? = null,
    habilitado: Boolean = true,
    ajuda: String? = null
) {
    var aberto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = aberto, onExpandedChange = { if (habilitado) aberto = it }) {
        OutlinedTextField(
            value = selecionado,
            onValueChange = {},
            readOnly = true,
            enabled = habilitado,
            label = { Text(rotulo) },
            isError = erro != null,
            supportingText = (erro ?: ajuda)?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = aberto) },
            // menuAnchor liga o menu a este campo (posição e largura)
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = { Text(textoDaOpcao(opcao)) },
                    onClick = {
                        aoEscolher(opcao)
                        aberto = false
                    }
                )
            }
        }
    }
}
