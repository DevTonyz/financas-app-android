package com.antony.financasapp.ui.categorias

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.ui.app
import kotlinx.coroutines.delay

private const val TAMANHO_MAXIMO_DO_NOME = 40

// Lista as categorias do usuário e permite criar, renomear e excluir
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaCategorias(
    onVoltar: () -> Unit,
    viewModel: CategoriasViewModel = viewModel { CategoriasViewModel(app().categoriaRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }

    // Quando aparece um aviso no estado, mostra o Snackbar e avisa o ViewModel que já mostrou
    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            avisos.showSnackbar(it)
            viewModel.avisoMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirNova) {
                Icon(Icons.Filled.Add, contentDescription = "Nova categoria")
            }
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { espacoDasBarras ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(espacoDasBarras)
        ) {
            when {
                estado.carregando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                estado.erro != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(estado.erro!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::carregar) { Text("Tentar de novo") }
                }

                estado.categorias.isEmpty() -> Text(
                    "Nenhuma categoria. Toque em + para criar.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    // Espaço extra embaixo para o último item não ficar escondido atrás do botão +
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(estado.categorias, key = { it.id }) { categoria ->
                        ItemDeCategoria(
                            categoria = categoria,
                            aoTocar = { viewModel.abrirRenomear(categoria) },
                            aoExcluir = { viewModel.abrirExcluir(categoria) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    when (val dialogo = estado.dialogo) {
        is DialogoCategoria.Nova -> DialogoDeNome(
            titulo = "Nova categoria",
            nome = dialogo.nome,
            salvando = estado.salvando,
            aoMudarNome = viewModel::aoMudarNome,
            aoConfirmar = viewModel::confirmar,
            aoCancelar = viewModel::fecharDialogo
        )
        is DialogoCategoria.Renomear -> DialogoDeNome(
            titulo = "Renomear categoria",
            nome = dialogo.nome,
            salvando = estado.salvando,
            aoMudarNome = viewModel::aoMudarNome,
            aoConfirmar = viewModel::confirmar,
            aoCancelar = viewModel::fecharDialogo
        )
        is DialogoCategoria.Excluir -> AlertDialog(
            onDismissRequest = viewModel::fecharDialogo,
            title = { Text("Excluir categoria?") },
            text = { Text("A categoria \"${dialogo.categoria.nome}\" será apagada.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmar, enabled = !estado.salvando) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::fecharDialogo) { Text("Cancelar") }
            }
        )
        null -> Unit
    }
}

@Composable
private fun ItemDeCategoria(
    categoria: CategoriaResponse,
    aoTocar: () -> Unit,
    aoExcluir: () -> Unit
) {
    // ListItem é a linha padrão do Material 3 (texto à esquerda, ícone à direita)
    ListItem(
        headlineContent = { Text(categoria.nome) },
        trailingContent = {
            IconButton(onClick = aoExcluir) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir ${categoria.nome}")
            }
        },
        modifier = Modifier.clickable(onClick = aoTocar)
    )
}

@Composable
private fun DialogoDeNome(
    titulo: String,
    nome: String,
    salvando: Boolean,
    aoMudarNome: (String) -> Unit,
    aoConfirmar: () -> Unit,
    aoCancelar: () -> Unit
) {
    val tamanho = nome.trim().length
    val nomeValido = tamanho in 1..TAMANHO_MAXIMO_DO_NOME

    // TextFieldValue guarda o texto E a posição do cursor. Começamos com o cursor no fim,
    // senão ao renomear o que a pessoa digita entra antes do nome antigo
    var campo by remember { mutableStateOf(TextFieldValue(nome, TextRange(nome.length))) }
    // Foca o campo assim que o diálogo abre, para o teclado já subir.
    // O diálogo é uma janela à parte: a pausa curta dá tempo de ela aparecer antes de pedir o foco
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        foco.requestFocus()
    }

    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                value = campo,
                onValueChange = {
                    campo = it
                    aoMudarNome(it.text)
                },
                label = { Text("Nome") },
                modifier = Modifier.focusRequester(foco),
                singleLine = true,
                isError = tamanho > TAMANHO_MAXIMO_DO_NOME,
                supportingText = { Text("$tamanho/$TAMANHO_MAXIMO_DO_NOME") }
            )
        },
        confirmButton = {
            TextButton(onClick = aoConfirmar, enabled = nomeValido && !salvando) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) { Text("Cancelar") }
        }
    )
}
