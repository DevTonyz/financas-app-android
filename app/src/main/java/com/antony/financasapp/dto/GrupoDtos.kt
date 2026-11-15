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
data class MembroResponse(
    val id: Long,
    val login: String,
    val voce: Boolean
)

// Corpo de POST /grupos/{id}/membros
data class NovoMembroRequest(val login: String)

// GET /transacoes/grupo/{id}/acerto?mes=&ano=
data class AcertoResponse(
    val mes: Int,
    val ano: Int,
    val total: BigDecimal,
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
