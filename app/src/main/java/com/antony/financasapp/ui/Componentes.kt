package com.antony.financasapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.antony.financasapp.R

// Peças de tela usadas em mais de um lugar

// Logo e nome do app, no topo do login e do cadastro
@Composable
fun LogoDoApp(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // contentDescription nulo: o nome logo abaixo já diz o que é (o leitor de tela não repete)
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Campo de senha com o botão "Mostrar"/"Ocultar".
 * É um botão de texto, e não um ícone de olho, porque o olho só existe na biblioteca
 * material-icons-extended, que é pesada (deixa o app bem maior) só por causa de um ícone.
 */
@Composable
fun CampoDeSenha(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    ultimoCampo: Boolean = false,
    aoConcluir: () -> Unit = {},
    erro: String? = null
) {
    // rememberSaveable: continua visível/oculta ao girar a tela
    var visivel by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        singleLine = true,
        isError = erro != null,
        supportingText = erro?.let { { Text(it) } },
        visualTransformation = if (visivel) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(onClick = { visivel = !visivel }) {
                Text(if (visivel) "Ocultar" else "Mostrar")
            }
        },
        // "Próximo" pula para o campo seguinte; no último campo, "Concluído" já envia o formulário
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = if (ultimoCampo) ImeAction.Done else ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onDone = { aoConcluir() }),
        modifier = modifier.fillMaxWidth()
    )
}

// Lista vazia: ícone grande, uma frase e (opcional) uma dica do que fazer
@Composable
fun EstadoVazio(
    icone: ImageVector,
    texto: String,
    modifier: Modifier = Modifier,
    dica: String? = null
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Text(texto, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        dica?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
