# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.1-fcm-android-dev` · versionCode **3** |
| Latest release | TBD — filled in after release |
| APK SHA-256 | TBD — filled in after release |
| Prior tip | [`v0.2.0-auth-push-lock-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.0-auth-push-lock-dev) |

## Now → next

| Now | Next |
|-----|------|
| Firebase project provisioned (`my-aadlc-proj`); Android side fully wired (`google-services.json`, `google-services` plugin, `AgentPortalFirebaseMessagingService` registered and receiving). Backend send is still log-only — needs a Firebase Admin SDK service-account key (separate credential). OAuth/PKCE SSO still not functional against the live server (blocked on a real `centralized-security-system` promote). Zero device/emulator verification. | Get the Admin SDK service-account key and wire real backend sending; promote `centralized-security-system` DEV→PROD for SSO; sideload on the Realme P2 Pro for the first real device pass |

Session: 2026-07-26.
