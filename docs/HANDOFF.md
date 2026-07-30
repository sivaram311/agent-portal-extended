# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.6-subagents-crash-fix-dev` · versionCode **16** |
| APK SHA-256 | ``917E8A91ABE0F257579F902793855993BDF2C05A76C404A1E3A4E702E173041E`` |

## v0.4.6 — Sub-agents + crash hardening

- Sub-agents sheet (⋮ → Sub-agents; amber chip under chat): list active/finished, **Abandon** via `POST /api/sessions/{id}/subagents/{subId}/abandon`
- STOMP `subagent_*` events refresh the list
- Crash/close hardening: safe AppLockGate (no hard cast, lifecycle-gated biometric, overlay), safe JWT/STOMP JSON reads, Markwon try/catch, STOMP acquire/release

## v0.4.5 — Android rate-limit exempt

- `X-Agent-Portal-Client: android` header; portal RateLimitFilter exemption

## Ops note — cleartext localhost

Restart DEV with `agent-portal/scripts/start-dev-backend.ps1` if auth config falls back to `http://localhost:9000`.

Session: 2026-07-30.

