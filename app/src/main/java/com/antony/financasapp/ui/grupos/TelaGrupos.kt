package com.antony.financasapp.ui.grupos

import com.antony.financasapp.ui.EstadoVazio
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.ui.app

// Lista dos grupos de que eu faço parte, com o "+" para criar um novo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaGrupos(
    onVoltar: () -> Unit,
    onAbrirGrupo: (Long) -> Unit,
    viewModel: GruposViewModel = viewModel { GruposViewModel(app().grupoRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grupos") },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirNovo) {
                Icon(Icons.Filled.Add, contentDescription = "Novo grupo")
            }
        }
    ) { espacoDasBarras ->
        Box(
            Modifier
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

                estado.grupos.isEmpty() -> EstadoVazio(
                    icone = Icons.Filled.Person,
                    texto = "Você ainda não está em nenhum grupo",
                    dica = "Toque em + para criar um e dividir as contas.",
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                    items(estado.grupos, key = { it.id }) { grupo ->
                        ListItem(
                            headlineContent = { Text(grupo.nome) },
                            supportingContent = grupo.descricao?.let { { Text(it) } },
                            modifier = Modifier.clickable { onAbrirGrupo(grupo.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (estado.criando) {
        AlertDialog(
            onDismissRequest = viewModel::fecharNovo,
            title = { Text("Novo grupo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estado.nome,
                        onValueChange = viewModel::aoMudarNome,
                        label = { Text("Nome") },
                        placeholder = { Text("Ex.: Casa") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = estado.descricao,
                        onValueChange = viewModel::aoMudarDescricao,
                        label = { Text("Descrição (opcional)") },
                        singleLine = true
                    )
                    estado.erroDialogo?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::criar,
                    enabled = estado.nome.isNotBlank() && !estado.salvando
                ) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = viewModel::fecharNovo) { Text("Cancelar") } }
        )
    }
}
