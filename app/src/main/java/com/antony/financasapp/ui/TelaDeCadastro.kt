package com.antony.financasapp.ui

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.antony.financasapp.core.SessionManager
import com.antony.financasapp.dto.CadastroRequest
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * Tela de criar conta.
 * Se o cadastro der certo, já faz o login sozinho e chama [onContaCriada],
 * para a pessoa não ter que digitar tudo de novo.
 */
@Composable
fun TelaDeCadastro(
    onContaCriada: () -> Unit,
    onVoltarParaLogin: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var mensagemErro by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    // Confere os dados antes de gastar uma chamada na API.
    // Retorna a mensagem de erro, ou null se estiver tudo certo.
    fun validar(): String? = when {
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Digite um e-mail válido."
        senha.length < 6 -> "A senha precisa ter pelo menos 6 caracteres."
        senha != confirmarSenha -> "As senhas não são iguais."
        else -> null
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Criar conta", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha (mínimo 6 caracteres)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmarSenha,
            onValueChange = { confirmarSenha = it },
            label = { Text("Confirmar senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = mensagemErro, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            enabled = !carregando,
            onClick = {
                val erro = validar()
                if (erro != null) {
                    mensagemErro = erro
                    return@Button
                }
                mensagemErro = ""
                carregando = true

                scope.launch {
                    try {
                        val api = RetrofitClient.getApiService(context)
                        val emailLimpo = email.trim()
                        val resposta = api.cadastrar(CadastroRequest(emailLimpo, senha))

                        when (resposta.code()) {
                            201 -> {
                                // Conta criada: entra direto com os mesmos dados
                                val login = api.fazerLogin(LoginRequest(emailLimpo, senha))
                                val token = login.body()?.token
                                if (login.isSuccessful && token != null) {
                                    sessionManager.salvarToken(token)
                                    onContaCriada()
                                } else {
                                    // Raro, mas a conta existe: manda para o login normal
                                    onVoltarParaLogin()
                                }
                            }
                            409 -> mensagemErro = "Este e-mail já tem uma conta. Faça login."
                            400 -> mensagemErro = "Confira o e-mail e a senha."
                            else -> mensagemErro = "Erro no servidor (${resposta.code()}). Tente de novo."
                        }
                    } catch (e: Exception) {
                        mensagemErro = "Erro de conexão."
                    } finally {
                        carregando = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (carregando) {
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
