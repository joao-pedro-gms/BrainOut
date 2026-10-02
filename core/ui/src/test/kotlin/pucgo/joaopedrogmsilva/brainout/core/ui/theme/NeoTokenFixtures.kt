// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Fixtures compartilhadas dos testes de token (NB-04).

package pucgo.joaopedrogmsilva.brainout.core.ui.theme

import androidx.compose.ui.graphics.Color
import java.io.File

private val roleAccessors: Map<String, (NeoColors) -> Color> = mapOf(
    "background" to { it.background },
    "surface" to { it.surface },
    "surface.alt" to { it.surfaceAlt },
    "text.primary" to { it.textPrimary },
    "text.muted" to { it.textMuted },
    "border.default" to { it.borderDefault },
    "border.onAccent" to { it.borderOnAccent },
    "focus" to { it.focus },
    "shadow" to { it.shadow },
    "action.background" to { it.actionBackground },
    "action.text" to { it.actionText },
    "brand.background" to { it.brandBackground },
    "brand.text" to { it.brandText },
    "link" to { it.link },
    "onLink" to { it.onLink },
    "secondary" to { it.secondary },
    "onSecondary" to { it.onSecondary },
    "secondary.container" to { it.secondaryContainer },
    "secondary.text" to { it.secondaryText },
    "success" to { it.success },
    "onSuccess" to { it.onSuccess },
    "success.container" to { it.successContainer },
    "success.text" to { it.successText },
    "info" to { it.info },
    "info.container" to { it.infoContainer },
    "info.text" to { it.infoText },
    "warning" to { it.warning },
    "warning.container" to { it.warningContainer },
    "warning.text" to { it.warningText },
    "error" to { it.error },
    "onError" to { it.onError },
    "error.container" to { it.errorContainer },
    "error.text" to { it.errorText },
    "disabled.background" to { it.disabledBackground },
    "disabled.text" to { it.disabledText },
    "inverse.background" to { it.inverseBackground },
    "inverse.text" to { it.inverseText },
    "inverse.primary" to { it.inversePrimary },
    "scrim" to { it.scrim },
)

/**
 * Resolve o nome de um papel semântico (`"text.primary"`, `"info.container"`)
 * para a cor efetiva de um tema.
 *
 * Existe para que os testes de contraste e de paridade leiam os papéis pelo
 * **nome usado na especificação**, e não pela propriedade Kotlin: assim um
 * renomeio no Kotlin quebra o teste, em vez de passar despercebido. O mapa
 * evita um `when` de 39 ramos (que estoura o limite de complexidade do
 * detekt sem trazer nenhum benefício aqui).
 */
internal fun neoRole(colors: NeoColors, role: String): Color =
    roleAccessors[role]?.invoke(colors)
        ?: error("Papel semântico desconhecido no teste: '$role'")

private val primitives: Map<String, Color> = mapOf(
    "ink" to NeoPrimitive.ink,
    "paper" to NeoPrimitive.paper,
    "white" to NeoPrimitive.white,
    "neutral" to NeoPrimitive.neutral,
    "muted" to NeoPrimitive.muted,
    "yellow" to NeoPrimitive.yellow,
    "violet" to NeoPrimitive.violet,
    "mint" to NeoPrimitive.mint,
    "blue" to NeoPrimitive.blue,
    "orange" to NeoPrimitive.orange,
    "night" to NeoPrimitive.night,
    "charcoal" to NeoPrimitive.charcoal,
    "graphite" to NeoPrimitive.graphite,
    "mutedDark" to NeoPrimitive.mutedDark,
    "shadowDark" to NeoPrimitive.shadowDark,
    "ochre" to NeoPrimitive.ochre,
    "purple" to NeoPrimitive.purple,
    "purpleLight" to NeoPrimitive.purpleLight,
    "purpleDeep" to NeoPrimitive.purpleDeep,
    "purplePaper" to NeoPrimitive.purplePaper,
    "green" to NeoPrimitive.green,
    "greenPaper" to NeoPrimitive.greenPaper,
    "greenDeep" to NeoPrimitive.greenDeep,
    "greenLight" to NeoPrimitive.greenLight,
    "blueInk" to NeoPrimitive.blueInk,
    "bluePaper" to NeoPrimitive.bluePaper,
    "blueDeep" to NeoPrimitive.blueDeep,
    "amberInk" to NeoPrimitive.amberInk,
    "amberPaper" to NeoPrimitive.amberPaper,
    "amberDeep" to NeoPrimitive.amberDeep,
    "amberLight" to NeoPrimitive.amberLight,
    "red" to NeoPrimitive.red,
    "redInk" to NeoPrimitive.redInk,
    "redPaper" to NeoPrimitive.redPaper,
    "redDeep" to NeoPrimitive.redDeep,
    "redLight" to NeoPrimitive.redLight,
    "black" to NeoPrimitive.black,
)

/** Resolve o nome de um primitivo (`"ink"`, `"redPaper"`) para a cor Kotlin. */
internal fun neoPrimitive(name: String): Color =
    primitives[name] ?: error("Primitivo desconhecido no teste: '$name'")

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
