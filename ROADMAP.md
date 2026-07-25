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
| P2 | Firebase Cloud Messaging + backend `device_tokens` table + `WebhookService`-sibling push dispatch | Android half live — Firebase project provisioned, `google-services.json` + the `google-services` Gradle plugin wired in, `AgentPortalFirebaseMessagingService` registered and receiving (`onNewToken` registers with the backend, `onMessageReceived` handles `input_required` data messages). Backend half still log-only: `PushNotificationService.sendToDevice` needs a Firebase Admin SDK service-account key (separate from `google-services.json`) before it can actually send — one isolated `TODO(firebase)` method + the `firebase-admin` dependency once that key exists |
| P2 | Inline notification-action permission approval (the actual remote-session differentiator) | Done for foreground/backgrounded-but-alive app — `PermissionApprovalNotifier` posts an Approve/Reject system notification the moment `ChatViewModel` detects a new pending permission, handled by `PermissionActionReceiver` calling the same `decidePermission` API the in-app card uses. Extending this to work when the app process is fully killed is the Firebase row above, not a separate task |
| P2 | Device Lab E2E on Realme P2 Pro once a physical device/ADB is available | Blocked — no ADB on build host, same as forgecity-launcher |

## How to use

This repo does not (yet) have Cursor skills of its own; treat each row above as a self-contained workstream. Keep root `README.md` and this roadmap updated when a track changes.

## Related

- Backend/API this app consumes: [`agent-portal`](https://github.com/sivaram311/agent-portal) — [docs/ROADMAP.md](../agent-portal/docs/ROADMAP.md) tracks the corresponding backend/frontend work (webhooks, device tokens, CSS).
- Handoff / build state: [docs/HANDOFF.md](docs/HANDOFF.md).
- Device SoT for E2E: `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`.
