# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.5-token-refresh-fix-dev` · versionCode **7** |
| Latest release | TBD — filled in after release |
| APK SHA-256 | `85447E0339B099014AB439BED4DE49BF6E50E325996AD2B4D4D0952346768A71` |
| Prior tip | [`v0.2.4-chat-streaming-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.4-chat-streaming-dev) |

## Device testing log

- **2026-07-27, `v0.2.3`**: first real device pass (Realme P2 Pro) — launch, notification permission grant, password login, session list (real backend data) all confirmed working.
- **2026-07-27, `v0.2.4` in the field**: user reported "not responding" / "text not binding" after actually using chat for a while. Diagnosed via the backend API directly (not guesswork) — confirmed real prompt/response pairs existed server-side that the app never displayed, and confirmed no new prompts were reaching the server at all for the failing case. Root cause: access tokens expire in 15 minutes, there was no refresh logic anywhere, and `ChatViewModel.sendPrompt()`/`decidePermission()` discarded their `Result` entirely — any failure, expired token or otherwise, failed with zero visible indication.
- Fix (`v0.2.5`) not yet re-tested on the device — the failure took ~15+ minutes of real usage to reproduce last time, so it needs another extended real-usage session to confirm, not just a fresh install.

## Now → next

| Now | Next |
|-----|------|
| `TokenAuthenticator` (OkHttp `Authenticator`) refreshes the access token on a 401 via `POST {authUrl}/auth/refresh` and retries once — verified directly against the live server (real `200`, fresh access token, non-rotating refresh token handled correctly by `TokenStore.saveTokens`'s existing null-safe semantics). `ChatScreen` now has a dismissible `ErrorBanner` for any send/decide-permission failure, and a failed send restores the typed prompt instead of discarding it. Keyboard (`imePadding`) and real streaming (`assistant_delta`/`thinking_delta`) fixes from `v0.2.4` are included but still not device-verified. | Extended real-usage session on the Realme P2 Pro (well past 15 minutes) to confirm the token refresh actually fires transparently and chat keeps working instead of silently dying again |

Session: 2026-07-27.
