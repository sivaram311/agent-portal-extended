# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.3-docs-hotfix-dev` · versionCode **5** |
| Latest release | [`v0.2.3-docs-hotfix-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.3-docs-hotfix-dev) |
| APK SHA-256 | `BD91C9E5744D6BED2F9E58C970DF08E900CBF91C39D7C5F6B23920C66A974F3A` |
| Prior tip | [`v0.2.2-fcm-live-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.2-fcm-live-dev) |

## Now → next

| Now | Next |
|-----|------|
| Docs-only hotfix (release-link fill-in from the previous ship) + version bump — no app-code change since `v0.2.2-fcm-live-dev`. **Pushed on explicit user instruction without the standing Reviewer-GO gate** (CONSCIOUS.md #17 normally requires one before any push) — logged here and in ACTIVITY-LOG.md for the audit trail. Firebase push, biometric lock, notification-action approval all remain as verified in `v0.2.2`. OAuth/PKCE SSO still blocked on a `centralized-security-system` promote. Zero device/emulator verification. | Promote `centralized-security-system` DEV→PROD for SSO; sideload on the Realme P2 Pro — the first point a real push notification (and the whole app) can be observed working on an actual device |

Session: 2026-07-27.
