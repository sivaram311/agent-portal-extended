# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.6-token-refresh-403-fix-dev` · versionCode **8** |
| Latest release | [`v0.2.6-token-refresh-403-fix-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.6-token-refresh-403-fix-dev) |
| APK SHA-256 | `709643FA2F00042A0E9BE1AEBABF81C41396425FCD50C63D85B8F9A13702CA82` |
| Prior tip | [`v0.2.5-token-refresh-fix-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.5-token-refresh-fix-dev) |

## Device testing log

- **2026-07-27, `v0.2.3`**: first real device pass (Realme P2 Pro) — launch, notification permission grant, password login, session list all confirmed working.
- **2026-07-27, `v0.2.4` in the field**: user reported "not responding" / "text not binding" after extended chat use. Diagnosed via the backend API directly — confirmed real prompt/response pairs existed server-side. Root cause identified as token expiry + silently-discarded failures. Shipped `v0.2.5`.
- **2026-07-27, `v0.2.5` in the field**: user reported realtime updates require leaving and re-entering the session ("not automatically refresh... I should go back and come"). Investigated and found the `v0.2.5` fix had a real, verifiable flaw: it used OkHttp's `Authenticator`, which only auto-fires on HTTP `401` — but `curl` against the live server confirmed this backend returns `403` for every auth failure (no token, invalid token, expired token; both `/api/**` and `/ws/websocket`), never `401`. The `v0.2.5` refresh logic was correct but never once invoked. Fixed properly in `v0.2.6`.
- `v0.2.6`'s first draft was caught by Reviewer **NO-GO** before it ever shipped: the WebSocket retry's recursive `connectInternal()` call hit its own reentrancy guard (state was still `CONNECTING`, never reset before retrying) and would have silently stuck at `CONNECTING` forever — the exact same "stops working, no error" symptom this fix exists to solve, just reproduced a different way. Fixed (reset state to `DISCONNECTED` before the retry call) and re-reviewed before push.
- `v0.2.6` (post-fix) not yet tested on-device — needs the same extended-usage scenario (15+ minutes, then check whether chat/realtime keeps working transparently) that surfaced the original bug.

## Now → next

| Now | Next |
|-----|------|
| Token refresh is now triggered correctly: a plain OkHttp `Interceptor` (`TokenAuthenticator.kt`, despite the class name — kept for history, it's no longer an `Authenticator`) retries any `403` REST response once after a refresh, and `StompWebSocketClient.connect()` does the same directly for the WebSocket handshake, which doesn't go through the interceptor retry path the same way. Both paths share `TokenRefresher.tryRefresh()`. Verified piece-by-piece against the live server before shipping: WS handshake with a valid token → `101`; without one → `403`; `POST /auth/refresh` → `200` with a fresh token. | Extended real-usage session on the Realme P2 Pro, specifically watching whether a chat session left open past 15 minutes keeps receiving realtime updates without needing to leave and re-enter |

Session: 2026-07-27.
