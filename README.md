# Agent Portal — Extended

Native Android client (and future extended-features surface) for **[Agent Portal](https://github.com/sivaram311/agent-portal)** (`E:\MyWorkspace\agent-portal`, Spring Boot 3.5 + Angular 19) — lets users run and supervise AI coding-agent sessions from a phone instead of only a browser.

This is a **separate repo** from `agent-portal` itself: `https://github.com/sivaram311/agent-portal-extended` (public). It talks to the existing Agent Portal REST/WebSocket API; it does not fork or duplicate the backend.

**Status:** skeleton milestone `v0.1.0-skeleton-dev` (versionCode 1). Builds clean on the host. **Zero device/emulator verification** — no ADB/emulator on this build host (same limitation this machine has repeatedly hit with `E:\MyWorkspace\sandbox\forgecity-launcher`; see [docs/HANDOFF.md](docs/HANDOFF.md) for the disclosure pattern).

## Features

Built (this milestone):

- Compose UI skeleton — login screen, session list, chat/transcript screen with a Claude-app-like UX, styled in Agent Portal's own **navy/teal** branding (not Anthropic's)
- Retrofit/OkHttp networking layer against the existing Agent Portal REST API
- Hand-rolled STOMP-over-WebSocket client for realtime session streaming (`/ws/websocket` on the portal backend)
- Room for local session/message caching
- EncryptedSharedPreferences for JWT token storage
- Password-lane login, session list, and chat wired end-to-end (ViewModels + nav graph) against the network/data layer above — **unverified on a physical device**, no ADB on this build host

Not yet done / explicitly deferred (see [ROADMAP.md](ROADMAP.md)):

- OAuth/PKCE SSO login (password-lane login is wired; SSO is not)
- Firebase Cloud Messaging push notifications (dependency present, not wired — no Firebase project / `google-services.json` provisioned yet)
- Biometric app-lock
- Backend-side `device_tokens` table + push dispatch extension to `WebhookService` (lives in the `agent-portal` repo, not here)
- Inline notification-action tool-permission-approval flow — the actual "remote session" differentiator

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
