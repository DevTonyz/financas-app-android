package com.antony.financasapp.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antony.financasapp.ui.CampoDeSenha
import com.antony.financasapp.ui.LogoDoApp
import com.antony.financasapp.ui.app

@Composable
fun TelaDeLogin(
    onEntrou: () -> Unit,
    onCriarConta: () -> Unit,
    viewModel: LoginViewModel = viewModel { LoginViewModel(app().authRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(estado.entrou) {
        if (estado.entrou) onEntrou()
    }

    // Box centraliza; o verticalScroll + imePadding deixam rolar até o botão com a tela deitada ou o teclado aberto
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LogoDoApp()
            Spacer(Modifier.height(24.dp))
            Text("Bem-vindo", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = estado.email,
                onValueChange = viewModel::aoMudarEmail,
                label = { Text("E-mail") },
                singleLine = true,
                // ImeAction.Next: o botão do teclado vira "próximo" e leva para a senha
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            CampoDeSenha(
                valor = estado.senha,
                aoMudar = viewModel::aoMudarSenha,
                rotulo = "Senha",
                ultimoCampo = true,
                aoConcluir = viewModel::entrar
            )

            estado.erro?.let { erro ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = erro, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = !estado.carregando,
                onClick = viewModel::entrar,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (estado.carregando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Entrar")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onCriarConta) {
                Text("Não tem conta? Criar conta")
            }
        }
    }
}
