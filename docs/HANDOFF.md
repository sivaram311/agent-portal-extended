# Handoff — Agent Portal Extended (Android)

**Repo:** https://github.com/sivaram311/agent-portal-extended  
**Local:** `E:\MyWorkspace\agent-portal-extended`

## Current tip

| Field | Value |
|-------|-------|
| versionName | `0.4.1-tool-noise-fix-dev` · versionCode **11** |
| APK SHA-256 | `3692BA0750674E5CA64DE48F18908383C2F5BD44AC99549B0FDFB77A30475E44` |

## v0.4.1 — tool noise fix

- Turn-scoped activity (since last user message), not session-wide “176 tools”
- Categorize reads / edits / shells; primary chip hides pure reads
- Separate “Read N files” chip; timeline groups with reads collapsed by default
- Filter subagents, generic `tool`, `task-*.log`, abandoned; dedupe by toolCallId
- Backend (agent-portal): skip ACP tool updates without `toolCallId` (no random UUID inflation)

Session: 2026-07-28.
