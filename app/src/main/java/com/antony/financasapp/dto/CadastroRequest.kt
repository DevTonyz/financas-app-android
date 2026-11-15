package com.antony.financasapp.dto

// Corpo enviado para POST /usuarios (os nomes batem com o DadosCadastroUsuario da API)
data class CadastroRequest(
    val login: String,
    val senha: String
)

// Resposta da API quando o cadastro dá certo (201)
data class CadastroResponse(
    val id: Long,
    val login: String
)
