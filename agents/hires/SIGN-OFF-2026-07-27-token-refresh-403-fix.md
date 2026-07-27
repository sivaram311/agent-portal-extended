# SIGN-OFF — agent-portal-extended main (commit 154e5a7)

| Field | Value |
|-------|-------|
| Session | agent-portal-extended-token-refresh-403-fix-2026-07-27 |
| Reviewer agent id | reviewer-token-refresh-403-fix-1 |
| Provider | claude-code |
| Tip SHA | 154e5a7ce8ec34ecb72c2ca16ab47e36fa337c7d |
| Branch / tag | main (no tag on this commit; versionName bumped to 0.2.6-token-refresh-403-fix-dev in build.gradle.kts) |
| When (UTC+5:30) | 2026-07-27 |

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — README.md, ROADMAP.md, docs/HANDOFF.md all updated in this commit
- [x] No secrets in commit — scanned full diff, only field-name references, no key/token material
- [x] Fleet splits OK — n/a, no fleet/AV-upgrade dimension touched
- [ ] DEV E2E green if this push includes a release tag (#16) — n/a, not tagged
- [x] Login E2E used DEV public domain when host exists (#18) or waive documented — n/a to this push; this review independently exercised login/refresh/WS-handshake against the live public host (`https://delena.buzz`) via curl as part of verifying the commit's central claim (see below), not a formal Playwright E2E run
- [x] Tag ≠ live understood (matrix not falsely bumped) — no tag pushed, no DEPENDENCY-MATRIX claim made

## Verdict

**NO-GO**

### Findings

**1. The commit's central premise (403, not 401) is independently confirmed true — my own curl tests against the live server:**

| Test | Commit claims | My curl result |
|------|---------------|-----------------|
| `GET /api/sessions`, no auth header | 403 | **403** |
| `GET /api/sessions`, `Authorization: Bearer garbage` | 403 | **403** |
| WS handshake at `/ws/websocket`, no token | 403 | **403** |
| WS handshake at `/ws/websocket?access_token=<valid>` | 101 | **101 Switching Protocols** |
| `POST /auth/refresh` with a real refresh token + clientId | 200 + fresh accessToken | **200**, fresh `accessToken` returned, `refreshToken: null` (non-rotating, as `TokenStore`/`TokenRefresher` already assume) |

(Login itself: `POST /auth/login` with `demo`/`demo123`/`agent-portal` returned a transient `401 Invalid credentials or unauthorized for client` on the very first attempt of this session — immediately followed by a clean `200` on retry with the identical body, and every subsequent login/refresh call succeeded. Treated as environment noise, not evidence against the commit's claim, since every 403-vs-401 assertion above was independently reproduced afterward.)

So: the commit's stated root cause is real, not a plausible-sounding guess. **This is not the same class of miss as v0.2.5.**

**2. `TokenAuthenticator.kt` (REST interceptor) — correct, no loop, no leak.** Confirmed it is now `class TokenAuthenticator(...) : Interceptor` (no longer `Authenticator`), and `NetworkModule.provideOkHttpClient` wires it via `.addInterceptor(...)`, not `.authenticator(...)` — the signature change is real and actually wired in, not just a doc comment. Triggers only on `response.code == 403 && tokenStore.getRefreshToken() != null`. Calls `response.close()` before refreshing — no body leak. On refresh failure, returns `chain.proceed(request)` (a fresh, real response) rather than throwing or returning something unsafe. On the infinite-loop question specifically: this interceptor is registered once via `addInterceptor` (an application interceptor). Its own retry (`chain.proceed(retried)`) proceeds *forward* to the next interceptor in the chain (logging → network) — OkHttp's chain model does not re-invoke this same interceptor from the top on that call. So even if the retried request also comes back 403, it is structurally impossible for this to loop: `intercept()` only ever executes once per original outer request, and the second 403 is simply returned as-is to the caller. No `responseCount`-style guard is needed here (unlike the old `Authenticator`), and its absence is not a bug.

**3. `StompWebSocketClient.kt` — genuine bug, the retry-on-403 path never actually executes.** This is the blocking finding.

`connectInternal(allowAuthRetry: Boolean)` opens with the exact same reentrancy guard that existed in the pre-commit `connect()` (moved verbatim, per the diff — `@@ -57,6 +57,10 @@` through `@@ -64,18 +68,7 @@`):

```kotlin
private fun connectInternal(allowAuthRetry: Boolean) {
    if (_connectionState.value == ConnectionState.CONNECTING ||
        _connectionState.value == ConnectionState.CONNECTED
    ) {
        return
    }
    _connectionState.value = ConnectionState.CONNECTING
    ...
}
```

Trace the actual runtime sequence for the scenario this commit exists to fix (stale token at `connect()` time):

1. `connect()` → `connectInternal(allowAuthRetry = true)`. State is not CONNECTING/CONNECTED yet, so it proceeds and sets `_connectionState.value = CONNECTING`, then calls `okHttpClient.newWebSocket(...)`.
2. The handshake fails with 403 (asynchronously, on OkHttp's dispatcher thread). `onFailure` fires. **At this point `_connectionState.value` is still `CONNECTING`** — nothing in between set it to anything else.
3. `onFailure` sees `allowAuthRetry && response?.code == 403 && refreshToken != null` → true, calls `TokenRefresher.tryRefresh(tokenStore)` → succeeds, then calls `connectInternal(allowAuthRetry = false)`.
4. Inside that call, the guard at the top checks `_connectionState.value == CONNECTING` — **which is still true from step 1** — and returns immediately. No new `okHttpClient.newWebSocket()` call is ever made. The refreshed token is never used to actually reconnect.
5. `onFailure` then returns (the `return` after the `connectInternal` call in the `if (refreshed)` branch), **without ever reaching the `_connectionState.value = ConnectionState.FAILED` line.**

Net effect: `_connectionState` is left stuck at `CONNECTING` permanently. No `FAILED`, no `CONNECTED`, no `DISCONNECTED` — a silent hang in an intermediate state, with no further code path to recover it (a caller watching `connectionState` sees it freeze at CONNECTING forever). This is exactly the failure mode `README.md`/`docs/HANDOFF.md` describe as the original bug ("stops working... with zero error shown"), now reproduced by the fix itself on the WebSocket path specifically — the one path the commit message calls out as the direct explanation for the user's field report. The REST-side interceptor fix (finding #2) is solid and would work; the WebSocket-side fix, which is the one actually responsible for the reported symptom, is dead code in the one case it exists to handle.

This is not the "infinite loop" failure mode the task asked me to rule out, but it is squarely the same category the task called out explicitly for the WebSocket client ("no silent hang in an intermediate state") — and it means the second refresh attempt this commit's entire message is built around never runs.

**4. `TokenRefresher.kt` — functionally identical to the `v0.2.5` `TokenAuthenticator.refreshSync`/`authenticate` refresh logic, just relocated.** Same `RefreshTokenRequest(refreshToken, clientId)` shape, same bare-`HttpURLConnection` call (still bypasses the app's own OkHttp client, avoiding recursion), same null-safe non-rotating-refresh-token handling via `TokenStore.saveTokens`, same `tokenStore.clear()` on failure. No logic drift from what was reviewed and approved in the `v0.2.5` diff.

**5. Scope and docs are clean.** Diff touches exactly `TokenRefresher.kt` (new), `TokenAuthenticator.kt`, `StompWebSocketClient.kt`, `NetworkModule.kt`, `android/app/build.gradle.kts` (version bump 7→8, `0.2.5-token-refresh-fix-dev`→`0.2.6-token-refresh-403-fix-dev`), and `README.md`/`ROADMAP.md`/`docs/HANDOFF.md`. Nothing unrelated. `README.md`/`ROADMAP.md`/`docs/HANDOFF.md` all honestly state `v0.2.5` never actually fired and that `v0.2.6` is not yet device-verified — no overclaiming. No secrets anywhere in the diff.

### Why NO-GO despite the 403 diagnosis being correct

The diagnostic work (curl-verified 403-vs-401) is real and good, and the REST-side interceptor fix genuinely works. But the WebSocket-side retry — the specific code path whose failure produced the field report this commit is responding to — has a reentrancy-guard bug that makes the refresh-and-reconnect attempt a guaranteed no-op, leaving `ConnectionState` stuck at `CONNECTING` indefinitely. Shipping this would very likely reproduce the same user-visible symptom ("stops working, no error, has to leave and re-enter the screen") on the exact path it was meant to fix. Recommend: reset/bypass the `CONNECTING` guard for the internal retry call (e.g. set `_connectionState.value = ConnectionState.DISCONNECTED` before the `connectInternal(allowAuthRetry = false)` call in `onFailure`, or restructure `connectInternal` so the guard only applies to externally-initiated `connect()` calls, not the internal retry), then re-verify on-device per the extended-usage scenario already planned in `docs/HANDOFF.md`, before this is re-submitted for push.
