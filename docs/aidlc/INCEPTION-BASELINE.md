# AI-DLC Inception Baseline - agent-portal-extended

**Captured:** 2026-08-01 (as-is snapshot, not a target design)

## Purpose

Native Android client for [Agent Portal](https://github.com/sivaram311/agent-portal) (`E:\MyWorkspace\agent-portal`, Spring Boot + Angular). It lets users run and supervise AI coding-agent sessions from a phone instead of only a browser. This is a **separate repo** from `agent-portal` itself (`https://github.com/sivaram311/agent-portal-extended`); it consumes the existing Agent Portal REST/WebSocket API and does not fork or duplicate the backend.

## Tech stack

Derived from `android/` Gradle project and root README:

| Layer | As stated in-repo |
|-------|-------------------|
| Language / UI | Kotlin **2.3.21**, Jetpack Compose (BOM `2025.07.00`), Material3 |
| Package / app ID | `buzz.delena.agentportal` |
| Build | Gradle **8.14** (`gradle-wrapper.properties`), Android Gradle Plugin **8.11.1**, KSP **2.3.10**, Google Services plugin **4.4.2** |
| SDK | `compileSdk` / `targetSdk` **35**, `minSdk` **26**; JVM target **17** (JDK **21** listed as build prerequisite in README) |
| Networking | OkHttp BOM **4.12.0**, Retrofit **2.11.0**, kotlinx-serialization-json **1.7.3**, kotlinx-coroutines-android **1.9.0** |
| Local data | Room **2.7.2**, EncryptedSharedPreferences (`security-crypto` **1.1.0-alpha06**), DataStore Preferences **1.1.1** |
| Auth / SSO | AppAuth-Android **0.11.1** (Custom Tabs + OAuth/PKCE) |
| Push | Firebase BOM **33.13.0** (`firebase-messaging-ktx`) |
| Markdown | Markwon **4.6.2** + prism4j **2.0.0** |
| Other AndroidX | Navigation Compose **2.9.0**, Biometric **1.2.0-alpha05**, WorkManager **2.10.2**, SplashScreen **1.0.1** |
| App version | `versionName` `0.4.8-oom-http-log-fix-dev`, `versionCode` **18** |
| Composition | Manual `NetworkModule` / `AppContainer` (no Hilt/Koin dependency present) |

Default DEV backend URLs in `BuildConfig`: `API_BASE_URL` = `https://delena.buzz`, `WS_BASE_URL` = `wss://delena.buzz`.

## Current features (as-built)

**Navigation (Compose):** three routes — `login`, `sessions`, `chat/{sessionId}` (`Routes.kt` / `NavGraph.kt`).

**Auth**
- Password login via runtime `GET api/auth/config`, then POST credentials to CSS auth server (`authUrl` + `loginPath`)
- OAuth/PKCE SSO via Custom Tabs + AppAuth; authorize/token on css-next issuer; redirect `buzz.delena.agentportal://oauth/callback`
- Access-token refresh on HTTP 403 (REST interceptor) and on WebSocket handshake 403; Manage → Reconnect / Sign out
- EncryptedSharedPreferences for JWT storage; biometric / device-credential `AppLockGate` when a session is stored (fails open with warning if neither configured)

**Sessions list**
- List/create sessions (providers Cursor | Antigravity; thin create with workspace `demo`)
- Filters: All / Needs you / Running / Failed
- Connection status strip (Password/SSO, JWT subject, TTL, refresh readiness, auth host)
- Pull-to-refresh; Manage sheet with Reconnect, Sign out, Send diagnostics

**Chat / supervisor loop**
- Transcript with Claude-style thread UX (navy/teal branding): collapsible tool/files chips, Activity timeline, tool detail (Raw/Render), Changes sheet (diff + Keep/Restore), Sub-agents sheet (abandon)
- Live STOMP streaming (`assistant_delta` / `thinking_delta`); Decision bottom sheet (permission/plan); Cancel / Archive
- Composer with `imePadding()`; thumb-zone Auto / attach / mic affordances (as documented in README)
- Prompt-safe REST timeouts (read 5m / call 6m); STOMP keep-alive (client heartbeats + OkHttp WS pings + auto-reconnect with resubscribe)
- Client header `X-Agent-Portal-Client: android` for portal rate-limit exemption

**Push / notifications**
- FCM registration via `POST/DELETE api/devices`; `AgentPortalFirebaseMessagingService`
- Inline notification Approve/Reject for pending tool permissions (`PermissionApprovalNotifier` / `PermissionActionReceiver`)
- Notification tap navigates to Chat

**Diagnostics**
- Manage → Send diagnostics (`POST api/diagnostics/client-logs`); pending crash dump upload on next launch
- OkHttp debug logging HEADERS-only (never BODY); tools fetched with `?compact=true`

**REST surface used (Retrofit `AgentPortalApi` / `DeviceApi`)**
- `GET api/auth/config`, `GET api/health`
- Sessions: list/create/get, messages, prompt, cancel, archive/unarchive, permissions + decide, tools (`compact`), changes + diff/accept/reject, subagent abandon
- Devices: `POST api/devices`, `DELETE api/devices/{token}`
- Diagnostics: `POST api/diagnostics/client-logs`
- WebSocket: `{WS_BASE_URL}/ws/websocket` (STOMP-over-WebSocket)

**Explicitly not shipped (per README / ROADMAP):** remaining portal web tabs parity (Code / Guidance / Console); formal Device Lab E2E of SSO on Realme P2 Pro; verified real-device FCM arrival (code paths live; arrival unverified — no ADB on build host).

## Deploy topology (known facts below - cross-check against what you find in-repo, note any discrepancy explicitly rather than silently picking one)

**Given facts (task brief):** Native Android (Kotlin/Compose) client for agent-portal — no web port; talks to agent-portal REST/WebSocket API via OAuth/PKCE SSO through css-next.

**In-repo cross-check — aligned:**
- Project is Android-only under `android/` (Gradle app `AgentPortalMobile`); no web frontend/port in this repo
- Default API/WS hosts: `https://delena.buzz` / `wss://delena.buzz` (`build.gradle.kts` BuildConfig)
- STOMP path documented and coded as `/ws/websocket` (`StompWebSocketClient.kt`)
- Auth config from portal `GET api/auth/config`; password lane hits CSS `authUrl`+`loginPath`; SSO uses AppAuth against issuer `{issuer}/oauth/authorize` and `{issuer}/oauth/token` (README names issuer host `https://css-next.delena.buzz`; code resolves issuer from runtime auth config)
- Redirect scheme allow-list: `buzz.delena.agentportal://oauth/callback` (manifest + AppAuth placeholder)

**Discrepancy / staleness noted:**
- Root README **Security note** still says “Password-lane login only until OAuth/PKCE SSO lands,” but Features / ROADMAP / code show SSO shipped as of **v0.4.4**. Treat the Security note as outdated relative to the Features list and source.

## Known debt / gaps (as-is, factual)

- **No automated tests in tree:** `android/app/src` contains only `main/`; JUnit / AndroidX Test dependencies are declared but no `*Test*.kt` sources found
- **No DI framework:** `NetworkModule` comment states no Hilt/Koin — manual composition root
- **Device verification gaps (ROADMAP P2):** FCM on-device arrival unverified; Device Lab E2E on Realme P2 Pro blocked (no ADB on build host); formal SSO Device Lab E2E deferred (README)
- **Portal UI parity incomplete:** Code / Guidance / Console tabs not in Android client (README deferred; Sub-agents shipped v0.4.6)
- **Stale README Security note** (password-only claim vs shipped SSO) — see Deploy topology
- **No `TODO`/`FIXME` comments** found under `android/` Kotlin sources at inspection time
- Unrelated working-tree change present at capture time (not part of this baseline doc): modified `android/.../HttpErrorMessages.kt`

## Sources consulted

- `README.md`
- `ROADMAP.md`
- `docs/HANDOFF.md`
- `android/build.gradle.kts`
- `android/settings.gradle.kts`
- `android/app/build.gradle.kts`
- `android/gradle/wrapper/gradle-wrapper.properties`
- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/java/buzz/delena/agentportal/nav/NavGraph.kt`
- `android/app/src/main/java/buzz/delena/agentportal/nav/Routes.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/network/AgentPortalApi.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/network/AuthApi.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/network/DeviceApi.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/network/NetworkModule.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/network/StompWebSocketClient.kt` (header/comments + `/ws/websocket`)
- `android/app/src/main/java/buzz/delena/agentportal/core/network/TokenRefresher.kt`
- `android/app/src/main/java/buzz/delena/agentportal/core/data/AuthRepository.kt`
- `android/app/src/main/java/buzz/delena/agentportal/ui/viewmodel/AuthViewModel.kt` (SSO issuer paths / redirect URI)
- Repo layout inspection: `docs/`, `agents/hires/` (listed for awareness; content not required for feature inventory beyond ROADMAP/README gaps)
- `git status --short` at start of capture (pre-existing `HttpErrorMessages.kt` modification noted only)
)
