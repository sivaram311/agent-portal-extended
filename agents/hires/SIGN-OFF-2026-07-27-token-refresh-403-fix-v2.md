# SIGN-OFF — agent-portal-extended main (commit 05901b6, re-review after NO-GO)

| Field | Value |
|-------|-------|
| Session | agent-portal-extended-token-refresh-403-fix-2026-07-27 |
| Reviewer agent id | reviewer-token-refresh-403-fix-v2 |
| Provider | claude-code |
| Tip SHA | 05901b6b13746b2978a7943d29c41469fd2fb1c0 |
| Branch / tag | main (2 unpushed local commits: 154e5a7, 05901b6; no tag on either) |
| When (UTC+5:30) | 2026-07-27 |

**Context**: re-review after a real NO-GO. The prior reviewer
(`agents/hires/SIGN-OFF-2026-07-27-token-refresh-403-fix.md`, archived in
`05901b6`) confirmed the 403-vs-401 diagnosis in `154e5a7` was correct via
live `curl` tests against `https://delena.buzz`, but found the WebSocket
retry's recursive `connectInternal(allowAuthRetry = false)` call hit its own
reentrancy guard (state still `CONNECTING` from the first attempt, never
reset) and silently no-op'd, leaving `_connectionState` stuck at `CONNECTING`
forever. `05901b6` is the fix: `_connectionState.value =
ConnectionState.DISCONNECTED` is set immediately before the recursive call.
This review independently re-traces the full retry path end to end rather
than assuming the fix is sufficient just because the diagnosis was correct.

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — `docs/HANDOFF.md` updated in `05901b6` to record the NO-GO/fix cycle
- [x] No secrets in commit — diff is code + a review sign-off doc + a docs line; no key/token material
- [x] Fleet splits OK — n/a, no fleet/AV-upgrade dimension touched
- [ ] DEV E2E green if this push includes a release tag (#16) — n/a, not tagged
- [x] Login E2E used DEV public domain when host exists (#18) or waive documented — waived for this review; verified by static trace of the retry state machine plus a real Gradle build (`assembleDebug`/`lintDebug`), not a live device/E2E run against `delena.buzz`. The prior reviewer already did the live curl work confirming 403-vs-401; this review's job was the WebSocket state-machine correctness of the fix itself, which a build/trace verifies more precisely than another curl pass would.
- [x] Tag ≠ live understood (matrix not falsely bumped) — no tag pushed, no DEPENDENCY-MATRIX claim made

## Verdict

**GO**

### Findings

**1. Full current file read** — `StompWebSocketClient.kt` read in its entirety (227 lines), not just the diff hunk, to check the fixed function in the context of the whole retry/state-machine design.

**2. Traced sequence for a 403 WebSocket handshake failure, step by step:**

1. `connect()` → `connectInternal(allowAuthRetry = true)`.
2. Guard: state is `DISCONNECTED` (initial value of the `MutableStateFlow`) → passes.
3. `_connectionState.value = CONNECTING`, then `okHttpClient.newWebSocket(...)`.
4. Handshake fails 403 → `onFailure` fires (OkHttp dispatcher thread). At this point state is still `CONNECTING`.
5. `allowAuthRetry && response?.code == 403 && tokenStore.getRefreshToken() != null` → true. `TokenRefresher.tryRefresh(tokenStore)` runs synchronously (blocking `HttpURLConnection` call, not routed back through the intercepted OkHttp client — confirmed in `TokenRefresher.kt`, avoids recursion).
6. On success: **the fixed line**, `_connectionState.value = ConnectionState.DISCONNECTED`, executes unconditionally right before the recursive call — not after it, not gated by any other condition. Confirmed by reading lines 101–121 in context.
7. `connectInternal(allowAuthRetry = false)` runs: guard now sees `DISCONNECTED` → passes. No other guard exists in this function — sets `CONNECTING` again and issues a genuinely new `okHttpClient.newWebSocket(...)` call. This is the concrete gap the prior NO-GO identified, and it is now closed: the refreshed token is actually used for a second real handshake attempt.
8. If this second attempt also fails (still-bad token, network error, whatever): `onFailure` fires again, but `allowAuthRetry` is `false` this time, so the whole `if` block at line 101 is skipped and execution falls straight to `_connectionState.value = ConnectionState.FAILED` (line 120). No remaining path loops back into another retry.
9. If it succeeds (101 handshake): `onOpen` sends the STOMP `CONNECT` frame; state moves to `CONNECTED` only once a STOMP `CONNECTED` frame is parsed and dispatched (line 181) — a pre-existing design point, unchanged by this fix, and not a new stuck-state risk introduced here (see note below).

Every path from the 403 branch terminates in either a genuine second connection attempt (→ eventually `CONNECTED` or `FAILED`) or directly in `FAILED`. No path re-enters the guard with a state that blocks it. **The specific bug the prior reviewer found is closed.**

**3. Race-condition check (state observers during the transient `DISCONNECTED` window)**: `ChatViewModel.kt` line 74 does `stompClient.connectionState.collect { connectionState -> if (connectionState == ConnectionState.CONNECTED) { ... subscribeToSession ... } }` — it branches **only** on `CONNECTED`, never on `DISCONNECTED` or `CONNECTING`. `ChatViewModel` calls `stompClient.connect()` exactly once, in `init` (line 72); nothing else in the codebase calls `.connect()` on `stompClient` (grepped the full `android/` tree — only call sites are `ChatViewModel.kt:72` and the `connectInternal` recursion itself). So the transient `DISCONNECTED` value set immediately before the recursive `connectInternal` call has no observer that reacts to it — not a redundant-`connect()`-call hazard, and not a thread-safety hazard either: `MutableStateFlow.value =` is a synchronous atomic write: the collector coroutine is dispatched to independently and cannot itself call back into `connectInternal` synchronously on this thread before the recursive call executes. **Non-issue, confirmed rather than assumed.**

**4. Old failed `WebSocket` object**: the first (failed) `newWebSocket()` call's object is discarded when `webSocket = okHttpClient.newWebSocket(...)` is reassigned in the recursive call. OkHttp `WebSocket` instances that fail the handshake (never reach `onOpen`) hold no open socket/thread resources requiring an explicit `.close()` — `onFailure` is OkHttp's own terminal signal that the object is already done. **Confirmed non-issue**, not a leak.

**5. Build verification** — ran directly rather than trusting the commit message:
```
cd E:\MyWorkspace\agent-portal-extended\android
.\gradlew.bat :app:assembleDebug :app:lintDebug
```
Result: `BUILD SUCCESSFUL in 4s`, 50 actionable tasks (1 executed, 49 up-to-date — the tree was already built from this exact source). No compile errors, `lintDebug` task completed with no failure.

**6. Prior NO-GO sign-off integrity**: diffed the working-tree copy of `agents/hires/SIGN-OFF-2026-07-27-token-refresh-403-fix.md` against `git show 05901b6:agents/hires/SIGN-OFF-2026-07-27-token-refresh-403-fix.md` — byte-identical. The archived NO-GO is the real, unedited prior findings (including its own live-curl verification table and its explicit NO-GO verdict and root-cause trace) — not softened or altered when committed.

### Non-blocking observations (not introduced by this fix, out of scope for GO/NO-GO here, worth a future look)

- `stompClient` is a single instance held for the app's lifetime on `AppContainer` (not per-`ChatViewModel`). If a `ChatViewModel` is cleared (`disconnect()`, sets `DISCONNECTED`, nulls `webSocket`) while an old `newWebSocket()` handshake or retry from a *previous* session is still in flight on OkHttp's dispatcher thread, a late `onFailure`/`onOpen` callback from that stale attempt could still touch shared state. This pattern predates `05901b6` (present since `154e5a7`) and is not part of the bug that was reviewed here.
- If a WebSocket handshake succeeds (101) but the server never sends a STOMP `CONNECTED` frame back, state stays at `CONNECTING` indefinitely — this is pre-existing design (state only advances to `CONNECTED` on `dispatch()` parsing a `CONNECTED` frame, line 181) and is unrelated to the 403 retry path this fix addresses.

Neither observation is a regression from `05901b6`; both predate it and are outside this review's scope.

### Conclusion

The fix in `05901b6` closes the exact gap the prior reviewer found: the recursive retry call now actually executes a second real handshake attempt instead of no-op'ing against its own reentrancy guard. The full retry sequence, traced end to end, always terminates in either `CONNECTED` (via a later STOMP `CONNECTED` frame) or `FAILED` — never stuck at `CONNECTING` or `DISCONNECTED` indefinitely. No new race condition or resource leak was introduced by the fix. Build passes for real. Prior NO-GO sign-off is genuine and unedited.

**GO** to push `154e5a7` and `05901b6`.
