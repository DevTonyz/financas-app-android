package com.antony.financasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

// 1. Mudamos de AppCompatActivity para ComponentActivity (a base do Compose)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Trocamos o setContentView (XML) pelo setContent (Kotlin Puro)
        setContent {
            // MaterialTheme aplica as cores e tipografia padrão do Android
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelaDeLogin()
                }
            }
        }
    }
}

// 3. A anotação @Composable diz ao Android que esta função desenha algo na tela
@Composable
fun TelaDeLogin() {
    // Pegamos o 'Contexto' (a tela atual do Android) para o SessionManager conseguir criar o arquivo
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { com.antony.financasapp.core.SessionManager(context) }

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagemErro by remember { mutableStateOf("") }
    var loginComSucesso by remember { mutableStateOf(false) } // Nova variável para mudar a cor da mensagem

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bem-vindo", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())

        if (mensagemErro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            // Se for sucesso fica verde, se for erro fica vermelho
            Text(
                text = mensagemErro,
                color = if (loginComSucesso) androidx.compose.ui.graphics.Color.Green else MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    try {
                        val request = com.antony.financasapp.dto.LoginRequest(email, senha)
                        val response = com.antony.financasapp.network.RetrofitClient.apiService.fazerLogin(request)

                        if (response.isSuccessful) {
                            val tokenDeAcesso = response.body()?.token
                            if (tokenDeAcesso != null) {
                                // A MÁGICA ACONTECE AQUI: Guarda o token no cofre do celular!
                                sessionManager.salvarToken(tokenDeAcesso)

                                loginComSucesso = true
                                mensagemErro = "Login feito com sucesso! Token Salvo."
                            }
                        } else {
                            loginComSucesso = false
                            mensagemErro = "E-mail ou senha incorretos."
                        }
                    } catch (e: Exception) {
                        loginComSucesso = false
                        mensagemErro = "Erro de conexão."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Entrar")
        }
    }
}