# Agent Portal — Extended

Native Android client (and future extended-features surface) for **[Agent Portal](https://github.com/sivaram311/agent-portal)** (`E:\MyWorkspace\agent-portal`, Spring Boot 3.5 + Angular 19) — lets users run and supervise AI coding-agent sessions from a phone instead of only a browser.

This is a **separate repo** from `agent-portal` itself: `https://github.com/sivaram311/agent-portal-extended` (public). It talks to the existing Agent Portal REST/WebSocket API; it does not fork or duplicate the backend.

**Status:** `v0.4.0-claude-thread-dev` (versionCode 10). Claude-style chat thread: activity chips + bottom sheets for tools/diffs. Happy path (`v0.3.0`) included.

## Features

Built:

- **Claude-style thread** — collapsible “Ran N tools / files changed” chips; Activity timeline sheet; tool detail (monospace + line numbers + Raw/Render); Changes sheet with green/red diff + Keep/Restore; thumb-zone composer (Auto / attach / mic)
- **Happy-path supervisor loop** — All / Needs you / Running / Failed filters; thin create (Cursor|Antigravity + demo); Decision bottom sheet; Cancel/Archive; notification tap → Chat
- Compose UI — login screen, session list, chat/transcript screen with a Claude-app-like UX, styled in Agent Portal's own **navy/teal** branding (not Anthropic's)
- Retrofit/OkHttp networking layer against the existing Agent Portal REST API
- Hand-rolled STOMP-over-WebSocket client for realtime session streaming (`/ws/websocket` on the portal backend)
- Room for local session/message caching
- EncryptedSharedPreferences for JWT token storage
- Password-lane login, session list, and chat wired end-to-end (ViewModels + nav graph), **verified against a live DEV backend** (login → JWT → authenticated session list/chat, all `200`) **and on a real device**
- Real per-token chat streaming — `ChatViewModel` parses the backend's actual STOMP event schema (`assistant_delta`/`thinking_delta`, the same source the web frontend's streaming reads) and appends text live, no more waiting for a full response before anything appears
- Chat input bar keyboard handling (`imePadding()` on the input container) — the input field no longer sits behind the keyboard when typing
- Access-token refresh — real fix for "the app stops responding after a while," on the **second** attempt: the first version relied on OkHttp's `Authenticator`, which only auto-fires on HTTP `401`, but this backend returns `403` for every auth failure (confirmed directly against the live server) — that version passed review and shipped without ever once firing. Now a plain `Interceptor` (triggers on any status code) handles the REST side, and `StompWebSocketClient` retries its own handshake on a 403 directly, since a WebSocket upgrade doesn't go through the interceptor retry path the same way. Refresh itself verified directly against the live auth server (`POST /auth/refresh` → real `200` + fresh token) before any Android code was written. Any remaining send/decide-permission failure shows a dismissible error banner instead of failing invisibly
- Biometric / device-credential app-lock (`AppLockGate`) gating the app when a session is stored — fails open with a visible warning if the device has neither configured
- Inline notification-action permission approval — Approve/Reject a pending tool-permission request straight from a system notification, works today for foreground/backgrounded-but-alive app process
- Backend `POST/DELETE /api/devices` device-token registration + `PushNotificationService`, verified end-to-end on DEV
- Firebase Cloud Messaging — **live end-to-end**, both sides: Android registered/receiving (`AgentPortalFirebaseMessagingService`), backend `PushNotificationService` initializes the Admin SDK from a service-account key and really sends, with stale/unregistered tokens auto-removed. Verified with a real (non-mocked) send against Google's servers — a throwaway smoke test confirmed a genuine FCM-level rejection for a deliberately-invalid token, proving the credential authenticates correctly
- OAuth/PKCE SSO via Custom Tabs + AppAuth-Android — **built but not functional against the live server yet**, see the caveat below

Not yet done / explicitly deferred (see [ROADMAP.md](ROADMAP.md)):

- OAuth/PKCE SSO live against css-next (needs promote)
- A real push notification arriving on a real device (no ADB on this build host)
- Formal Device Lab E2E on Realme P2 Pro
- Full portal tabs (Logs/Code/Guidance) — out of happy-path scope

## Known blocker: OAuth/PKCE SSO is not live yet

SSO is fully implemented (AppAuth-Android PKCE flow, `AuthViewModel.startSsoLogin`/`completeSsoLogin`), including a fix to the auth server's redirect allow-list (`centralized-security-system`'s `OAuthService.isRedirectUriAllowed`) to accept this app's custom URL scheme. That fix exists only in **local DEV source** — the auth server the app actually talks to (`css-next.delena.buzz`) runs from `G:\apps\css-next\centralized-security-system.jar` (**PROD**, shared with other live apps). Making SSO work end-to-end needs a real Q1/Q2 promote of `centralized-security-system` (evidence pack, EM GO/NO-GO) per this machine's standing orders — not an ad-hoc restart. Password-lane login is unaffected and fully functional.

## Prerequisites

Matching the toolchain already used by this machine's other native Android app (`forgecity-launcher`):

- JDK 21
- Android SDK (`compileSdk` 35, `minSdk` 26, `targetSdk` 35)
- Gradle **8.14** (via `gradlew.bat`, no local install needed)
- Android Gradle Plugin **8.11.1**
- Kotlin **2.3.21** (KSP **2.3.10**)
- Jetpack Compose (BOM `2025.07.00`)

## Project layout

```
agent-portal-extended/
  android/            Gradle project (Kotlin 2.3.21, Jetpack Compose, package buzz.delena.agentportal)
    app/src/main/java/buzz/delena/agentportal/
      core/           Networking, STOMP client, Room, auth/token storage
      ui/             Compose screens, navigation, theme
  docs/               HANDOFF.md and other operational docs
  agents/hires/       Reviewer sign-offs and activity-log drafts for this repo
  README.md
  ROADMAP.md
```

## How to build

```powershell
cd android
.\gradlew.bat assembleDebug
```

## Roadmap

See [ROADMAP.md](ROADMAP.md) for prioritized tracks and what's Backlog vs Blocked.

## Security note

This app stores an Agent Portal JWT locally via EncryptedSharedPreferences and talks to whatever backend origin it is configured against. Password-lane login only until OAuth/PKCE SSO lands — keep debug builds pointed at DEV, not PROD, until that milestone ships.
