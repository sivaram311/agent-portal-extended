# SIGN-OFF — agent-portal-extended / agent-portal / centralized-security-system (auth-push-lock ship)

| Field | Value |
|-------|-------|
| Session | agent-portal-extended-auth-push-lock-2026-07-26 |
| Reviewer agent id | reviewer-auth-push-lock-1 |
| Provider | claude-code |
| Role | Release / Push Reviewer (readonly) |
| When (UTC+5:30) | 2026-07-26 |

## Scope

Three local commits, one per repo, part of today's "biometric app-lock, OAuth/PKCE SSO, device-token
push, notification-action approval" feature ship. Deciding push GO/NO-GO for each, independently.
No files modified, no git add/commit/push executed by this review.

---

## Repo 1 — `E:\MyWorkspace\agent-portal-extended` (public)

| Field | Value |
|-------|-------|
| Tip SHA | `2a61cc54f522bb305365bb4528357d1098727673` |
| Branch | `main` |
| Files changed | 21 (+1014/-29) |

### Checklist
- [x] Docs updated same turn — README.md, ROADMAP.md, docs/HANDOFF.md all touched in this commit
- [x] No secrets in commit — full diff scanned line-by-line for credentials/keys/tokens; none found.
      `AuthViewModel.exchangeCodeForTokens` POSTs via `HttpURLConnection` to `$issuer/oauth/token`
      where `issuer` comes from `authRepository.getAuthConfig()` (runtime API call to this app's own
      backend), never a hardcoded URL or embedded client secret. PKCE flow uses only a public client
      id + generated code_verifier, consistent with a native-app OAuth client (no client secret is
      expected or present).
- [x] `AndroidManifest.xml`: `<receiver android:name=".notifications.PermissionActionReceiver"
      android:exported="false" />` confirmed exported="false" in the diff itself — not exported,
      not broadcastable by other apps on the device. No `<service>` block for
      `AgentPortalFirebaseMessagingService` was added to the manifest (confirmed absent in diff and
      in current manifest); the service class exists as inert source only, matching the session's
      own documented "build up to the Firebase boundary" decision. `build.gradle.kts`'s
      `firebase-messaging-ktx`/`firebase-bom` lines are unchanged context (pre-existing from the
      skeleton, not added by this commit); this commit only adds `net.openid:appauth` and
      `androidx.fragment:fragment-ktx`.
- [x] `AppLockGate.kt` reviewed for bypass: `unlocked` state defaults to `false` and is only ever set
      `true` inside `onAuthenticationSucceeded`, or via the explicit "Continue" button shown solely
      when the device itself has no lock screen/biometric configured (`deviceUnprotected`, an honest
      degraded-security disclosure, not a silent bypass). `content()` renders only when `!hasSession`
      (nothing to protect) or `unlocked == true`. No path renders protected content with
      `hasSession=true` and `unlocked=false`.
- [x] Scope check: all 21 files map cleanly to the four stated features (OAuth/PKCE SSO, biometric
      lock, device-token push client plumbing, notification-action approval) plus integration glue
      (MainActivity bridge, NavGraph, ChatViewModel wiring) and docs. Nothing unrelated found.

### Verdict
**GO**

### Findings
- None blocking. Minor observation (non-blocking): OAuth/PKCE SSO is fully coded but not
  functional end-to-end yet since the server-side allow-list change (Repo 3) isn't deployed —
  this is disclosed accurately in README/ROADMAP, not a defect in this commit.

---

## Repo 2 — `E:\MyWorkspace\agent-portal` (public, live DEV/PREPROD/PROD deployments elsewhere)

| Field | Value |
|-------|-------|
| Tip SHA | `3a0c7d0e113a91a87b2ca152fa58d1d28f3337b3` |
| Branch | `main` |
| Files changed | 8 |

### Checklist
- [x] Docs updated same turn — `docs/OPS.md` documents the new `/api/devices` endpoint and push
      dispatch wiring
- [x] No secrets in commit — full diff scanned; none found
- [x] `DeviceTokenController`'s `/api/devices` endpoints require authentication: confirmed by
      reading the live `SecurityConfig.java` (unmodified by this commit) — the explicit `permitAll()`
      matcher list (lines 63-72: `/api/health`, `/api/auth/config`, `/api/auth/oauth/token`,
      `/api/presets`, `/api/agent/actions`, `/api/os-events`,
      `/api/integrations/forgecity/tamil-rewrite`, `/h2-console/**`) does **not** include
      `/api/devices`, so it falls under the default `auth.requestMatchers("/api/**").authenticated()`
      branch when `requireAuth` is true (CSS enabled or app-security enabled).
- [x] `PushNotificationService` confirmed log-only: `sendToDevice` only calls `log.info(...)` with a
      truncated token, no outbound HTTP/SDK call. `backend/pom.xml` diff for this commit is empty —
      no `firebase-admin` (or any) dependency was added. Matches the "build up to the boundary"
      decision.
- [x] `@Transactional` fix on `DeviceTokenController.unregister()` present and correct: the method
      calls `deviceTokenRepository.deleteByToken(token)`, a derived delete-query method, now wrapped
      with `@Transactional` — matches the documented bug ("No EntityManager with actual transaction
      available").
- [x] `scripts/start-dev-backend.ps1` reviewed line-by-line: reads `CURSOR_API_KEY` and
      `POSTGRES_PASSWORD` from `.env` into process env vars but never echoes/logs their values
      (only non-secret status like `cssEnabled`/`authUrl`/`issuer` and "Backend UP" are printed).
      Postgres defaults (`agent`/`agent`) are placeholder local-dev fallbacks, not a discovered
      credential, and only apply under the opt-in `-Postgres` switch.

### Verdict
**GO**

### Findings
- None blocking.

---

## Repo 3 — `E:\MyWorkspace\centralized-security-system` (public; source for a live shared PROD auth server, NOT deployed by this commit)

| Field | Value |
|-------|-------|
| Tip SHA | `616f6a08d80153773b20517004d67b959980cb57` |
| Branch | `release/0.2.0` |
| Files changed | 1 (+6/-0) |

### Checklist
- [x] Branch pre-existing, not created by this session: `git reflog` shows `release/0.2.0` checked
      out since `2026-07-15 05:11:48` with a long, unrelated commit history (seed/docs/hardening
      commits through `2026-07-17`) well before today. Today's commit is simply the newest tip
      (`[ahead 1]` of `origin/release/0.2.0`).
- [x] Diff is exactly `src/main/java/com/css/auth/service/OAuthService.java`, +6/-0, confirmed by
      `git show --stat`. The existing `http`/`https`/`localhost`/`delena.buzz` branch (the
      unmodified context lines around it) is byte-for-byte unchanged — the patch is a pure addition
      of one new `if` block after the existing return, nothing removed or reordered.
- [x] New branch is an exact-match allow-list entry:
      `if ("buzz.delena.agentportal".equalsIgnoreCase(scheme)) { return
      "oauth".equalsIgnoreCase(host); }` — matches one literal scheme string and one literal host
      string, both required. Not a wildcard, prefix match, or regex; a different custom scheme or a
      different host under this scheme would not match.
- [x] Pre-existing uncommitted work confirmed isolated: `git status --porcelain` after the commit
      shows ` M src/main/java/com/css/auth/controller/AuthController.java` (modified, uncommitted)
      plus untracked `.agent-portal/`, `.cursor/`, `AGENTS.md` — all still sitting there, untouched.
      `AuthController.java`'s on-disk mtime is `2026-07-15 14:20:59`, well before today's session,
      corroborating it predates this work and was not authored by it. None of these four items
      appear in the commit's file list (which is exactly the one `OAuthService.java` file) — today's
      commit did not sweep any of them in.
- [x] "Not deployed" claim independently verified: `Get-CimInstance Win32_Process -Filter
      "Name='java.exe'"` shows PID 1692 running
      `"G:\apps\css-next\centralized-security-system.jar" --spring.profiles.active=prod
      --server.port=5910` — i.e. `css-next.delena.buzz` is served from a pre-built JAR artifact
      under `G:\apps\css-next\`, entirely separate from this source checkout at
      `E:\MyWorkspace\centralized-security-system`. A `git commit` only changes tracked files in the
      working tree/object database; it cannot alter bytecode already loaded into a running JVM, nor
      the on-disk JAR at `G:\apps\css-next\centralized-security-system.jar`, which was built and
      copied there at some earlier point independent of this commit. For this change to reach that
      server, the JAR would need to be rebuilt from this source and redeployed to `G:\`, replacing
      the running process — that has not happened, and is exactly why the machine's Q1/Q2 promote
      gate (evidence pack + EM GO/NO-GO) exists before any such action. The commit message's own
      framing ("Source-only change: not deployed... needs a real Q1/Q2 promote") is accurate.

### Verdict
**GO**

### Findings
- None blocking. This repo is PROD-auth-adjacent source; the change itself is narrow, additive-only,
  correctly scoped to an exact scheme+host match, and cleanly isolated from unrelated in-progress
  uncommitted work in the same working tree. Pushing this commit to `origin/release/0.2.0` has zero
  effect on the live `css-next.delena.buzz` PROD instance until a separate, explicit Q1/Q2 promote
  (build + evidence pack + EM GO) deploys a new JAR — that gate is not being bypassed or implied to
  be satisfied by this push.

---

## Overall Verdict

**GO** — push all three commits.

### Top findings across the ship
1. Repo 3's redirect allow-list addition is exact-match (scheme AND host), does not touch or widen
   any existing branch, and is fully isolated from pre-existing unrelated uncommitted changes in the
   same working tree (`AuthController.java`, `.agent-portal/`, `.cursor/`, `AGENTS.md` all remain
   untouched and uncommitted).
2. Repo 1's new `PermissionActionReceiver` is genuinely `exported="false"`, and no Firebase
   `<service>` block was registered — both match the stated security posture, no accidental
   unauthenticated attack surface introduced.
3. Repo 3's "not deployed" claim is independently confirmed: the live `css-next` process (PID 1692,
   `G:\apps\css-next\centralized-security-system.jar --spring.profiles.active=prod`) is a separate
   pre-built artifact from a different drive, unreachable by a source-only git commit. Nothing
   downstream should mistake this push for a production change; a real Q1/Q2 promote is still
   required before the SSO redirect allow-list takes effect.

No secrets, no scope creep, no loosened auth, no swept-in unrelated work found in any of the three
commits.
