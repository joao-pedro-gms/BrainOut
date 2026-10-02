// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Fixtures compartilhadas dos testes de token (NB-04).

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File

/**
 * Resolve o nome de um papel semântico (`"text.primary"`, `"info.container"`)
 * para a cor efetiva de um tema.
 *
 * Existe para que os testes de contraste e de paridade leiam os papéis pelo
 * **nome usado na especificação**, e não pela propriedade Kotlin: assim um
 * renomeio no Kotlin quebra o teste, em vez de passar despercebido.
 */
internal fun neoRole(colors: NeoColors, role: String): Color = when (role) {
    "background" -> colors.background
    "surface" -> colors.surface
    "surface.alt" -> colors.surfaceAlt
    "text.primary" -> colors.textPrimary
    "text.muted" -> colors.textMuted
    "border.default" -> colors.borderDefault
    "border.onAccent" -> colors.borderOnAccent
    "focus" -> colors.focus
    "shadow" -> colors.shadow
    "action.background" -> colors.actionBackground
    "action.text" -> colors.actionText
    "brand.background" -> colors.brandBackground
    "brand.text" -> colors.brandText
    "link" -> colors.link
    "onLink" -> colors.onLink
    "secondary" -> colors.secondary
    "onSecondary" -> colors.onSecondary
    "secondary.container" -> colors.secondaryContainer
    "secondary.text" -> colors.secondaryText
    "success" -> colors.success
    "onSuccess" -> colors.onSuccess
    "success.container" -> colors.successContainer
    "success.text" -> colors.successText
    "info" -> colors.info
    "info.container" -> colors.infoContainer
    "info.text" -> colors.infoText
    "warning" -> colors.warning
    "warning.container" -> colors.warningContainer
    "warning.text" -> colors.warningText
    "error" -> colors.error
    "onError" -> colors.onError
    "error.container" -> colors.errorContainer
    "error.text" -> colors.errorText
    "disabled.background" -> colors.disabledBackground
    "disabled.text" -> colors.disabledText
    "inverse.background" -> colors.inverseBackground
    "inverse.text" -> colors.inverseText
    "inverse.primary" -> colors.inversePrimary
    "scrim" -> colors.scrim
    else -> error("Papel semântico desconhecido no teste: '$role'")
}

/** Resolve o nome de um primitivo (`"ink"`, `"redPaper"`) para a cor Kotlin. */
internal fun neoPrimitive(name: String): Color = when (name) {
    "ink" -> NeoPrimitive.ink
    "paper" -> NeoPrimitive.paper
    "white" -> NeoPrimitive.white
    "neutral" -> NeoPrimitive.neutral
    "muted" -> NeoPrimitive.muted
    "yellow" -> NeoPrimitive.yellow
    "violet" -> NeoPrimitive.violet
    "mint" -> NeoPrimitive.mint
    "blue" -> NeoPrimitive.blue
    "orange" -> NeoPrimitive.orange
    "night" -> NeoPrimitive.night
    "charcoal" -> NeoPrimitive.charcoal
    "graphite" -> NeoPrimitive.graphite
    "mutedDark" -> NeoPrimitive.mutedDark
    "shadowDark" -> NeoPrimitive.shadowDark
    "ochre" -> NeoPrimitive.ochre
    "purple" -> NeoPrimitive.purple
    "purpleLight" -> NeoPrimitive.purpleLight
    "purpleDeep" -> NeoPrimitive.purpleDeep
    "purplePaper" -> NeoPrimitive.purplePaper
    "green" -> NeoPrimitive.green
    "greenPaper" -> NeoPrimitive.greenPaper
    "greenDeep" -> NeoPrimitive.greenDeep
    "greenLight" -> NeoPrimitive.greenLight
    "blueInk" -> NeoPrimitive.blueInk
    "bluePaper" -> NeoPrimitive.bluePaper
    "blueDeep" -> NeoPrimitive.blueDeep
    "amberInk" -> NeoPrimitive.amberInk
    "amberPaper" -> NeoPrimitive.amberPaper
    "amberDeep" -> NeoPrimitive.amberDeep
    "amberLight" -> NeoPrimitive.amberLight
    "red" -> NeoPrimitive.red
    "redInk" -> NeoPrimitive.redInk
    "redPaper" -> NeoPrimitive.redPaper
    "redDeep" -> NeoPrimitive.redDeep
    "redLight" -> NeoPrimitive.redLight
    "black" -> NeoPrimitive.black
    else -> error("Primitivo desconhecido no teste: '$name'")
}

/** Par de contraste declarado em `docs/design/tokens.json` (`contrastChecks`). */
internal data class ContrastPair(val foreground: String, val background: String, val minimum: Double)

/**
 * Os 29 pares de contraste da especificação, transcritos de
 * `contrastChecks`. Transcrever é deliberado: o teste precisa falhar com o
 * valor **declarado**, não com o que o Kotlin acha que declarou.
 */
internal val declaredContrastPairs: List<ContrastPair> = listOf(
    ContrastPair("text.primary", "background", 4.5),
    ContrastPair("text.primary", "surface", 4.5),
    ContrastPair("text.primary", "surface.alt", 4.5),
    ContrastPair("text.muted", "background", 4.5),
    ContrastPair("text.muted", "surface", 4.5),
    ContrastPair("text.muted", "surface.alt", 4.5),
    ContrastPair("action.text", "action.background", 4.5),
    ContrastPair("brand.text", "brand.background", 4.5),
    ContrastPair("link", "background", 4.5),
    ContrastPair("link", "surface", 4.5),
    ContrastPair("link", "surface.alt", 4.5),
    ContrastPair("onLink", "link", 4.5),
    ContrastPair("onSecondary", "secondary", 4.5),
    ContrastPair("secondary.text", "secondary.container", 4.5),
    ContrastPair("onSuccess", "success", 4.5),
    ContrastPair("success.text", "success.container", 4.5),
    ContrastPair("info.text", "info.container", 4.5),
    ContrastPair("warning.text", "warning.container", 4.5),
    ContrastPair("onError", "error", 4.5),
    ContrastPair("error.text", "error.container", 4.5),
    ContrastPair("disabled.text", "disabled.background", 4.5),
    ContrastPair("inverse.text", "inverse.background", 4.5),
    ContrastPair("inverse.primary", "inverse.background", 4.5),
    ContrastPair("border.default", "background", 3.0),
    ContrastPair("border.default", "surface", 3.0),
    ContrastPair("border.default", "surface.alt", 3.0),
    ContrastPair("focus", "surface", 3.0),
    ContrastPair("border.onAccent", "action.background", 3.0),
    ContrastPair("border.onAccent", "brand.background", 3.0),
)

/** Caminhos tentados para o JSON de tokens, do mais provável ao menos. */
private val tokensCandidates = listOf(
    "../../docs/design/tokens.json",
    "../docs/design/tokens.json",
    "docs/design/tokens.json",
)

/**
 * Lê `docs/design/tokens.json` a partir do diretório de trabalho do teste.
 *
 * O app **não** lê este arquivo em runtime: o JSON é a especificação, o
 * Kotlin é a implementação, e este teste é a ponte entre os dois. Falha com
 * mensagem explícita se o arquivo não estiver onde o Gradle costuma rodar —
 * melhor falhar alto do que pular silenciosamente e não verificar nada.
 */
internal fun readTokensJson(): String {
    val file = tokensCandidates.map { File(it) }.firstOrNull { it.isFile }
        ?: error(
            "tokens.json não encontrado a partir de ${File(".").absolutePath}; " +
                "tentado: ${tokensCandidates.joinToString()}",
        )
    return file.readText()
}

/** Extrai `nome -> "#RRGGBB"` de um bloco delimitado do JSON. */
internal fun primitivesFrom(json: String): Map<String, String> {
    val block = json.substringAfter("\"primitives\"").substringBefore("\"themes\"")
    return Regex("\"([A-Za-z]+)\"\\s*:\\s*\"(#[0-9A-Fa-f]{6})\"")
        .findAll(block)
        .associate { it.groupValues[1] to it.groupValues[2] }
}

/** Extrai `papel -> primitivo` do bloco de um tema. */
internal fun themeFrom(json: String, theme: String): Map<String, String> {
    // Recorta só o corpo do tema pedido: sem o `substringBefore("}")` o
    // bloco seguinte (`dark`) entra na leitura e sobrescreve os papéis.
    val block = json.substringAfter("\"$theme\"")
        .substringAfter("{")
        .substringBefore("}")
    return Regex("\"([A-Za-z.]+)\"\\s*:\\s*\"([A-Za-z]+)\"")
        .findAll(block)
        .associate { it.groupValues[1] to it.groupValues[2] }
}
