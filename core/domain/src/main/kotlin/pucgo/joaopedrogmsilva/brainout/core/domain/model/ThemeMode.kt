// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.domain.model

/**
 * Modo de tema do app: a escolha manual System/Light/Dark (NB-06,
 * issue #93, E4.5).
 *
 * Antes do NB-06 a alternância vinha apenas de
 * `isSystemInDarkTheme()` — não existia escolha manual persistida
 * (inventário auditado no DESIGN.md §1). O enum é a **infraestrutura
 * mínima**: define os três modos, a chave de persistência e a
 * precedência. A tela que grava a escolha é NB-27, fora do escopo
 * daqui.
 *
 * Precedência (`resolvesToDark`): [LIGHT] e [DARK] vencem a
 * configuração do sistema em qualquer sentido; [SYSTEM] segue o
 * sistema. A leitura da configuração acontece **uma única vez** no
 * `MainActivity` — nenhuma rota recalcula com `isSystemInDarkTheme()`
 * (plano §3.2.2), senão a escolha manual se perderia por tela.
 */
enum class ThemeMode {
    /** Segue a configuração do sistema (default de fábrica). */
    SYSTEM,

    /** Claro forçado, mesmo com o sistema escuro. */
    LIGHT,

    /** Escuro forçado, mesmo com o sistema claro. */
    DARK,
    ;

    /**
     * Resolve o modo para o estado escuro efetivo.
     *
     * @property systemInDarkTheme bit `Configuration.UI_MODE_NIGHT_*`
     *  lido no ponto único de resolução (a raiz do app).
     * @return `true` quando o tema efetivo é o escuro.
     */
    fun resolvesToDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    /** Serialização textual em `DataStore` (mesmo padrão do `SortOrder`). */
    fun toStorageKey(): String = name.lowercase()

    companion object {

        /**
         * Desserializa a chave de `DataStore`. Valor ausente ou
         * desconhecido (escrita externa, downgrade) cai no default
         * [SYSTEM] em vez de quebrar a UI.
         */
        fun fromStorageKey(value: String?): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            null, "", "system" -> SYSTEM
            else -> SYSTEM
        }
    }
}
