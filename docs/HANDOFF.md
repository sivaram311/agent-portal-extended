# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.2-fcm-live-dev` · versionCode **4** |
| Latest release | [`v0.2.2-fcm-live-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.2-fcm-live-dev) |
| APK SHA-256 | `771B974122DF01CE4A2574A7F14BECBE47865E10371E683022FBD358F1DF82EA` |
| Prior tip | [`v0.2.1-fcm-android-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.1-fcm-android-dev) |

## Now → next

| Now | Next |
|-----|------|
| Firebase push is live end-to-end. Backend `PushNotificationService` initializes the Admin SDK from a service-account key at `E:\MyAgent\workflow\secrets\firebase-admin-agent-portal.json` (machine-local, never committed) and really sends — verified with a throwaway JUnit smoke test that got back a genuine `INVALID_ARGUMENT` FCM rejection for a deliberately-invalid token, proving real auth against Google's servers. Stale/unregistered tokens auto-delete from `device_tokens`. Found and fixed a real bug along the way: `SessionEventBus` only forwarded `input_required` (Antigravity's free-text nudge, no `permissionId`) to push, not `permission_required`/`plan_required` (Cursor's actual approvable-permission events) — fixed on both backend and Android. OAuth/PKCE SSO still not functional against the live server (blocked on a real `centralized-security-system` promote). Zero device/emulator verification. | Promote `centralized-security-system` DEV→PROD for SSO; sideload on the Realme P2 Pro — the first point a real push notification (and the whole app) can be observed working on an actual device |

Session: 2026-07-27.
