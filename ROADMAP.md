# Agent Portal — Extended roadmap

Prioritized tracks for the native Android client (and future extended-features surface) for Agent Portal.

## Status

| Priority | Track | Status |
|----------|-------|--------|
| P0 | Compose UI skeleton (login, session list, chat/transcript — navy/teal branding) | Done |
| P0 | Network/data layer — Retrofit/OkHttp/Room/STOMP | Done |
| P0 | ViewModels wiring UI to data layer (AuthViewModel, SessionListViewModel, ChatViewModel) + nav graph | Done |
| P1 | Password-lane login wired end-to-end | Done — code path complete, **unverified on device** (no ADB on build host) |
| P1 | OAuth/PKCE SSO via Custom Tabs + AppAuth-Android | Backlog — needs css-next redirect allow-list to add the app's custom URL scheme |
| P1 | Realtime chat wired to STOMP client | Done (generic refetch-on-event) — full per-event-type parsing of the backend's event schema is still Backlog |
| P1 | Biometric app-lock + EncryptedSharedPreferences token storage hardening | Backlog |
| P2 | Firebase Cloud Messaging + backend `device_tokens` table + `WebhookService` push dispatch | Backlog |
| P2 | Inline notification-action permission approval (the actual remote-session differentiator) | Backlog |
| P2 | Device Lab E2E on Realme P2 Pro once a physical device/ADB is available | Blocked — no ADB on build host, same as forgecity-launcher |

## How to use

This repo does not (yet) have Cursor skills of its own; treat each row above as a self-contained workstream. Keep root `README.md` and this roadmap updated when a track changes.

## Related

- Backend/API this app consumes: [`agent-portal`](https://github.com/sivaram311/agent-portal) — [docs/ROADMAP.md](../agent-portal/docs/ROADMAP.md) tracks the corresponding backend/frontend work (webhooks, device tokens, CSS).
- Handoff / build state: [docs/HANDOFF.md](docs/HANDOFF.md).
- Device SoT for E2E: `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`.
