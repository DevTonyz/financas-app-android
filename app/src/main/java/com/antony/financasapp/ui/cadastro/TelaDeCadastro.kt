package com.antony.financasapp.ui.cadastro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = estado.email,
            onValueChange = viewModel::aoMudarEmail,
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.senha,
            onValueChange = viewModel::aoMudarSenha,
            label = { Text("Senha (mínimo 6 caracteres)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = estado.confirmarSenha,
            onValueChange = viewModel::aoMudarConfirmarSenha,
            label = { Text("Confirmar senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        estado.erro?.let { erro ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = erro, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

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
