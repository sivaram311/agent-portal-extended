# Handoff â€” Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.3-reconnect-logout-dev` Â· versionCode **13** |
| APK SHA-256 | `7396FE2269950E545505A8B4AF55C5B4F21B7F565B5A326F74E21BE20D890539` |

## v0.4.3 â€” reconnect + logout

- Connection strip: tap **Manage** â†’ sheet with Reconnect + Sign out
- Reconnect refreshes access token (without wiping on failure) and force-reconnects STOMP on Chat
- Sign out clears tokens/WS and returns to Login
- â€œUnknownâ€ â†’ display **Signed in** (pre-v0.4.2 installs); red only for real auth failure
- JWT payload Base64 padding fixed; refresh label distinguishes missing vs ready

## v0.4.2 â€” prompt timeout + auth status

- REST OkHttp read/call timeouts raised (5m/6m)
- STOMP heartbeats + auto-reconnect; auth status strip

Session: 2026-07-29.

