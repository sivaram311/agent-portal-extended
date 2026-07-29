package buzz.delena.agentportal.core.data

/**
 * How the current CSS tokens were obtained. Persisted alongside the tokens so
 * the UI can show Password vs SSO without re-deriving it from the JWT
 * (both lanes currently mint the same agent-portal client tokens).
 */
enum class AuthMethod {
    PASSWORD,
    SSO,
    UNKNOWN,
    ;

    val label: String
        get() = when (this) {
            PASSWORD -> "Password"
            SSO -> "SSO"
            UNKNOWN -> "Unknown"
        }
}
