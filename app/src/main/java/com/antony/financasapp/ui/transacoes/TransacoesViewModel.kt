package com.antony.financasapp.ui.transacoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antony.financasapp.data.CategoriaRepository
import com.antony.financasapp.data.FiltroTransacoes
import com.antony.financasapp.data.Resultado
import com.antony.financasapp.data.TransacaoRepository
import com.antony.financasapp.dto.CategoriaResponse
import com.antony.financasapp.dto.EscopoParcelas
import com.antony.financasapp.dto.TransacaoResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransacoesUiState(
    val filtro: FiltroTransacoes = FiltroTransacoes.Todas,
    // Carregando a primeira página: spinner na tela toda
    val carregando: Boolean = true,
    val erro: String? = null,
    val transacoes: List<TransacaoResponse> = emptyList(),
    // Paginação: última página que chegou e se ela era a última
    val pagina: Int = 0,
    val ultimaPagina: Boolean = true,
    // Buscando a próxima página: spinner pequeno no fim da lista
    val carregandoMais: Boolean = false,
    // Categorias para o filtro
    val categorias: List<CategoriaResponse> = emptyList(),
    // Transação esperando o "sim" do diálogo de exclusão (depois de arrastar o item)
    val paraExcluir: TransacaoResponse? = null,
    val aviso: String? = null
)

class TransacoesViewModel(
    private val repository: TransacaoRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransacoesUiState())
    val uiState: StateFlow<TransacoesUiState> = _uiState.asStateFlow()

    // Busca em andamento: ao trocar de filtro cancelamos a anterior, para a resposta
    // do filtro velho não se misturar com a do novo
    private var buscaAtual: Job? = null

    init {
        carregar()
        viewModelScope.launch {
            (categoriaRepository.listar() as? Resultado.Sucesso)?.let { r ->
                _uiState.update { it.copy(categorias = r.dados) }
            }
        }
        // Criou, editou ou excluiu em outra tela? Recarrega sem piscar o spinner.
        // drop(1) ignora o valor atual, que chega assim que começamos a escutar
        viewModelScope.launch {
            repository.mudancas.drop(1).collect { carregar(mostrarSpinner = false) }
        }
    }

    fun filtrar(filtro: FiltroTransacoes) {
        if (filtro == _uiState.value.filtro) return
        _uiState.update { it.copy(filtro = filtro) }
        carregar()
    }

    // Busca a primeira página de novo (filtro novo, "Tentar de novo" ou algo mudou)
    fun carregar(mostrarSpinner: Boolean = true) {
        buscaAtual?.cancel()
        if (mostrarSpinner) _uiState.update { it.copy(carregando = true, erro = null) }
        val filtro = _uiState.value.filtro

        buscaAtual = viewModelScope.launch {
            when (val resultado = repository.listar(filtro, pagina = 0)) {
                is Resultado.Sucesso -> _uiState.update {
                    it.copy(
                        carregando = false,
                        erro = null,
                        carregandoMais = false,
                        transacoes = resultado.dados.content,
                        pagina = resultado.dados.number,
                        ultimaPagina = resultado.dados.last
                    )
                }
                is Resultado.Erro -> _uiState.update {
                    it.copy(carregando = false, carregandoMais = false, erro = resultado.mensagem)
                }
            }
        }
    }

    // Chamado quando a lista chega perto do fim
    fun carregarMais() {
        val estado = _uiState.value
        if (estado.carregando || estado.carregandoMais || estado.ultimaPagina || estado.erro != null) return

        _uiState.update { it.copy(carregandoMais = true) }
        buscaAtual = viewModelScope.launch {
            when (val resultado = repository.listar(estado.filtro, estado.pagina + 1)) {
                is Resultado.Sucesso -> _uiState.update {
                    it.copy(
                        carregandoMais = false,
                        // distinctBy evita item repetido se algo foi criado entre uma página e outra
                        transacoes = (it.transacoes + resultado.dados.content).distinctBy { t -> t.id },
                        pagina = resultado.dados.number,
                        ultimaPagina = resultado.dados.last
                    )
                }
                is Resultado.Erro -> _uiState.update { it.copy(carregandoMais = false, aviso = resultado.mensagem) }
            }
        }
    }

    fun pedirExclusao(transacao: TransacaoResponse) = _uiState.update { it.copy(paraExcluir = transacao) }

    fun cancelarExclusao() = _uiState.update { it.copy(paraExcluir = null) }

    fun confirmarExclusao(escopo: EscopoParcelas) {
        val transacao = _uiState.value.paraExcluir ?: return
        _uiState.update { it.copy(paraExcluir = null) }
        viewModelScope.launch {
            when (val resultado = repository.excluir(transacao.id, escopo)) {
                // A lista recarrega sozinha pelo "mudancas"; aqui só avisamos
                is Resultado.Sucesso -> _uiState.update { it.copy(aviso = mensagemDeExclusao(escopo)) }
                is Resultado.Erro -> _uiState.update { it.copy(aviso = resultado.mensagem) }
            }
        }
    }

    fun avisoMostrado() = _uiState.update { it.copy(aviso = null) }
}

// Usada aqui e no formulário
fun mensagemDeExclusao(escopo: EscopoParcelas) =
    if (escopo == EscopoParcelas.ESTA) "Transação excluída" else "Parcelas excluídas"
