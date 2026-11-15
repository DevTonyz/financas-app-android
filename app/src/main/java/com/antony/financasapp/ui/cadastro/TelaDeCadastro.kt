package com.antony.financasapp.ui.cadastro

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

/**
 * Tela de criar conta. Só desenha o estado do [CadastroViewModel] e repassa as ações para ele.
 * Se a conta for criada, o ViewModel já fez o login e chamamos [onContaCriada].
 */
@Composable
fun TelaDeCadastro(
    onContaCriada: () -> Unit,
    onVoltarParaLogin: () -> Unit,
    viewModel: CadastroViewModel = viewModel { CadastroViewModel(app().authRepository) }
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    // Quando a conta é criada, navega uma única vez
    LaunchedEffect(estado.contaCriada) {
        if (estado.contaCriada) onContaCriada()
    }

    // Mesmo esquema do login: centralizado, mas rolável com a tela deitada ou o teclado aberto
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
            Text("Criar conta", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = estado.email,
                onValueChange = viewModel::aoMudarEmail,
                label = { Text("E-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            CampoDeSenha(
                valor = estado.senha,
                aoMudar = viewModel::aoMudarSenha,
                rotulo = "Senha (mínimo 6 caracteres)"
            )
            Spacer(modifier = Modifier.height(16.dp))

            CampoDeSenha(
                valor = estado.confirmarSenha,
                aoMudar = viewModel::aoMudarConfirmarSenha,
                rotulo = "Confirmar senha",
                ultimoCampo = true,
                aoConcluir = viewModel::criarConta
            )

            estado.erro?.let { erro ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = erro, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                enabled = !estado.carregando,
                onClick = viewModel::criarConta,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (estado.carregando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Criar conta")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onVoltarParaLogin) {
                Text("Já tenho conta. Entrar")
            }
        }
    }
}
