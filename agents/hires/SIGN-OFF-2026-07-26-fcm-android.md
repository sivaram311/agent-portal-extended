# SIGN-OFF — agent-portal-extended main (commit 0f2619b)

| Field | Value |
|-------|-------|
| Session | agent-portal-extended-fcm-android-2026-07-26 |
| Reviewer agent id | reviewer-fcm-android-1 |
| Provider | claude-code |
| Tip SHA | 0f2619b65dd5e088662b6e7cb91de3e0939f9c56 |
| Branch / tag | main (no tag being pushed) |
| When (UTC+5:30) | 2026-07-26 |

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — README.md, ROADMAP.md, docs/HANDOFF.md all updated in this commit
- [x] No secrets in commit — `android/app/google-services.json` is NOT part of this commit and is NOT tracked (see findings)
- [x] Fleet splits OK — N/A, no fleet/AV split touched; single-app Android client change
- [ ] DEV E2E green if this push includes a release tag (#16) — N/A, this push is a branch commit, not a tag; `versionName` was bumped to `0.2.1-fcm-android-dev` in source but no corresponding git tag is being pushed in this action
- [ ] Login E2E used DEV public domain when host exists (#18) — N/A, no login/auth flow touched by this commit
- [x] Tag ≠ live understood — N/A, no tag involved; matrix not touched

## Verdict

**GO**

### Findings

1. **google-services.json is genuinely untracked, and the .gitignore fix is real and correct.** Verified directly:
   - `git show 0f2619b --stat` does not list `android/app/google-services.json` among the 8 changed files.
   - `git ls-files | grep google-services` returns nothing — the file has never been tracked.
   - The old `.gitignore` line `android/google-services.json` is an anchored pattern that only matches a file at exactly that path, and would NOT have matched the real file location `android/app/google-services.json`. This commit changes it to `android/**/google-services.json`.
   - `git check-ignore -v android/app/google-services.json` confirms a match against the new pattern: `.gitignore:9:android/**/google-services.json  android/app/google-services.json`.
   - The file genuinely exists on disk at `android/app/google-services.json` (680 bytes, present), so this is not a moot fix for a nonexistent file — the fix closes a real gap that would otherwise have let the file be committed on the next `git add .`.
   - Per the reviewer's brief, the API key in `google-services.json` is Google's own designed-to-be-public identifier (any decompiled APK reveals it), not a bearer secret — so even had this fix been absent, the exposure class is hygiene, not a credential leak. Not a factor in the verdict either way since the file is confirmed untracked.

2. **Android wiring in the diff is standard and correctly scoped, no issues found.**
   - `android/build.gradle.kts`: `com.google.gms.google-services` plugin declared at root with `version "4.4.2" apply false` (correct root-module pattern, matches sibling plugin declarations already in the file).
   - `android/app/build.gradle.kts`: same plugin id applied (without version) in the app module — correct placement, `versionCode` 2→3 and `versionName` bumped to `0.2.1-fcm-android-dev` consistently.
   - `AndroidManifest.xml`: new `<service android:name=".push.AgentPortalFirebaseMessagingService" android:exported="false">` — `exported="false"` is present and correct (not exported to other apps), intent-filter action is exactly `com.google.firebase.MESSAGING_EVENT` as expected for FCM.
   - `AgentPortalFirebaseMessagingService.kt` change is doc-comment only (updates the class-level KDoc to reflect it's now live/registered instead of dormant) — no logic changes, consistent with the class body being unchanged in the diff.
   - Commit is scoped exactly as expected: `.gitignore` fix, plugin wiring (root + app module), manifest service registration, one doc-comment update, README/ROADMAP/HANDOFF doc updates, and the version bump. No unrelated files or changes present.
   - Doc updates in README.md, ROADMAP.md, and docs/HANDOFF.md accurately describe the state as "Android side wired" while explicitly and repeatedly flagging "Backend send is still log-only" pending a Firebase Admin SDK service-account key — no overclaiming that push notifications work end-to-end.

No blocking issues. Commit is clean, correctly scoped, and the one real bug caught (gitignore anchoring) is genuinely fixed and verified. GO to push commit 0f2619b to main.
