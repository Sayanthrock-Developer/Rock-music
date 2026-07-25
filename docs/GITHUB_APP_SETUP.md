# Rock Music GitHub App

This document defines the account-level GitHub App used by Rock Music for repository metadata, releases, workflow status, checks, pull requests, and issue visibility.

The repository includes a manifest template at `.github/github-app-manifest.json`. The template intentionally uses `example.invalid` callback and webhook URLs. Replace every placeholder with the HTTPS endpoints of the trusted Rock Music backend before registering the app.

## Purpose

The GitHub App may:

- read repository metadata;
- read release information and release assets;
- read GitHub Actions workflow and run status;
- read checks;
- read pull requests and issues;
- receive release, workflow, check, and pull-request webhook events.

The first version is read-only. Write permissions must not be added unless a separately reviewed feature requires them.

## App identity

Recommended values:

| Field | Value |
| --- | --- |
| GitHub App name | `Rock Music Sayanth` |
| Homepage URL | `https://github.com/Sayanthrock-Developer/Rock-music` |
| Description | `Read-only GitHub integration for Rock Music releases, workflow status, pull requests, and repository metadata.` |
| Visibility | Private |
| Installation scope | Only selected repositories |

GitHub App names are globally unique. If the recommended name is already taken, use `Rock Music Sayanth Developer` and update the manifest before registration.

## Required backend URLs

Replace the placeholder host in the manifest with real HTTPS endpoints:

- OAuth callback: `https://YOUR_BACKEND/github/callback`
- Setup callback: `https://YOUR_BACKEND/github/setup`
- Webhook receiver: `https://YOUR_BACKEND/github/webhook`

Do not point these URLs directly to the Android app. GitHub App private keys and webhook secrets belong only on a trusted backend.

## Registration steps

1. Sign in to GitHub as an owner of `Sayanthrock-Developer`.
2. Open organization settings.
3. Open **Developer settings** → **GitHub Apps** → **New GitHub App**.
4. Enter the identity and backend URLs above.
5. Set the repository permissions exactly as listed below.
6. Subscribe to the listed webhook events.
7. Create the GitHub App.
8. Generate one private key and download the `.pem` file once.
9. Generate a strong webhook secret.
10. Install the app only on `Sayanthrock-Developer/Rock-music` during initial testing.

The exact wording of GitHub settings pages may vary, but the permission names and security requirements in this document remain mandatory.

## Repository permissions

| Permission | Access |
| --- | --- |
| Actions | Read-only |
| Checks | Read-only |
| Contents | Read-only |
| Issues | Read-only |
| Metadata | Read-only |
| Pull requests | Read-only |

Do not grant administration, secrets, environments, deployments, packages, members, organization administration, or repository write access for the initial app.

## Webhook events

Subscribe to:

- Check run
- Pull request
- Release
- Workflow run

GitHub also sends installation lifecycle events required to track installations. The backend must safely handle duplicate and out-of-order deliveries.

## Backend secrets

Store these only in an encrypted backend secret manager:

```text
GITHUB_APP_ID=
GITHUB_APP_CLIENT_ID=
GITHUB_APP_PRIVATE_KEY_PEM=
GITHUB_APP_WEBHOOK_SECRET=
GITHUB_APP_CALLBACK_URL=
GITHUB_APP_SETUP_URL=
GITHUB_APP_WEBHOOK_URL=
```

`GITHUB_APP_ID` and the client ID are identifiers, not authentication secrets. The private key and webhook secret are highly sensitive.

Never commit:

- the `.pem` private key;
- installation access tokens;
- user access tokens;
- webhook secrets;
- client secrets;
- JWTs;
- signed callback state values.

## Authentication model

The backend must:

1. create a short-lived GitHub App JWT using the private key;
2. exchange that JWT for an installation access token;
3. request only the installation and repository selected by the user;
4. keep installation tokens server-side;
5. return only the minimum normalized data needed by the Android client.

The Android app must never mint GitHub App JWTs or receive the private key. It must not use a personal access token as a replacement for the GitHub App.

## Webhook verification

For every webhook request:

1. read the raw request body before JSON parsing;
2. calculate `HMAC-SHA256(rawBody, GITHUB_APP_WEBHOOK_SECRET)`;
3. compare the result with `X-Hub-Signature-256` using a constant-time comparison;
4. reject missing, malformed, or mismatched signatures;
5. record the `X-GitHub-Delivery` identifier and ignore duplicates;
6. accept only the configured event types;
7. return quickly and process longer work asynchronously on the backend.

Do not log raw authorization headers, tokens, private keys, callback codes, or complete webhook payloads containing private repository data.

## Android client contract

The Android app may call the trusted backend for normalized data such as:

- installed account and repository name;
- latest release version and permitted asset metadata;
- workflow run state and conclusion;
- pull-request title, number, state, and checks summary;
- issue title, number, state, and labels.

The Android app must show explicit states:

- Not configured
- Not installed
- Authentication required
- Repository not selected
- Offline
- Permission missing
- Rate limited
- Service unavailable
- Available

The client must never display a fake installation, release, workflow, or authentication success state.

## Installation safety

During testing:

- keep the app private;
- install it only on the Rock Music repository;
- select repositories individually rather than granting all repositories;
- rotate the webhook secret after any suspected exposure;
- revoke and replace the private key if it leaves the backend secret manager;
- review GitHub App audit logs after permission or installation changes.

## Verification checklist

- [ ] All `example.invalid` URLs replaced
- [ ] App created under `Sayanthrock-Developer`
- [ ] Private visibility enabled
- [ ] Only read permissions granted
- [ ] Only required webhook events enabled
- [ ] Private key stored outside the repository and APK
- [ ] Webhook secret stored outside the repository and APK
- [ ] Signature validation tested with valid and invalid payloads
- [ ] Duplicate delivery handling tested
- [ ] Installation limited to `Rock-music`
- [ ] Backend installation-token exchange tested
- [ ] Android unavailable and error states tested
- [ ] App permissions reviewed before production installation

## Future write capabilities

Any future feature that writes comments, creates issues, updates pull requests, dispatches workflows, or uploads release assets requires:

- a separate issue and pull request;
- the exact additional permission documented;
- user confirmation before each write action;
- audit logging;
- tests for denied and revoked permissions;
- a new security review before the permission is enabled in production.
