// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.util

import android.util.Log

/**
 * Utilitário de log seguro para chamadas em testes unitários JVM.
 *
 * Em testes JVM puros onde o SDK Android não é mockado (ex.: Robolectric
 * desabilitado), chamadas diretas a `Log.d` ou `Log.e` lançam
 * `RuntimeException("Method d in android.util.Log not mocked")`.
 * O [runCatching] silencia a exceção em testes JVM mantendo a execução
 * normal em runtime Android.
 */
internal fun logDebug(
    tag: String,
    message: String,
) {
    runCatching { Log.d(tag, message) }
}

internal fun logWarn(
    tag: String,
    message: String,
) {
    runCatching { Log.w(tag, message) }
}

internal fun logError(
    tag: String,
    message: String,
    throwable: Throwable? = null,
) {
    if (throwable != null) {
        runCatching { Log.e(tag, message, throwable) }
    } else {
        runCatching { Log.e(tag, message) }
    }
}

/**
 * Mascara e-mail para log: `jo***@exemplo.com`. Evita PII em claro no
 * logcat sem perder a capacidade de correlacionar eventos.
 */
internal fun String.maskEmail(): String {
    val at = indexOf('@')
    if (at <= 0) return "***"
    return take(2) + "***" + substring(at)
}
