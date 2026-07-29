# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.4-sso-session-fix-dev` · versionCode **14** |
| APK SHA-256 | `AB5BEAAB1C96A529B219FBDE429E9BEF26161C7E9839262E80B0C28C1AC3A622` |

## v0.4.4 — SSO open + false session expiry

- Android 11+ `<queries>` for Custom Tabs / https browsers so AppAuth can find a browser (SSO was silently not opening)
- Keep `AuthorizationService` until SSO completes; clearer “no browser” error; `openid` scope
- Token refresh: clear storage only when auth server returns 4xx (not on network/5xx blips)
- TokenAuthenticator / WS: do not wipe a still-valid JWT on failed refresh
- Soften 403 copy (no longer always “session expired”); app-lock copy clarifies token still saved
- TokenStore uses `commit()` for auth keys; proactive soft refresh near expiry on session list

## v0.4.3 — reconnect + logout

- Connection strip Manage → Reconnect / Sign out; Unknown → Signed in

## v0.4.2 — prompt timeout + auth status

- REST timeouts 5m/6m; STOMP keep-alive; auth status strip

Session: 2026-07-29.

