package com.antony.financasapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antony.financasapp.core.SessionEvents
import com.antony.financasapp.ui.appAtual
import com.antony.financasapp.ui.cadastro.TelaDeCadastro
import com.antony.financasapp.ui.home.TelaPrincipal
import com.antony.financasapp.ui.login.TelaDeLogin

// Nomes das rotas num lugar só, para não errar digitando "home" em vários arquivos
object Rotas {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val HOME = "home"
}

// Vai para [rota] e apaga todo o histórico, para o botão "voltar" não reabrir telas antigas
private fun NavHostController.irLimpandoHistorico(rota: String) {
    navigate(rota) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNavHost() {
    val app = appAtual()
    val navController = rememberNavController()

    // Decide a primeira tela uma única vez: com token vai para a home, sem token para o login
    val rotaInicial = remember { if (app.authRepository.estaLogado()) Rotas.HOME else Rotas.LOGIN }

    // Se a API disser que o token venceu (401), o AuthInterceptor avisa e caímos aqui
    LaunchedEffect(Unit) {
        SessionEvents.sessaoExpirada.collect { navController.irLimpandoHistorico(Rotas.LOGIN) }
    }

    NavHost(navController = navController, startDestination = rotaInicial) {
        composable(Rotas.LOGIN) {
            TelaDeLogin(
                onEntrou = { navController.irLimpandoHistorico(Rotas.HOME) },
                onCriarConta = { navController.navigate(Rotas.CADASTRO) }
            )
        }

        composable(Rotas.CADASTRO) {
            TelaDeCadastro(
                onContaCriada = { navController.irLimpandoHistorico(Rotas.HOME) },
                onVoltarParaLogin = { navController.popBackStack() }
            )
        }

        composable(Rotas.HOME) {
            TelaPrincipal(
                onSair = {
                    app.authRepository.sair()
                    navController.irLimpandoHistorico(Rotas.LOGIN)
                }
            )
        }
    }
}
