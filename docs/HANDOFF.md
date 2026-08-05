# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `1.1.0` · versionCode **20** |
| APK | `foreman-1.1.0-debug.apk` — published as the `v1.1.0` GitHub release asset, since `*.apk` is gitignored |
| APK SHA-256 | `C44555A42620519754B02FEF50391936C7CC80AAB9B2021F5C59B4DB39777C89` |

## v1.1.0 — Offline prompt queue + workspace picker

**Offline queue.** `sendPrompt` no longer restores the draft and shows a banner when the network is
down. Every prompt is written to a new Room table (`pending_prompts`, AppDatabase v2, destructive
fallback) before the request goes out, so the bubble renders immediately as `Queued` and survives a
process restart. `SessionRepository` classifies the failure: `IOException` or 5xx keeps the row
queued, anything else (4xx, parse errors) deletes it and surfaces the existing error banner.
A `ConnectivityObserver` (`ConnectivityManager.NetworkCallback`, waits for `NET_CAPABILITY_VALIDATED`)
flushes the session's queue FIFO on reconnect; a shared mutex keeps that flush from racing the
in-flight direct send. Five failed attempts mark the row `FAILED`, which renders as
`Failed – tap to retry`. A flush stops at the first still-retryable prompt so later prompts cannot
overtake it, but parks a permanently-rejected one as `FAILED` and carries on. The STOMP
`assistant_delta` streaming path is untouched.

**Workspace picker.** `CreateSessionSheet` no longer hard-codes `demo`. It shows `demo` plus the last
8 workspace paths used on this device (DataStore Preferences, `workspace_preferences`) as chips, with
a collapsible free-text path field. The chosen path is recorded only after the create call succeeds.
No backend endpoint was invented — if `GET /api/workspaces` is added later, merge it into the same
chip list.

**Known gap:** no ADB on this build host, so this ships on build + review evidence under the
CONSCIOUS #16 waiver recorded in `ROADMAP.md`. The queue transitions have not been exercised on a
real device.

## v0.4.8 — OOM crash fix (diagnostics-proven)

Crash dump `admin_…_crash.log` (Realme, 0.4.7):  
`OutOfMemoryError` allocating **~45MB** inside `HttpLoggingInterceptor` (BODY) while buffering  
`GET /api/sessions/{id}/tools` (**45,107,253 bytes**, 420 tool runs).

Fixes:
- Debug OkHttp logging: **HEADERS** only (never BODY); redact Authorization
- Request tools with `?compact=true` (backend truncates args/output)
- Cap tool `output` kept in Compose state

## v0.4.7 — Mobile diagnostics upload

Manage → Send diagnostics; crash pending upload on next launch.
