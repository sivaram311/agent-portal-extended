# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.2-prompt-timeout-auth-status-dev` · versionCode **12** |
| APK SHA-256 | `4182E9F1A4670AB47462DFDB9A6D3C0FAF8AA2068EEE73136F89D59D451E22FF` |

## v0.4.2 — prompt timeout + auth status

- REST OkHttp read/call timeouts raised (5m/6m) so `/prompt` ACP handshake no longer dies at 10s → nginx 499
- Dedicated WebSocket OkHttp client (readTimeout 0 + pingInterval) + STOMP heartbeats + auto-reconnect
- Chat re-subscribes via `flatMapLatest` on CONNECTED; Room refresh no longer wipes live streaming buffer
- User-facing errors prefer backend `error` body (e.g. “Session already has an active run”)
- Connection status strip on Sessions + Chat: Password/SSO, subject, token TTL, refresh, auth host, Live/Connecting/Offline (tap to expand)

Session: 2026-07-29.
