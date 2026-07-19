package com.antony.financasapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.antony.financasapp.core.SessionManager
import com.antony.financasapp.dto.LoginRequest
import com.antony.financasapp.network.RetrofitClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val sessionManager = remember { SessionManager(context) }

                    // 1. Cria o Controlador do Carrossel de telas
                    val navController = rememberNavController()

                    // 2. Lê o cofre: se tem token, vai pra home. Se não, vai pro login.
                    val rotaDeLargada = if (sessionManager.buscarToken() != null) "home" else "login"

                    // 3. Configura o Roteador e cadastra as telas
                    NavHost(
                        navController = navController,
                        startDestination = rotaDeLargada
                    ) {
                        composable("login") {
                            // Passamos o controlador para a tela de login poder mudar de tela depois
                            TelaDeLogin(navController = navController)
                        }

                        composable("home") {
                            TelaPrincipal(sessionManager = sessionManager)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelaDeLogin(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagemErro by remember { mutableStateOf("") }
    var loginComSucesso by remember { mutableStateOf(false) }

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
                        val request = LoginRequest(email, senha)
                        val response = RetrofitClient.apiService.fazerLogin(request)

                        if (response.isSuccessful) {
                            val tokenDeAcesso = response.body()?.token
                            if (tokenDeAcesso != null) {
                                // Guarda no cofre
                                sessionManager.salvarToken(tokenDeAcesso)

                                loginComSucesso = true
                                mensagemErro = "Login feito com sucesso!"

                                // AÇÃO DE NAVEGAÇÃO: Pula para a home e destrói o histórico do login
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
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

@Composable
fun TelaPrincipal(sessionManager: SessionManager) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bem-vindo às suas Finanças!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Apaga o token para você conseguir testar o login novamente
            sessionManager.limparToken()
        }) {
            Text("Sair da Conta")
        }
    }
}