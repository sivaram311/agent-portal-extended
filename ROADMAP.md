# Agent Portal — Extended roadmap

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
| P2 | Firebase Cloud Messaging on-device proof | Code live; arrival unverified |
| P2 | Device Lab E2E on Realme P2 Pro | Blocked — no ADB |

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
