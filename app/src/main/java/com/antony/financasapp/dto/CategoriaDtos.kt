package com.antony.financasapp.dto

// Uma categoria como a API devolve (cada usuário só recebe as dele)
data class CategoriaResponse(
    val id: Long,
    val nome: String
)

// Corpo de POST /categorias e PUT /categorias/{id}
data class CategoriaRequest(
    val nome: String
)
