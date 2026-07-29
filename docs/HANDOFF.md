# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.5-android-rate-limit-exempt-dev` · versionCode **15** |
| APK SHA-256 | `E187C7257FED689F82D3046C17A47EF0012AC2F0BEC1C801D092F0F9E16B365D` |

## v0.4.5 — Android rate-limit exempt

- Sends `X-Agent-Portal-Client: android` on REST calls
- Agent Portal DEV RateLimitFilter exempts that header (unlimited for the phone app)
- Web / other clients still limited (120/min on DEV)

## v0.4.4 — SSO open + false session expiry

- Android 11+ `<queries>` for Custom Tabs; soft token refresh clear policy

Session: 2026-07-29.
