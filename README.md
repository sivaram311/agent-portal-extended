# Agent Portal â€” Extended

Native Android client (and future extended-features surface) for **[Agent Portal](https://github.com/sivaram311/agent-portal)** (`E:\MyWorkspace\agent-portal`, Spring Boot 3.5 + Angular 19) â€” lets users run and supervise AI coding-agent sessions from a phone instead of only a browser.

This is a **separate repo** from `agent-portal` itself: `https://github.com/sivaram311/agent-portal-extended` (public). It talks to the existing Agent Portal REST/WebSocket API; it does not fork or duplicate the backend.

**Status:** `v0.4.7-diagnostics-dev` (versionCode 17). Manage → Send diagnostics; crash dumps upload on next launch.

## Features

Built:

- **Connection status strip** â€” Sessions + Chat show Password/SSO (persisted at login), JWT subject, access-token TTL, refresh readiness, auth host; Chat also shows Live / Connecting / Offline (tap to expand)
- **Prompt-safe timeouts** â€” REST read 5m / call 6m so ACP handshake on `POST /prompt` does not abort as nginx 499
- **STOMP keep-alive** â€” client heartbeats + OkHttp WS pings + auto-reconnect with backoff; resubscribe on reconnect
- **Claude-style thread** â€” collapsible â€œRan N tools / files changedâ€ chips; Activity timeline sheet; tool detail (monospace + line numbers + Raw/Render); Changes sheet with green/red diff + Keep/Restore; thumb-zone composer (Auto / attach / mic)
- **Happy-path supervisor loop** â€” All / Needs you / Running / Failed filters; thin create (Cursor|Antigravity + demo); Decision bottom sheet; Cancel/Archive; notification tap â†’ Chat
- Compose UI â€” login screen, session list, chat/transcript screen with a Claude-app-like UX, styled in Agent Portal's own **navy/teal** branding (not Anthropic's)
- Retrofit/OkHttp networking layer against the existing Agent Portal REST API
- Hand-rolled STOMP-over-WebSocket client for realtime session streaming (`/ws/websocket` on the portal backend)
- Room for local session/message caching
- EncryptedSharedPreferences for JWT token storage
- Password-lane login, session list, and chat wired end-to-end (ViewModels + nav graph), **verified against a live DEV backend** (login â†’ JWT â†’ authenticated session list/chat, all `200`) **and on a real device**
- Real per-token chat streaming â€” `ChatViewModel` parses the backend's actual STOMP event schema (`assistant_delta`/`thinking_delta`, the same source the web frontend's streaming reads) and appends text live, no more waiting for a full response before anything appears
- Chat input bar keyboard handling (`imePadding()` on the input container) â€” the input field no longer sits behind the keyboard when typing
- Access-token refresh â€” real fix for "the app stops responding after a while," on the **second** attempt: the first version relied on OkHttp's `Authenticator`, which only auto-fires on HTTP `401`, but this backend returns `403` for every auth failure (confirmed directly against the live server) â€” that version passed review and shipped without ever once firing. Now a plain `Interceptor` (triggers on any status code) handles the REST side, and `StompWebSocketClient` retries its own handshake on a 403 directly, since a WebSocket upgrade doesn't go through the interceptor retry path the same way. Refresh itself verified directly against the live auth server (`POST /auth/refresh` â†’ real `200` + fresh token) before any Android code was written. Any remaining send/decide-permission failure shows a dismissible error banner instead of failing invisibly â€” including the backend's real `error` string when present
- Biometric / device-credential app-lock (`AppLockGate`) gating the app when a session is stored â€” fails open with a visible warning if the device has neither configured
- Inline notification-action permission approval â€” Approve/Reject a pending tool-permission request straight from a system notification, works today for foreground/backgrounded-but-alive app process
- Backend `POST/DELETE /api/devices` device-token registration + `PushNotificationService`, verified end-to-end on DEV
- Firebase Cloud Messaging â€” **live end-to-end**, both sides: Android registered/receiving (`AgentPortalFirebaseMessagingService`), backend `PushNotificationService` initializes the Admin SDK from a service-account key and really sends, with stale/unregistered tokens auto-removed. Verified with a real (non-mocked) send against Google's servers â€” a throwaway smoke test confirmed a genuine FCM-level rejection for a deliberately-invalid token, proving the credential authenticates correctly
- OAuth/PKCE SSO via Custom Tabs + AppAuth-Android â€” authorize URL + custom-scheme allow-list verified live on css-next; Android 11+ `<queries>` added in v0.4.4 so Custom Tabs can open

Not yet done / explicitly deferred (see [ROADMAP.md](ROADMAP.md)):

- Formal Device Lab E2E of SSO on Realme P2 Pro
- A real push notification arriving on a real device (no ADB on this build host)
- Remaining portal tabs (Code / Guidance / Console) — Sub-agents shipped in v0.4.6

## SSO notes

Password login uses same-origin `https://delena.buzz/auth/*`. SSO opens `{issuer}/oauth/authorize` on `https://css-next.delena.buzz` (Custom Tabs) and returns via `buzz.delena.agentportal://oauth/callback`. css-next prod jar already allows that custom scheme. If SSO still does nothing, install/update Chrome (or any browser) â€” Android 11+ hides browsers from the app without the manifest `<queries>` block shipped in v0.4.4.

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

This app stores an Agent Portal JWT locally via EncryptedSharedPreferences and talks to whatever backend origin it is configured against. Password-lane login only until OAuth/PKCE SSO lands â€” keep debug builds pointed at DEV, not PROD, until that milestone ships.

