# Agent Portal — Extended roadmap

Prioritized tracks for the native Android client (and future extended-features surface) for Agent Portal.

## Status

| Priority | Track | Status |
|----------|-------|--------|
| P0 | Compose UI skeleton (login, session list, chat/transcript — navy/teal branding) | Done |
| P0 | Network/data layer — Retrofit/OkHttp/Room/STOMP | Done |
| P0 | ViewModels wiring UI to data layer (AuthViewModel, SessionListViewModel, ChatViewModel) + nav graph | Done |
| P1 | Password-lane login wired end-to-end | **Done, verified on a real device** (Realme P2 Pro) |
| P1 | OAuth/PKCE SSO via Custom Tabs + AppAuth-Android | Built, **not live** — needs css-next Q1/Q2 promote for redirect allow-list |
| P1 | Realtime chat wired to STOMP client | **Done** — live `assistant_delta` typewriter |
| P1 | Chat input bar keyboard handling | **Done** — `imePadding()` |
| P1 | Access-token refresh + error surfacing | **Done** (`v0.2.6`) — 403-triggered interceptor + WS handshake retry |
| P1 | Biometric app-lock + EncryptedSharedPreferences | Done |
| **P0 happy path** | **Sessions → Chat → Decision sheet → stream → archive** | **Done in `v0.3.0-happy-path-dev`** — Needs-you filter, thin create sheet (provider + demo), Decision bottom sheet (Allow once / Always / Reject + plan Accept/Reject), Cancel/Archive overflow, notification tap → open chat |
| P2 | Firebase Cloud Messaging + device tokens | Live end-to-end (code path); **on-device push arrival still unverified** (no ADB on build host) |
| P2 | Inline notification-action permission approval | Done for foreground/backgrounded-but-alive; FCM path wired |
| P2 | Device Lab E2E on Realme P2 Pro | Blocked — no ADB on build host |

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
