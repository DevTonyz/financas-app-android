package com.antony.financasapp.dto

import java.math.BigDecimal

// Um grupo na lista (GET /grupos) e na resposta de criar
data class GrupoResponse(
    val id: Long,
    val nome: String,
    val descricao: String?
)

// Corpo de POST /grupos
data class GrupoRequest(
    val nome: String,
    val descricao: String?
)

// GET /grupos/{id}: o grupo com os membros
data class GrupoDetalheResponse(
    val id: Long,
    val nome: String,
    val descricao: String?,
    val membros: List<MembroResponse>
)

// "voce" = é o usuário logado (o app não guarda o próprio id; a API diz quem é)
// dataEntrada vem em "yyyy-MM-dd" e é nula para quem já era membro antes dessa regra (divide tudo).
// Cada gasto só é dividido entre quem já estava no grupo na data dele
data class MembroResponse(
    val id: Long,
    val login: String,
    val voce: Boolean,
    val dataEntrada: String? = null
)

// Corpo de POST /grupos/{id}/membros. dataEntrada em "yyyy-MM-dd" (sem ela, a API usa hoje)
data class NovoMembroRequest(val login: String, val dataEntrada: String? = null)

// GET /transacoes/grupo/{id}/acerto?mes=&ano=
data class AcertoResponse(
    val mes: Int,
    val ano: Int,
    val total: BigDecimal,
    // Parte de quem está logado (quem entrou no meio do mês só divide os gastos a partir da entrada)
    val parteJusta: BigDecimal,
    val membros: List<MembroAcertoResponse>
)

// Saldo positivo = tem a receber; negativo = deve. A soma dos saldos é 0
data class MembroAcertoResponse(
    val usuarioId: Long,
    val login: String,
    val voce: Boolean,
    val totalPago: BigDecimal,
    val saldo: BigDecimal
)

// GET /grupos/{id}/saldos: saldo acumulado (todos os meses até hoje, já descontados os pagamentos)
data class SaldosResponse(
    val membros: List<SaldoDoMembro>,
    // Quem paga quanto a quem para todos ficarem quites
    val sugestoes: List<SugestaoDePagamento>
)

// Saldo > 0 = tem a receber; < 0 = deve
data class SaldoDoMembro(
    val usuarioId: Long,
    val login: String,
    val voce: Boolean,
    val saldo: BigDecimal
)

data class SugestaoDePagamento(
    val deUsuarioId: Long,
    val deLogin: String,
    val paraUsuarioId: Long,
    val paraLogin: String,
    val valor: BigDecimal
)

// Um acerto entre dois membros. Não é gasto: não entra no total, só mexe nos saldos. A data SAI em "dd/MM/yyyy"
data class PagamentoResponse(
    val id: Long,
    val deUsuarioId: Long,
    val deLogin: String,
    val paraUsuarioId: Long,
    val paraLogin: String,
    val valor: BigDecimal,
    val data: String
)

// Corpo de POST /grupos/{id}/pagamentos. A data ENTRA em "yyyy-MM-dd"
data class PagamentoRequest(
    val deUsuarioId: Long,
    val paraUsuarioId: Long,
    val valor: BigDecimal,
    val data: String
)
