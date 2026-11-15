package com.antony.financasapp.ui.grupos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.DadosDoGrupoNoMes
import com.antony.financasapp.data.GrupoRepository
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.dto.GrupoDetalheResponse
import com.antony.financasapp.ui.deslocarMes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class GrupoUiState(
    val mes: Int,
    val ano: Int,
    // Carregando/erro do grupo (membros). Sem ele, a tela não tem o que mostrar
    val carregando: Boolean = true,
    val erro: String? = null,
    val grupo: GrupoDetalheResponse? = null,
    // Carregando/erro do mês (total, gastos, acerto), que muda ao trocar de mês
    val carregandoMes: Boolean = true,
    val erroMes: String? = null,
    val dadosDoMes: DadosDoGrupoNoMes? = null,
    // Diálogo "Adicionar por e-mail"
    val adicionando: Boolean = false,
    val email: String = "",
    val erroEmail: String? = null,
    val salvando: Boolean = false,
    val confirmandoSaida: Boolean = false,
    // Mensagem rápida (Snackbar), ex.: "fulano foi adicionado"
    val aviso: String? = null,
    // Saiu do grupo: a tela volta para a lista
    val saiu: Boolean = false
) {
    // A API marca quem sou eu na lista de membros: serve para saber quais gastos posso editar
    val meuId: Long? get() = grupo?.membros?.firstOrNull { it.voce }?.id
}

class GrupoViewModel(
    private val id: Long,
    private val repository: GrupoRepository,
    transacaoRepository: TransacaoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        Calendar.getInstance().let { GrupoUiState(mes = it.get(Calendar.MONTH) + 1, ano = it.get(Calendar.YEAR)) }
    )
    val uiState: StateFlow<GrupoUiState> = _uiState.asStateFlow()

    private var buscaDoMes: Job? = null

    init {
        carregar()
        // Lançou, editou ou excluiu um gasto (no formulário)? O total e o acerto mudam: recarrega o mês
        viewModelScope.launch { transacaoRepository.mudancas.drop(1).collect { carregarMes() } }
    }

    fun carregar() {
        _uiState.update { it.copy(carregando = true, erro = null) }
        viewModelScope.launch {
            when (val r = repository.detalhar(id)) {
                is Resultado.Sucesso -> {
                    _uiState.update { it.copy(carregando = false, grupo = r.dados) }
                    carregarMes()
                }
                is Resultado.Erro -> _uiState.update { it.copy(carregando = false, erro = r.mensagem) }
            }
        }
    }

    fun mesAnterior() = trocarMes(-1)

    fun proximoMes() = trocarMes(+1)

    private fun trocarMes(delta: Int) {
        _uiState.update {
            val (mes, ano) = deslocarMes(it.mes, it.ano, delta)
            it.copy(mes = mes, ano = ano)
        }
        carregarMes()
    }

    fun carregarMes() {
        // Cancela a busca anterior para a resposta de um mês velho não sobrescrever a do atual
        buscaDoMes?.cancel()
        _uiState.update { it.copy(carregandoMes = true, erroMes = null) }
        val (mes, ano) = _uiState.value.let { it.mes to it.ano }
        buscaDoMes = viewModelScope.launch {
            when (val r = repository.buscarMes(id, mes, ano)) {
                is Resultado.Sucesso -> _uiState.update { it.copy(carregandoMes = false, dadosDoMes = r.dados) }
                is Resultado.Erro -> _uiState.update { it.copy(carregandoMes = false, erroMes = r.mensagem) }
            }
        }
    }

    // --- Adicionar membro ---
    fun abrirAdicionar() = _uiState.update { it.copy(adicionando = true, email = "", erroEmail = null) }

    fun fecharAdicionar() = _uiState.update { it.copy(adicionando = false) }

    fun aoMudarEmail(texto: String) = _uiState.update { it.copy(email = texto.trim(), erroEmail = null) }

    fun adicionar() {
        val email = _uiState.value.email
        if (_uiState.value.salvando) return
        // Mesma checagem simples do cadastro: o resto (se a conta existe) quem diz é a API
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(erroEmail = "Digite um e-mail válido") }
            return
        }
        _uiState.update { it.copy(salvando = true, erroEmail = null) }
        viewModelScope.launch {
            when (val r = repository.adicionarMembro(id, email)) {
                is Resultado.Sucesso -> {
                    _uiState.update {
                        it.copy(salvando = false, adicionando = false, grupo = r.dados, aviso = "$email agora faz parte do grupo")
                    }
                    // Com mais um membro, a parte justa de cada um muda
                    carregarMes()
                }
                // 404 (sem conta) e 409 (já é membro) aparecem embaixo do campo
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, erroEmail = r.mensagem) }
            }
        }
    }

    // --- Sair do grupo ---
    fun pedirSaida() = _uiState.update { it.copy(confirmandoSaida = true) }

    fun cancelarSaida() = _uiState.update { it.copy(confirmandoSaida = false) }

    fun confirmarSaida() {
        _uiState.update { it.copy(confirmandoSaida = false, salvando = true) }
        viewModelScope.launch {
            when (val r = repository.sair(id)) {
                is Resultado.Sucesso -> _uiState.update { it.copy(salvando = false, saiu = true) }
                is Resultado.Erro -> _uiState.update { it.copy(salvando = false, aviso = r.mensagem) }
            }
        }
    }

    fun avisoMostrado() = _uiState.update { it.copy(aviso = null) }
}
