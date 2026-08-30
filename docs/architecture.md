# Architecture

## Shape

This is a single Moneydance **FeatureModule**, written in Kotlin, shipped as `lunchflow.mxt`.

It talks **outbound HTTPS** to Lunch Flow’s Personal API and writes into the open `AccountBook`. There is no local server, no OAuth callback, and no Lunch Flow Platform app.

```text
┌─────────────────────────────────────────────────────────┐
│ Moneydance JVM (JRE 21 / 25)                            │
│  FeatureModule (Main)                                   │
│    SettingsStore  ← AccountBook local storage           │
│    LunchFlowClient → https://www.lunchflow.app/api/v1   │
│    mappings       ← lunchflow.mappings in local storage │
│    SyncService    → OnlineTxn + showDownloadedTxns      │
│    Swing UI (SecondaryDialog) on the EDT                │
└─────────────────────────────────────────────────────────┘
```

## Why Personal API, not Platform API

| | Personal API | Platform API |
| --- | --- | --- |
| Base URL | `https://lunchflow.app/api/v1` | `https://lunchflow.app/api/platform/v1` |
| Auth | `x-api-key` from the user’s API destination | `client_id` / `client_secret` + OAuth |
| Who is the Lunch Flow customer | The Moneydance user | This app, or the user via app-pays / user-pays |
| Bank linking | Already done in Lunch Flow | In-app OAuth, closer to Moneydance+ |
| Secrets in the extension | Only the user’s key, entered at runtime | App secret that must not ship in an `.mxt` |

Personal API is an explicit product decision. Revisit only if we later want in-Moneydance bank linking. That would be a different security design (secret cannot live in the MXT; we would need a tiny backend or a public-client OAuth flow Lunch Flow supports).

Peer integrations that already use Personal API: [Sure](https://www.lunchflow.app/docs/guides/destinations/sure), [Firefly III](https://docs.firefly-iii.org/tutorials/data-importer/lunchflow/), [actual-flow](https://github.com/lunchflow/actual-flow), [monetr](https://monetr.app/documentation/use/lunch_flow).

## Moneydance integration points

Use the official extension API, not unsupported internals, unless a documented Infinite Kind sample does the same thing.

| Need | Mechanism |
| --- | --- |
| Menu item | `FeatureModuleContext.registerFeature` |
| Open UI | `invoke(uri)` |
| File lifecycle | `handleEvent`: `md:file:opened`, `md:file:closing`, `md:file:closed` |
| Optional auto-sync | Same events; never sync when no book is open |
| Home page status | `HomePageView` (later) |
| Logging | Status bar via `MoneydanceGUI.setStatus`; console via `System.err` + `AppDebug.ALL` with a `lunchflow:` prefix. Never log the API key. |
| Dialogs | `SecondaryDialog` so window position persists |

`init()` must not assume a GUI or a data file. Register the feature there; construct windows lazily.

Entry class: `com.moneydance.modules.features.lunchflow.Main`  
Metadata: `meta_info.dict` with `id = lunchflow`.

## Settings and secrets

Per **data file**, not per machine:

- API key
- Account mappings (`lunchFlowAccountId` → Moneydance account UUID)
- Per-mapping `syncStartDate`, `lastPostedDate`
- Auto-import on file open (`lunchflow.importOnOpen`, default off)

Cleared status follows Moneydance **Mark Transactions as Cleared When Confirmed**, not our import code.

Store non-secrets in `AccountBook` local storage under `lunchflow.*` (`lunchflow.mappings`, `lunchflow.importOnOpen`). Store the API key in the same encrypted data file via `LocalStorage.put("lunchflow.apiKey")` and `cacheAuthentication`. Do not delete the put-copy after a cache write (the auth cache has been empty after restart). Never a sidecar file or logs. UI: password field, Save key, Remove key, Refresh accounts. Mappings persist on Import and on window close (X / Alt+F4 / Escape / Close), not a dedicated Save mappings button.

## Decision: import through downloaded transactions

Write `OnlineTxn`s into `account.getDownloadedTxns()`, then call **`MoneydanceGUI.showDownloadedTxns(account)`** so Moneydance’s own `OnlineTxnMerger` creates unconfirmed register rows (`ol.orig-txn`, Confirm, Merge Choices). That is the OFX path. Do not create `ParentTxn`s ourselves.

- **Confirm** = standalone register txn. **Merge** = combine with an existing row. Merge copies **FITID** onto the survivor; next sync skips it. Delete without confirm → it comes back.
- **Automatically Merge Downloaded Transactions** is the user’s pref; we do not force it.
- **Cleared** follows **Mark as Cleared When Confirmed**.

Hidden metadata (not user Keywords):

| Key | Where | Purpose |
| --- | --- | --- |
| FITID `lunchflow:{accountId}:{txnId}` | `OnlineTxn.setFITxnId` / `AbstractTxn.setFiTxnId(PROTO_TYPE_OFX, …)` | Posted identity; skip + merge |
| FITID `lunchflow:pending:{accountId}:{id-or-synth}` | same | Pending identity |
| `lunchflow.pending` = `true` | `OnlineTxn.setExtraParameter` / `ParentTxn.setParameter` | Cheap filter |
| `OnlineTxn.setPending(true)` | downloaded row | Staging flag |

Null Lunch Flow pending ids use `synth:{sha256(date, amount, currency, merchant, description).take(16)}`.

Pending set-reconcile applies to register `ParentTxn`s we tagged with `lunchflow:pending:` — including rows the user already **confirmed** to clear the blue dot. Confirm is not a do-not-touch barrier for *our* holds. Never delete reminder/typed rows or posted `lunchflow:` FITIDs. Never follow a split onto another account’s parent. Never treat “all uncleared register rows” as pending.

Pending → posted, **unique** match (exact amount, merchant case-insensitive, date within 7 days, 1:1): retarget that parent to the posted FITID, clear `lunchflow.pending`, drop `[PENDING] `. Unconfirmed also take the settled payee; confirmed keep the user’s Description minus our label. Ambiguous, amount-changed, or vanished: `deleteItem` that pending parent (confirmed or not) and add a posted download when Lunch Flow has one. Auth £100 / capture £95 must not leave both amounts in the register.

## First import window

Per mapping, **sync start date** (`YYYY-MM-DD`). Default: first day of the current month. Blank = all history Lunch Flow will return **for this run**. Fetch from = min(From, lastPosted − 7 days, oldest open lunchflow:pending: date − 1 day). Seven days covers late posted clearing (timezone, weekend, holiday), not card-auth life; live holds keep the window open for as long as they sit. After a successful import, persist `syncStartDate = max(current From, lastPosted − 7)` so From only moves **forward**. A January backfill then walks up to last posted − 7. Typing an older From and clicking Import still backfills. `include_pending=true`.

Unconfirmed pending rows get a `[PENDING] ` **Description** prefix after `showDownloadedTxns`, so they stay visible in the register. `OnlineTxn.setName` and `ol.orig-payee` stay the raw merchant — Moneydance’s similar-payee matcher is prefix/suffix on that tag, and `[PENDING] ` at the front zeros the prefix score. Hidden FITID / `lunchflow.pending` remain the source of truth. Description prefix is stripped on promote to posted; a leftover `[PENDING] ` on `ol.orig-payee` is stripped on our FITIDs at import.

Currency: if Lunch Flow `currency` is set and differs from the Moneydance account’s `CurrencyType.idString`, skip that mapping (hard error).

## Sync algorithm

1. HTTP off the EDT (`SyncService` / `SwingWorker`). Apply download-list writes and `showDownloadedTxns` on the EDT.
2. `GET /accounts/{id}/transactions?include_pending=true&from=&to=` (`to` = today; `from` = min of mapping start, last posted − 7, oldest open hold − 1).
3. Skip only FITIDs still on **live register** `ParentTxn`s on that account. Prune download-list rows whose FITID is already on the register.
4. Posted with a non-null id: skip if FITID known; else `downloaded.newTxn()`, fill, `STATUS_NEW`.
5. Pending: set-reconcile register parents with our pending FITID prefix (promote unique matches even if already confirmed; delete vanished holds even if confirmed); else add a NEW download.
6. `downloaded.syncItem()` + `account.downloadedTxnsUpdated()`.
7. `MoneydanceGUI.showDownloadedTxns(account)`.
8. On success, persist `lastPostedDate` and roll `syncStartDate` forward to last posted − 7 days (not earlier than the From the user set).

Amount sign: Lunch Flow `amount` is signed cashflow on `OnlineTxn.setAmount` (negative = money leaving the account). Register signs are owned by Moneydance’s converter.

Currency: if Lunch Flow `currency` is set and differs from the Moneydance account, skip that mapping (hard error). Do not convert silently.

## HTTP client

JDK `java.net.http.HttpClient` (follow `NORMAL` redirects; call `www`). JSON via the small in-repo parser (`api/Json.kt`), not Jackson. Timeouts, User-Agent `moneydance-lunchflow/{module_build}`. Treat 401/403 as “key or subscription”, 404 as mapping stale.

## Threading

- HTTP + parse + FITID scan: background.
- All Swing and all `syncItem()` that touch UI-bound objects: EDT, or follow Infinite Kind’s documented pattern if `syncItem()` is safe off-EDT — **verify against moneydance_open samples before choosing**. Default to: compute on background, apply writes on EDT unless a reference extension does otherwise.

## Testing

Layer the code so `LunchFlowClient` and FITID / amount conversion have pure unit tests with fixture JSON. The FeatureModule and Swing stay thin.

A full GUI test requires installing an `.mxt` into a throwaway Moneydance file. Do not use production data files in automated tests. GitHub Actions runs unit tests only; master also uploads an unsigned `.mxt` artifact.

## Build

Infinite Kind DevKit **5.1** jars in `lib/`:

- Gradle wrapper 9.x, Microsoft OpenJDK 21 on the owner’s Windows machine
- Kotlin language and API **1.9**, Java release **17**
- `layout.buildDirectory` under `%TEMP%` (OneDrive)
- If `dist/lunchflow.mxt` is locked by Moneydance, sign writes `dist/lunchflow-new.mxt`

Official listing requires Infinite Kind to audit and **counter-sign** the MXT. Local `genkeys` is for development force-load only.
