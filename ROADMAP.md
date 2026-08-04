# Foreman roadmap

Prioritized tracks for the native Android client (and future extended-features surface) for Agent Portal.

## Status

| Priority | Track | Status |
|----------|-------|--------|
| P0 | Compose UI skeleton (login, session list, chat/transcript — navy/teal branding) | Done |
| P0 | Network/data layer — Retrofit/OkHttp/Room/STOMP | Done |
| P0 | ViewModels wiring UI to data layer (AuthViewModel, SessionListViewModel, ChatViewModel) + nav graph | Done |
| P1 | Password-lane login wired end-to-end | **Done, verified on a real device** (Realme P2 Pro) |
| P1 | OAuth/PKCE SSO via Custom Tabs + AppAuth-Android | **v0.4.4** — css-next allow-list live; Android `<queries>` so Custom Tabs open |
| P1 | Realtime chat wired to STOMP client | **Done** — live `assistant_delta` typewriter |
| P1 | Chat input bar keyboard handling | **Done** — `imePadding()` |
| P1 | Access-token refresh + error surfacing | **Done** (`v0.2.6`) — 403-triggered interceptor + WS handshake retry |
| P1 | Biometric app-lock + EncryptedSharedPreferences | Done |
| **P0 happy path** | Sessions → Chat → Decision → stream → archive | Done `v0.3.0` |
| **P0 Claude thread** | In-thread activity chips + bottom sheets | Done `v0.4.0` |
| **P0 tool noise** | Turn-scope + categorize + filter + no random toolCallId | **Done `v0.4.1-tool-noise-fix-dev`** |
| **P0 prompt/WS** | Long REST timeouts + STOMP heartbeat/reconnect + auth status strip | **Done `v0.4.2`** |
| **P0 account** | Manage sheet: Reconnect + Sign out; Unknown ≠ red | **Done `v0.4.3-reconnect-logout-dev`** |
| **P0 SSO/session** | SSO opens + no false “expired” while JWT TTL left | **Done `v0.4.4-sso-session-fix-dev`** |
| **P0 rate limit** | Android exempt via `X-Agent-Portal-Client: android` | **Done `v0.4.5-android-rate-limit-exempt-dev`** (needs portal RateLimitFilter) |
| **P0 subagents** | Sub-agents sheet + abandon (Logs-tab parity) | **Done `v0.4.6-subagents-crash-fix-dev`** |
| **P0 diagnostics** | Send diagnostics + crash dump upload to portal | **Done `v0.4.7-diagnostics-dev`** |
| **P0 OOM tools** | Stop BODY HTTP log + compact `/tools` (45MB session crash) | **Done `v0.4.8-oom-http-log-fix-dev`** |
| **P0 rename + fixes** | Foreman rename, About label, refresh-token UX, WS reconnect hardening | **Done `v1.0.0`** |
| P2 | Firebase Cloud Messaging on-device proof | Code live; arrival unverified |
| P2 | Device Lab E2E on Realme P2 Pro | Blocked — no ADB |

## CONSCIOUS #16 waiver (release tags on this project)

**Status:** user-directed waiver, 2026-08-04 — applies to this project only, until ADB access exists.

CONSCIOUS #16 normally requires Device Lab E2E on DEV before any annotated release tag ("Missing DEV E2E → NO-GO for tag"). This project cannot satisfy that gate structurally — there is no ADB access on the build host (see the "Device Lab E2E … Blocked — no ADB" row above), so Device Lab E2E has never been runnable here, not even once, for any prior tag either.

The user explicitly waived the gate for this project: *"apk e2e does not apply for this project alone. since adb required."* Tags on `agent-portal-extended` therefore proceed on Reviewer SIGN-OFF + a successful local build (`./gradlew :app:assembleDebug` / `compileDebugKotlin`) as the smoke bar, without Device Lab E2E evidence, until ADB access is available. This is a standing, documented exception for this repo — re-confirm with the user if picking this up much later, rather than assuming it's still current.

## Happy path (shipped)

```
Login → Sessions (filter: Needs you) → Chat
  → Decision sheet (permission/plan)
  → Watch stream / send follow-up
  → ⋮ Archive
```

Notification tap opens the same Chat + decision loop.

## How to use

Treat each row as a self-contained workstream. Keep root `README.md` and this roadmap updated when a track changes.

## Related

- Backend/API: [`agent-portal`](https://github.com/sivaram311/agent-portal)
- Handoff: [docs/HANDOFF.md](docs/HANDOFF.md)
- Device SoT: `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
