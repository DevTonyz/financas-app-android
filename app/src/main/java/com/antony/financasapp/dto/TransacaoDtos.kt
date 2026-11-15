package com.antony.financasapp.dto

import java.math.BigDecimal

// Resposta de GET /transacoes/resumo?mes=&ano=
data class ResumoResponse(
    val mes: Int,
    val ano: Int,
    // BigDecimal em vez de Double: dinheiro não pode ter erro de arredondamento (0.1 + 0.2 != 0.3 em Double)
    val totalGasto: BigDecimal?
)

// Uma transação como a API devolve. Atenção: a data SAI em "dd/MM/yyyy"
data class TransacaoResponse(
    val id: Long,
    val descricao: String,
    val valor: BigDecimal,
    val data: String,
    val nomeCategoria: String?,
    // Campos usados para preencher o formulário de edição
    val categoriaId: Long?,
    val formaPagamento: FormaPagamento?,
    val parcelas: Int?,
    val grupoId: Long?
)

// Corpo de POST /transacoes e PUT /transacoes/{id}. Aqui a data ENTRA em "yyyy-MM-dd"
data class TransacaoRequest(
    val descricao: String,
    val valor: BigDecimal,
    val data: String,
    val categoriaId: Long,
    val formaPagamento: FormaPagamento,
    val parcelas: Int,
    val grupoId: Long? = null
)

// O Gson converte o enum pelo nome ("CREDITO"), que é o mesmo que a API usa
enum class FormaPagamento(val rotulo: String) {
    CREDITO("Crédito"),
    DEBITO("Débito"),
    PIX("Pix"),
    DINHEIRO("Dinheiro")
}

// Página do Spring (Page<T>). Genérica: serve para transações e, depois, para outras listas
data class PaginaResponse<T>(
    val content: List<T>,
    val totalPages: Int,
    val number: Int,
    val last: Boolean
)
