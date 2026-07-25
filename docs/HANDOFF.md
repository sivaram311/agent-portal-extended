# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.0-auth-push-lock-dev` · versionCode **2** |
| Latest release | [`v0.2.0-auth-push-lock-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.0-auth-push-lock-dev) |
| APK SHA-256 | `A29CA32C84F80FD362147420C3CD1EEC95F762B726BA97D5D12B88494D81E5B8` |
| Prior tip | [`v0.1.0-skeleton-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.1.0-skeleton-dev) |

## Now → next

| Now | Next |
|-----|------|
| Biometric lock, notification-action permission approval, and backend device-token push infra all built and verified end-to-end on live DEV (login → JWT → authenticated API calls, `/api/devices` register/unregister). OAuth/PKCE SSO is fully coded but **not functional yet** — its one backend dependency (`centralized-security-system`'s redirect-allowlist fix) exists only in local DEV source; the live auth server runs a separate PROD jar (`G:\apps\css-next\`) that doesn't have it. Zero device/emulator verification (no ADB on this build host). | Promote `centralized-security-system` DEV→PROD (Q1/Q2, evidence + EM GO) to make SSO functional; provision a Firebase project + `google-services.json` to activate push; sideload on the Realme P2 Pro for the first real device pass |

Session: 2026-07-26.
