package com.antony.financasapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antony.financasapp.core.SessionEvents
import com.antony.financasapp.ui.appAtual
import com.antony.financasapp.ui.cadastro.TelaDeCadastro
import com.antony.financasapp.ui.categorias.TelaCategorias
import com.antony.financasapp.ui.grupos.TelaGrupo
import com.antony.financasapp.ui.grupos.TelaGrupos
import com.antony.financasapp.ui.login.TelaDeLogin
import com.antony.financasapp.ui.painel.TelaPainel
import com.antony.financasapp.ui.transacoes.TelaFormTransacao
import com.antony.financasapp.ui.transacoes.TelaTransacoes

// Nomes das rotas num lugar só, para não errar digitando "home" em vários arquivos
object Rotas {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val HOME = "home"
    const val CATEGORIAS = "categorias"
    const val TRANSACOES = "transacoes"
    const val GRUPOS = "grupos"

    // {id} sem "?" é obrigatório: a tela do grupo sempre precisa saber qual grupo é
    const val GRUPO = "grupo/{id}"
    fun grupo(id: Long) = "grupo/$id"

    // "?id=" e "&grupoId=" são opcionais: sem id a tela cria, com id edita.
    // grupoId preenche o campo "Grupo" ao criar um gasto a partir da tela do grupo
    const val FORM_TRANSACAO = "transacao?id={id}&grupoId={grupoId}"
    fun formTransacao(id: Long? = null, grupoId: Long? = null): String {
        val parametros = listOfNotNull(id?.let { "id=$it" }, grupoId?.let { "grupoId=$it" })
        return if (parametros.isEmpty()) "transacao" else "transacao?" + parametros.joinToString("&")
    }
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
            TelaPainel(
                onAbrirCategorias = { navController.navigate(Rotas.CATEGORIAS) },
                onAbrirGrupos = { navController.navigate(Rotas.GRUPOS) },
                onVerTodas = { navController.navigate(Rotas.TRANSACOES) },
                onNovaTransacao = { navController.navigate(Rotas.formTransacao()) },
                onEditarTransacao = { navController.navigate(Rotas.formTransacao(it)) },
                onSair = {
                    app.authRepository.sair()
                    navController.irLimpandoHistorico(Rotas.LOGIN)
                }
            )
        }

        composable(Rotas.CATEGORIAS) {
            TelaCategorias(onVoltar = { navController.popBackStack() })
        }

        composable(Rotas.TRANSACOES) {
            TelaTransacoes(
                onVoltar = { navController.popBackStack() },
                onNova = { navController.navigate(Rotas.formTransacao()) },
                onEditar = { navController.navigate(Rotas.formTransacao(it)) }
            )
        }

        composable(Rotas.GRUPOS) {
            TelaGrupos(
                onVoltar = { navController.popBackStack() },
                onAbrirGrupo = { navController.navigate(Rotas.grupo(it)) }
            )
        }

        composable(
            Rotas.GRUPO,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entrada ->
            TelaGrupo(
                id = entrada.arguments!!.getLong("id"),
                onVoltar = { navController.popBackStack() },
                onNovoGasto = { navController.navigate(Rotas.formTransacao(grupoId = it)) },
                onEditarTransacao = { navController.navigate(Rotas.formTransacao(it)) }
            )
        }

        composable(
            Rotas.FORM_TRANSACAO,
            // Long não aceita null como argumento de rota, então -1 significa "sem valor"
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                navArgument("grupoId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { entrada ->
            val id = entrada.arguments?.getLong("id")?.takeIf { it > 0 }
            val grupoId = entrada.arguments?.getLong("grupoId")?.takeIf { it > 0 }
            TelaFormTransacao(id = id, grupoId = grupoId, onVoltar = { navController.popBackStack() })
        }
    }
}
