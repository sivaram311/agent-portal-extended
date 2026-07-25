package buzz.delena.agentportal.nav

/**
 * Shared route contract. Screens under the ui.screens package and the
 * eventual ViewModels that wire them to core.data repositories both target
 * these routes.
 */
object Routes {
    const val LOGIN = "login"
    const val SESSION_LIST = "sessions"
    const val CHAT = "chat/{sessionId}"

    fun chat(sessionId: String) = "chat/$sessionId"
}
