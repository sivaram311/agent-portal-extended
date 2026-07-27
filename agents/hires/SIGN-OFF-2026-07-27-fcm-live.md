# SIGN-OFF — agent-portal + agent-portal-extended (FCM live-send pair) `main`

| Field | Value |
|-------|-------|
| Session | agent-portal-extended-fcm-live-2026-07-27 |
| Reviewer agent id | reviewer-fcm-live-1 |
| Provider | claude-code |
| Repo 1 | `E:\MyWorkspace\agent-portal` |
| Tip SHA (repo 1) | `301e326cb0b8c41b64ce0fbcbb7240410cf4da88` |
| Branch | main |
| Repo 2 | `E:\MyWorkspace\agent-portal-extended` |
| Tip SHA (repo 2) | `5b693866cc7731687713dfc36874244cd0b85a85` |
| Branch / tag | main (message references `v0.2.2-fcm-live-dev`, no tag pushed by this commit) |
| When (UTC+5:30) | 2026-07-27 |

## Checklist

- [x] Docs updated same turn (CONSCIOUS #12) — `docs/OPS.md` (repo 1); `README.md`, `ROADMAP.md`, `docs/HANDOFF.md` (repo 2)
- [x] No secrets in commit — verified both diffs, `git status`, and both repos' `.gitignore`
- [x] Fleet splits OK — N/A, no CSS/AV-classic split touched by this change
- [ ] DEV E2E green — N/A, no release tag included in either commit (backend `mvn compile` + Android `assembleDebug`/`lintDebug` clean; no live device/emulator push delivery verified — correctly disclosed as such in both repos' docs)
- [ ] Login E2E — N/A, no auth-surface change in this pair
- [x] Tag ≠ live understood — repo 2 commit message references `v0.2.2-fcm-live-dev` as a version string only; no tag object was created/pushed by either commit, matrix not implicated

## Verdict

**GO** (both repos)

## Per-repo detail

### Repo 1 — agent-portal (`301e326`)

1. **No secrets** — confirmed. `E:\MyAgent\workflow\secrets\firebase-admin-agent-portal.json` appears only as a path string in `scripts/start-dev-backend.ps1` (guarded by `Test-Path`, never echoed) and in `docs/OPS.md` prose. Grepped the full diff for `BEGIN PRIVATE KEY`, `private_key`, `client_email`, `"type": "service_account"` — zero hits. `git status` is clean, confirming nothing beyond this diff is staged.
2. **Fail-safe init** — `PushNotificationService.init()` (`@PostConstruct`) returns early (log only, no throw) when `credentialsPath` is blank or the file doesn't exist (`Files.isRegularFile`); `IOException` from `GoogleCredentials.fromStream` is caught and logged, not propagated. `FirebaseApp.initializeApp` and the credentials read are local/synchronous, no network call at boot — Spring Boot startup cannot be blocked or failed by this path regardless of Firebase reachability.
3. **SessionEventBus** — read the full post-change method (not just the commit message). `isWebhookEvent` is still exactly the original 4 types; `webhookService.publish` is called **only** inside `if (isWebhookEvent)`, unchanged trigger set and payload. `isPushEvent` (`= isWebhookEvent || permission_required || plan_required`) gates `pushNotificationService.notifyOwner`, which now additionally fires for the two new types. Verified additive-only, no regression to the external webhook contract.
4. **start-dev-backend.ps1** — new lines only do `if (Test-Path $firebaseCreds) { $env:FIREBASE_CREDENTIALS_PATH = $firebaseCreds }`; no logging/echo of the value or file contents.
5. **pom.xml** — full file read; exactly one dependency block added (`com.google.firebase:firebase-admin:9.4.3`), nothing else in the file touched. `mvn compile` was reported clean (context), consistent with no visible conflict.
6. **docs/OPS.md** — accurately describes the live-send behavior and the event-routing fix; does not claim device-level delivery verification anywhere in the diff.

### Repo 2 — agent-portal-extended (`5b69386`)

1. `google-services.json` does not appear in `--stat` for this commit and is not tracked at all (`git ls-files | grep google-services` empty); it's covered by `.gitignore` (`android/**/google-services.json`).
2. `AgentPortalFirebaseMessagingService.kt` — `onMessageReceived` now checks `eventType == "permission_required" || eventType == "plan_required"` (was `input_required`), requires `permissionId` present, else logs and ignores. This **matches** repo 1's `PushNotificationService.sendToDevice`, which attaches `permissionId`/`toolLabel`/`detail` precisely when `payload.get("permissionId") != null` for those same two event types. The two commits agree; `input_required` now falls through to the informational-only else-branch on the Android side (unhandled, debug-logged), consistent with the backend still sending it under the unchanged webhook-parity push path but never carrying a `permissionId`.
3. Version bump `versionCode 3→4`, `versionName 0.2.1-fcm-android-dev→0.2.2-fcm-live-dev` — sequential, intentional, matches `docs/HANDOFF.md`'s updated `Prior tip` link (→ `v0.2.1-fcm-android-dev`).
4. README/ROADMAP/HANDOFF — all three consistently claim only that the **backend send path** is live/non-mocked (auth verified against real Google servers via the throwaway `INVALID_ARGUMENT` test), and each explicitly lists "a real push notification arriving on a real device" as **not yet verified** (no ADB on build host). No overclaim of device-level delivery found. Minor style note (non-blocking): the phrase "live end-to-end" recurs in the commit message and doc headers before the device-verification caveat is restated — reads as "both server and client code paths are live," not "confirmed delivered," and is followed by the caveat every time it's used, so not misleading in context, but worth phrasing more precisely as "credential/send path verified live; device delivery unverified" in future commits to avoid ambiguity at a skim.

## Findings

- No secrets, no crash-on-boot regression, no webhook-contract regression, and the two repos' event-name fixes agree with each other — the four things this review was specifically watching for are all clean.
- Minor doc-wording nit only (see repo 2 item 4) — not blocking.
- `docs/HANDOFF.md`'s new APK SHA-256 value could not be independently verified against a built artifact (no APK present in either diff/repo checkout) — recommend confirming it against the actual `assembleDebug` output before relying on it for release-integrity checks, but this is not a push blocker.

**Overall: GO for both `git push` operations (repo 1 `301e326`, repo 2 `5b69386`).**
