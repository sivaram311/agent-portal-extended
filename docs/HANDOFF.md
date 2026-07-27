# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.3.0-happy-path-dev` · versionCode **9** |
| Latest release | [`v0.3.0-happy-path-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.3.0-happy-path-dev) |
| APK SHA-256 | `91511BBB1B6F9528A7A5B290456AA4C356AC622C55173D2C79812F52DF57CE54` |
| Prior tip | [`v0.2.6-token-refresh-403-fix-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.6-token-refresh-403-fix-dev) |

## Happy path (v0.3.0)

Shipped supervisor loop:

1. Login (password)
2. Sessions list with filters: **All / Needs you / Running / Failed**
3. FAB → thin create (Cursor | Antigravity, workspace `demo`)
4. Chat with live stream
5. **Decision bottom sheet** — tool: Allow once / Always allow / Reject; plan: Accept / Reject
6. Needs-you banner when sheet dismissed
7. ⋮ → Cancel run / Archive
8. Notification tap → open that session’s Chat

## Device testing log

- **2026-07-27, `v0.2.3`–`v0.2.6`**: login/list/stream/token-refresh fixes (see prior handoff notes).
- **2026-07-28, `v0.3.0`**: happy-path UX shipped; **awaiting on-device pass** (Needs you filter, Decision sheet, archive, notification deep link).

## Now → next

| Now | Next |
|-----|------|
| Happy-path supervisor UI on device | Realme P2 Pro smoke: Needs you → Approve → stream → Archive; FCM arrival when ADB available; SSO promote still blocked on css-next |

Session: 2026-07-28.
