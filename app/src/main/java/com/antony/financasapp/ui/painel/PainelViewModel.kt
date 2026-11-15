package com.antony.financasapp.ui.painel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.dto.TransacaoResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

data class PainelUiState(
    val mes: Int,
    val ano: Int,
    val carregando: Boolean = true,
    val erro: String? = null,
    val total: BigDecimal = BigDecimal.ZERO,
    val transacoes: List<TransacaoResponse> = emptyList()
)

class PainelViewModel(private val repository: TransacaoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        Calendar.getInstance().let {
            PainelUiState(mes = it.get(Calendar.MONTH) + 1, ano = it.get(Calendar.YEAR))
        }
    )
    val uiState: StateFlow<PainelUiState> = _uiState.asStateFlow()

    // Guarda a busca em andamento: se o usuário trocar de mês rápido, cancelamos a anterior
    // para a resposta de um mês velho não sobrescrever a do mês atual
    private var buscaAtual: Job? = null

    init {
        carregar()
    }

    fun mesAnterior() = trocarMes(-1)

    fun proximoMes() = trocarMes(+1)

    private fun trocarMes(delta: Int) {
        _uiState.update {
            // Converte para "meses desde o ano 0" para a virada de ano sair de graça
            val total = it.ano * 12 + (it.mes - 1) + delta
            it.copy(mes = total % 12 + 1, ano = total / 12)
        }
        carregar()
    }

    fun carregar() {
        buscaAtual?.cancel()
        _uiState.update { it.copy(carregando = true, erro = null) }
        val (mes, ano) = _uiState.value.let { it.mes to it.ano }

        buscaAtual = viewModelScope.launch {
            when (val resultado = repository.buscarMes(mes, ano)) {
                is Resultado.Sucesso -> _uiState.update {
                    it.copy(
                        carregando = false,
                        total = resultado.dados.total,
                        transacoes = resultado.dados.transacoes
                    )
                }
                is Resultado.Erro -> _uiState.update {
                    it.copy(carregando = false, erro = resultado.mensagem)
                }
            }
        }
    }
}
