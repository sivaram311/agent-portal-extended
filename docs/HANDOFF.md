# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.8-oom-http-log-fix-dev` · versionCode **18** |
| APK SHA-256 | `EF6F3E79F5F8D2549F38049CBE5F5E99A39F7B702D086ECAFB7FB4EDE82BD759` |

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
