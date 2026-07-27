# Agent Portal — Extended roadmap

Prioritized tracks for the native Android client (and future extended-features surface) for Agent Portal.

## Status

| Priority | Track | Status |
|----------|-------|--------|
| P0 | Compose UI skeleton (login, session list, chat/transcript — navy/teal branding) | Done |
| P0 | Network/data layer — Retrofit/OkHttp/Room/STOMP | Done |
| P0 | ViewModels wiring UI to data layer (AuthViewModel, SessionListViewModel, ChatViewModel) + nav graph | Done |
| P1 | Password-lane login wired end-to-end | Done — code path complete, **unverified on device** (no ADB on build host) |
| P1 | OAuth/PKCE SSO via Custom Tabs + AppAuth-Android | Built, **not live** — the `centralized-security-system` redirect-allow-list fix (`OAuthService.isRedirectUriAllowed`) exists only in local DEV source; the auth server the app actually talks to (`css-next.delena.buzz`) runs `G:\apps\css-next\centralized-security-system.jar` (PROD). Needs a real Q1/Q2 promote (evidence pack + EM GO) before SSO can complete against a live server — not an ad-hoc restart, other apps share that instance |
| P1 | Realtime chat wired to STOMP client | Done (generic refetch-on-event) — full per-event-type parsing of the backend's event schema is still Backlog |
| P1 | Biometric app-lock + EncryptedSharedPreferences token storage hardening | Done — `AppLockGate` gates the nav host behind `BiometricPrompt`/device-credential when a stored session exists; fails open with a visible warning if the device has neither configured, so it can't hard-lock a user out |
| P2 | Firebase Cloud Messaging + backend `device_tokens` table + `WebhookService`-sibling push dispatch | **Live end-to-end.** Firebase project provisioned; Android registered/receiving; backend `PushNotificationService` initializes the Admin SDK from a service-account key (`app.firebase.credentials-path`, machine-local at `E:\MyAgent\workflow\secrets\firebase-admin-agent-portal.json`, never committed) and really sends. Verified with a throwaway JUnit smoke test hitting real Firebase servers with a deliberately-invalid token — got back a genuine `INVALID_ARGUMENT` FCM rejection (proves auth succeeded), not a credential error. Stale/unregistered tokens are auto-deleted from `device_tokens` on send failure. **Not yet verified with a real push arriving on a real device** — no ADB on this build host |
| P2 | Inline notification-action permission approval (the actual remote-session differentiator) | Done for foreground/backgrounded-but-alive app, and now correctly wired for real push too — **found and fixed a real event-naming bug**: Cursor's actual tool/plan approval events are `permission_required`/`plan_required` (the ones carrying a real `permissionId`), not `input_required` (Antigravity's free-text nudge, never has one) — `SessionEventBus` was only forwarding the original 4 webhook event types to push, so a real Cursor permission request would never have triggered a push notification. Fixed: push now also fires on `permission_required`/`plan_required` (webhook contract left unchanged) and the Android FCM handler matches |
| P2 | Device Lab E2E on Realme P2 Pro once a physical device/ADB is available | Blocked — no ADB on build host, same as forgecity-launcher |

## How to use

This repo does not (yet) have Cursor skills of its own; treat each row above as a self-contained workstream. Keep root `README.md` and this roadmap updated when a track changes.

## Related

- Backend/API this app consumes: [`agent-portal`](https://github.com/sivaram311/agent-portal) — [docs/ROADMAP.md](../agent-portal/docs/ROADMAP.md) tracks the corresponding backend/frontend work (webhooks, device tokens, CSS).
- Handoff / build state: [docs/HANDOFF.md](docs/HANDOFF.md).
- Device SoT for E2E: `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`.
