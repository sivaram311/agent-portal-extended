# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.7-diagnostics-dev` · versionCode **17** |
| APK SHA-256 | `0B12C6FDB9744810345413AB9BA926F4E258B8A3D7A92E697D584DC1D5B88D79` |

## v0.4.7 — Mobile diagnostics upload

- Manage → **Send diagnostics** uploads ring buffer + own logcat to `POST /api/diagnostics/client-logs`
- Uncaught crashes write `pending-crash.log` and upload on next launch
- Portal stores files under `logs/mobile-diagnostics/{date}/…` (admin list/download)

## v0.4.6 — Sub-agents + crash hardening

- Sub-agents sheet + Abandon; AppLockGate / STOMP / Markwon hardening

Session: 2026-07-30.
