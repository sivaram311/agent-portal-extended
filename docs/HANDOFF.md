# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended (public)
**Local:** `E:\MyWorkspace\agent-portal-extended`
**Device SoT:** `E:\MyAgent\workflow\devices\REALME-P2-PRO.md`
**Standing rules:** `E:\MyAgent\workflow\CONSCIOUS.md`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.2.4-chat-streaming-dev` · versionCode **6** |
| Latest release | TBD — filled in after release |
| APK SHA-256 | `EC440B7D1E537355421CA1DA8161BCD8966F0F23EB44E2ADACDA33CFD1B9EDE9` |
| Prior tip | [`v0.2.3-docs-hotfix-dev`](https://github.com/sivaram311/agent-portal-extended/releases/tag/v0.2.3-docs-hotfix-dev) |

## Device testing (new as of 2026-07-27)

First-ever real device pass, Realme P2 Pro (via `v0.2.3-docs-hotfix-dev`): app launched, notification permission prompt appeared and was granted, password-lane login succeeded, session list rendered the real backend data (2 IDLE + 1 CANCELLED session) correctly. Chat screen, biometric re-lock on background/foreground, and the new streaming/keyboard fixes below have not yet been re-confirmed on-device against this specific build.

## Now → next

| Now | Next |
|-----|------|
| Two real bugs found and fixed, both surfaced by a user bug report (written in Flutter terms against a Kotlin/Compose app — translated the underlying issues, verified which applied here before fixing): (1) chat input bar had no `imePadding()`, so the keyboard could cover the text field on newer Android with edge-to-edge enabled; (2) `ChatViewModel` was doing generic "refetch on any STOMP event" instead of parsing the backend's real per-token streaming events (`assistant_delta`/`thinking_delta`) — now appends live text as it streams, same source the web frontend already uses. Neither fix has been device-verified yet. | Sideload `v0.2.4` on the Realme P2 Pro and actually type in the chat input with the keyboard open, and send a prompt to watch text stream in live — first real confirmation either fix works as intended |

Session: 2026-07-27.
