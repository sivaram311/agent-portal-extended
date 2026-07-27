# Agent Portal — Extended roadmap

Prioritized tracks for the native Android client (and future extended-features surface) for Agent Portal.

## Status

| Priority | Track | Status |
|----------|-------|--------|
| P0 | Compose UI skeleton (login, session list, chat/transcript — navy/teal branding) | Done |
| P0 | Network/data layer — Retrofit/OkHttp/Room/STOMP | Done |
| P0 | ViewModels wiring UI to data layer (AuthViewModel, SessionListViewModel, ChatViewModel) + nav graph | Done |
| P1 | Password-lane login wired end-to-end | **Done, verified on a real device** (Realme P2 Pro) 2026-07-27 — login, session list (showing real backend data), notification permission prompt all confirmed working |
| P1 | OAuth/PKCE SSO via Custom Tabs + AppAuth-Android | Built, **not live** — the `centralized-security-system` redirect-allow-list fix (`OAuthService.isRedirectUriAllowed`) exists only in local DEV source; the auth server the app actually talks to (`css-next.delena.buzz`) runs `G:\apps\css-next\centralized-security-system.jar` (PROD). Needs a real Q1/Q2 promote (evidence pack + EM GO) before SSO can complete against a live server — not an ad-hoc restart, other apps share that instance |
| P1 | Realtime chat wired to STOMP client | **Done — real per-event-type parsing, not generic refetch.** `ChatViewModel` now parses the backend's actual `AgentEventDto` shape and appends `assistant_delta`/`thinking_delta` STOMP frames directly into the in-progress message for a live typewriter effect (Cursor's `session/update` stream, same source the web frontend's streaming already uses) — no more waiting for a full REST refetch to see new text. Every other event type still falls back to REST refresh, which naturally supersedes the streaming placeholder with the real persisted message |
| P1 | Chat input bar keyboard handling | **Done** — `ChatInputBar`'s container (in `ChatScreen`'s `bottomBar`) now has `Modifier.imePadding()`. Edge-to-edge (`enableEdgeToEdge()` in `MainActivity`) means `android:windowSoftInputMode="adjustResize"` alone no longer resizes the decor the pre-Compose way; without explicit `imePadding()` the input bar would sit behind the keyboard |
| P1 | Access-token refresh + error surfacing | **Done, on the second attempt — the first shipped fix never actually fired.** `v0.2.5` built a correct `POST {authUrl}/auth/refresh` refresh flow as an OkHttp `Authenticator`, which only auto-triggers on HTTP `401` — but this backend's Spring Security setup returns `403` for every unauthenticated/expired-token request, on both `/api/**` and `/ws/**` (confirmed directly against the live server with `curl`: no token, an invalid token, and no-token-on-the-WS-handshake all return `403`, never `401`). That fix passed review and shipped without ever once being invoked. Real fix (`v0.2.6`): the refresh logic is now a plain `Interceptor` (sees every response regardless of status) triggered on `403`, plus explicit retry-on-403 logic in `StompWebSocketClient.connect()` itself, since an `Authenticator`/`Interceptor` doesn't cleanly apply to a WebSocket upgrade request the same way. Also added: `ChatViewModel.sendPrompt()`/`decidePermission()` used to discard their `Result` entirely, so *any* failure failed completely silently — now shows a dismissible `ErrorBanner` and restores the typed prompt on failure |
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
