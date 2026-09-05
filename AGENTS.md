# AGENTS.md

Instructions for AI coding agents working in this repository. Humans should start with [README.md](README.md) (install and use only — no roadmap or agent-speak). Product intent lives in [docs/product.md](docs/product.md).

## Current state (read this first)

Shipped locally as **`module_build` 50** (`Version.kt`, `meta_info.dict`, and [CHANGELOG.md](CHANGELOG.md) must stay in lockstep). Phase 3 import is done. Phase 4 polish is in progress.

**Import path (do not regress):** write `OnlineTxn`s onto `account.getDownloadedTxns()`, then `MoneydanceGUI.showDownloadedTxns(account)` (`OnlineManager.processDownloadedTxns`). That is Moneydance’s OFX Confirm / Merge path (`ol.orig-txn`, blue dots). **Do not create `ParentTxn`s.** v19–v21 custom `ParentTxn` factories and “attach missing original” repairs were deleted on purpose.

**What works now**

- Settings: paste API key, Save key, Remove key. Persist `lunchflow.apiKey` with `LocalStorage.put` only (same layer as MD+ `access_tokens`). Empty `setApiKey` is a no-op; only `clearApiKey` / Remove key deletes. Never log the key.
- Mapping table: Lunch Flow account → Moneydance account + **From** date. Saved as `lunchflow.mappings`. **No Save mappings button.** Persist on **Import**, and on Close / title-bar X / Alt+F4 / Escape (`goneAway`), but only if accounts loaded this session (do not wipe mappings if the table never populated).
- **Import** and **Automatically import** (checkbox, default **off** until `lunchflow.importOnOpen=true`) share `SyncService`. Auto: ~1.8s after `md:file:opened`, then every 30 minutes while the file stays open (Swing `Timer`; stop on close/unload). HTTP off EDT; `showDownloadedTxns` + pending reconcile on EDT. Status bar / console describe the import, not why it ran.
- Progress: `MoneydanceGUI.setStatus("Lunch Flow: …", progress)` and Help → Console (`lunchflow:` via `System.err` + `AppDebug.ALL`). Never log the key.
- From date: this run is `min(From in the table, lastPosted − 7 days, oldest open Lunch Flow hold − 1 day)`. Seven days is timezone/weekend/holiday clearing, not auth life. Open holds come from register `lunchflow:pending:` dates so a live auth is never treated as vanished. Blank From **and** no last posted omits `from=` (all history). After a **successful** import, persist From as `max(current From, lastPosted − 7)` so it only walks **forward** (a long-lived hold extends the **fetch**, not the saved From). User can type an older From to backfill; then it walks forward again. `from=` older than the bank/consent window should 200 with fewer rows, not 4xx (see [docs/lunchflow-api.md](docs/lunchflow-api.md)).
- Posted FITID `lunchflow:{accountId}:{txnId}`; pending `lunchflow:pending:…`. Skip only live register `ParentTxn` FITIDs. Same pending FITID with a new Lunch Flow amount updates that register row (payee/memo too). Description-only changes wait until settle. Unique pending→posted match promotes in place; otherwise `deleteItem` that pending parent even if confirmed.
- `module_desc`: Open Banking / UK and EU first; Lunch Flow as the pipe; unaffiliated disclaimer. Do not mention Moneydance+ or Plaid in listing copy.

**Do not do next unless asked**

- README / in-app screenshots of the real UI (add under `docs/user/images/` and update when the window changes).
- Holdings; do not map investment/loan accounts in production advice.
- Do **not** write Lunch Flow `GET /accounts/{id}/balance` onto Moneydance ledger/available (`ol.ledgerbal` / `ol.availbal`). LF has one user-configurable number (pending vs settled). MD+’s two-line balance box is Plaid ledger + available; stuffing one LF figure into either slot is wrong.
- Marketplace outreach (Phase 5).

**Dogfood:** throwaway file **Lunch Flow testing**. Owner may also alpha the main file **after a backup**. Agents still must not request production data or keys.

## What this repo is

A **Moneydance extension** that downloads bank transactions (and later, if asked, investment holdings) from the **Lunch Flow Personal API** and imports them into the open Moneydance data file.

It is the non-US-friendly analogue of **Moneydance+**: Moneydance+ uses Plaid and is limited to US/Canada; this extension uses Lunch Flow, which already covers banks worldwide. Users still connect banks in Lunch Flow. This extension only pulls data that Lunch Flow already has.

**Target quality:** polished enough to submit to:

1. The official Moneydance extension directory (`Extensions → Manage Extensions`)
2. Lunch Flow’s destination / integrations listing

## Canonical names

| Item | Value |
| --- | --- |
| Extension ID (`meta_info.dict` `id`) | `lunchflow` |
| Display name | Lunch Flow |
| `meta_info.dict` `module_build` | Integer (`Version.MODULE_BUILD`). Moneydance shows this as **vN** in Manage Extensions. Increase on every shipped `.mxt`; keep `meta_info.dict` and [CHANGELOG.md](CHANGELOG.md) in lockstep with `Version.kt`. Do not invent a second version string. |
| Author / `meta_info.dict` `vendor` | Doug Wright |
| Java/Kotlin package | `com.moneydance.modules.features.lunchflow` |
| Artifact | `dist/lunchflow.mxt` |
| Language | Kotlin (Java only if a library forces it) |

Do not rename the extension ID after the first public build. Infinite Kind treats it as the install identity.

## Hard rules

- **Never hardcode a Lunch Flow API key, client secret, or any credential.** Keys come from the extension Settings UI and are stored in the open data file (`LocalStorage.put("lunchflow.apiKey")` only).
- **Lunch Flow signed cashflow goes on `OnlineTxn.setAmount`.** Moneydance’s download converter owns register signs. Do not create `ParentTxn`s or flip amounts ourselves.
- **Use the Personal API only** (`https://www.lunchflow.app/api/v1`, header `x-api-key`; follow redirects). Do not add the Platform API (`/api/platform/v1`, OAuth, `client_id` / `client_secret`) unless product direction explicitly changes. See [docs/architecture.md](docs/architecture.md).
- **Never log the API key**, paste it into commits, write it to `System.err` / `AppDebug`, or include it in crash reports. Mask it in the UI (`••••` plus last 4).
- **Import through Moneydance’s download converter.** Write `OnlineTxn`s onto `account.getDownloadedTxns()`, then call `MoneydanceGUI.showDownloadedTxns(account)` (`OnlineManager.processDownloadedTxns`). That is the OFX Confirm/Merge path (`ol.orig-txn`). Do not create `ParentTxn`s. FITID skip is against live register `ParentTxn`s only.
- **FITIDs:** posted `lunchflow:{accountId}:{txnId}`; pending `lunchflow:pending:{accountId}:{id-or-synth}`. Protocol `OnlineTxn.PROTO_TYPE_OFX`. Do not invent posted ids. Hidden flag `lunchflow.pending` via `OnlineTxn.setExtraParameter` / `ParentTxn.setParameter`. Never use user Keywords for this.
- **Pending set-reconcile our `lunchflow:pending:` register rows**, including after the user confirmed the blue dot. Same pending FITID still in Lunch Flow with a **new amount**: rewrite that parent’s cashflow (and Description/memo). Description-only changes wait until settle. Unique pending→posted match (exact amount, merchant, date within 7 days) updates that parent in place (posted FITID, clear `lunchflow.pending`, Description/memo from the posted txn). If it leaves Lunch Flow pending **without** a unique match (amount changed, ambiguous, vanished): `deleteItem` that parent even if confirmed, then add posted as a download when we have one. Never delete reminder/typed rows or posted `lunchflow:` FITIDs. Never follow a split onto another account’s parent. Do **not** put `[PENDING] ` on Description; identity is FITID / `lunchflow.pending` only. Strip a leftover prefix from Description / `ol.orig-payee` on our FITIDs. See [docs/architecture.md](docs/architecture.md).
- **Honor the mapping start date as a backfill floor.** Fetch `min(From, lastPosted − 7, oldest open hold − 1)`. After success, persist `syncStartDate = max(current From, lastPosted − 7)` so From only moves **forward**. The user can type an older From to force another backfill. FITIDs skip already-imported posted rows.
- **Account Access is critical in every user-facing doc.** `GET /accounts` is not “all Lunch Flow connections”. It is the subset enabled on the API destination. New banks often stay off. Setup instructions, Help, marketplace copy, and in-app status must say: Destinations → that API destination → Account Access → enable each account → Refresh accounts. Do not describe Account Access only as an optional way to hide accounts.
- **Help button** (`How to get an API key`) must eventually open a 101-level setup guide (what Lunch Flow is, connect banks, Account Access, API destination, paste key, map, import). It currently points at Lunch Flow’s destination docs as a stopgap — do not treat that as the final link.
- **GUI on the EDT.** Network I/O and JSON parsing on a background thread (`SwingWorker` or equivalent). Never block the Event Dispatch Thread on HTTP.
- **Release listeners** on `md:file:closing` / `md:file:closed` and in `unload()`. Do not retain `AccountBook`, `Account`, or `AbstractTxn` references after the file closes.
- **User-facing copy must say Lunch Flow is third-party.** This extension is by Doug Wright. It is not Lunch Flow, not Infinite Kind, and not affiliated with either. Reuse `Main.THIRD_PARTY_DISCLAIMER` (or the same wording) in the window, Settings, Help, and `module_desc`. Do not write copy that sounds like Doug Wright provides bank connections.
- **Do not commit** Moneydance signing keys, `privkey*`, Gradle local properties with passphrases, `*.mxt` built locally with a personal key, or real user data files.
- Keep this file true. If you change architecture, IDs, or commands, update `AGENTS.md` and the matching doc in `docs/` in the same change.
- **Changelog.** Maintain [CHANGELOG.md](CHANGELOG.md) using Keep a Changelog *categories* (Added/Changed/Fixed/Removed/Security). Headings are `## Unreleased` and `## 31 - YYYY-MM-DD` (integer `module_build`, **no square brackets** — those are for SemVer link refs). Every bump gets a **1–2 line high-level** entry. GitHub Release notes copy the section **body** only (the Release title is already `Lunch Flow vN`).
- **Commit and push every iteration**, even when `module_build` does not change (docs, polish, tests). Do not leave a day’s work only in the working tree. `git push origin` the current branch after each commit (this repo: `master`). Never commit keys, `userconfig/`, `lib/*.jar`, or `*.mxt`.
- **Do not commit planning scratch.** `docs/roadmap.md`, `docs/review-*.md`, and `docs/_local/` are gitignored. Keep them on disk if useful; never `git add -f` them. Durable next-steps live in **Current state** above, not in a committed roadmap.
- **Build the `.mxt` locally** on every iteration that the owner will install (`gradlew test lunchflow` → `dist/lunchflow.mxt`, signed with the gitignored key). CI’s unsigned GitHub Release is extra, not a replacement. Do not skip the local package because Actions ran.

## Non-goals (unless the user asks)

- Embedding Lunch Flow bank-link / OAuth inside Moneydance (that is the Platform API).
- Replacing OFX Direct Connect or Moneydance+.
- Writing transactions back to the bank. Lunch Flow is read-only; this extension is import-only.
- Python / Jython. New Moneydance extensions should be Kotlin.
- A standalone desktop app. The deliverable is an `.mxt` that runs inside Moneydance.

## Architecture snapshot

```
Moneydance (JVM, Swing)
  └─ FeatureModule Main
        ├─ Settings UI  → API key, Refresh accounts
        ├─ Mapping table → Lunch Flow account ↔ Moneydance Account + From
        ├─ SyncService  → Personal API client → OnlineTxn + showDownloadedTxns
        └─ Home widget  → last sync status (optional, later)
```

Settings and account mappings live **in the open `AccountBook`** so they travel with the data file. Store the API key with `LocalStorage.put("lunchflow.apiKey")` only — not `cacheAuthentication` (MD strips `_authentication` on save unless the file has a password and “store passwords”). Mappings stay in `lunchflow.mappings`.

HTTP client: Java 11+ `HttpClient` from the JRE Moneydance ships (MD2024: JRE 21, MD2026: JRE 25). No extra HTTP stack unless there is a clear need.

Details: [docs/architecture.md](docs/architecture.md). Lunch Flow contract: [docs/lunchflow-api.md](docs/lunchflow-api.md).

## Build and run

DevKit **5.1** jars in `lib/` (`moneydance-dev.jar`, `extadmin.jar`). Target **Java 17 bytecode**, **Kotlin language/API 1.9**. Gradle `layout.buildDirectory` is under `%TEMP%` because OneDrive syncs this repo. If `dist/lunchflow.mxt` is locked by a running Moneydance, the sign task writes `dist/lunchflow-new.mxt`.

```text
./gradlew clean genKeys lunchflow     # first machine: generate local signing keys
./gradlew test lunchflow              # compile, test, package, sign → dist/lunchflow.mxt
```

Install: Moneydance → **Extensions → Manage Extensions → Add from File…** → `dist/lunchflow.mxt`.

Unsigned builds may need a force-load during development; production listing requires Infinite Kind to audit and sign.

On Windows: `gradlew.bat lunchflow` (or `.\gradlew lunchflow`). Copy `lib/moneydance-dev.jar` and `lib/extadmin.jar` first (see `lib/README.md`). First machine also needs `userconfig/user.properties` (from the example) and `gradlew genKeys`.

**Toolchain:** this machine needs a **JDK 17+** (21 or 25 preferred), not a standalone `kotlinc`. The Gradle Kotlin plugin downloads the Kotlin 1.9 compiler. Moneydance itself ships a JRE; compiling the extension does not.

## Code layout (intended)

```text
src/main/kotlin/com/moneydance/modules/features/lunchflow/
  Main.kt                 FeatureModule; `md:file:opened` auto-import (~1.8s delay)
  api/                    Personal API client + small JSON parser (no Jackson)
  settings/               API key, mappings, import-on-open
  sync/                   SyncService, SyncEngine, FITID
  ui/                     SecondaryDialog, mapping table, MdNotify, ImportStatus
src/main/java/.../sync/MdAccess.java
                          Java facade over the Moneydance model (Kotlin cannot see some MD getters)
src/main/resources/.../meta_info.dict
```

Keep the API client free of Swing. Keep Swing free of raw JSON.

## Moneydance constraints that agents get wrong

- Entry point extends `com.moneydance.apps.md.controller.FeatureModule`.
- `init()` runs at app start: GUI and data file may **not** exist yet. Register the feature there; open windows on `invoke()` or `md:file:opened`.
- Package the class as `com/moneydance/modules/features/lunchflow/Main.class` plus `meta_info.dict`.
- **Do not create register `ParentTxn`s for import.** Staging is `OnlineTxn`; Moneydance’s converter creates the parent. Pending promote/delete may edit or `deleteItem` unconfirmed parents we tagged. Never `syncItem()` only on a split.
- Amounts are integer minor units in Moneydance; Lunch Flow amounts are decimal. Convert via the account’s `CurrencyType`.
- `SplitTxn.amount` is the wrong sign. Use `value` / `parentAmount` as documented by Infinite Kind.
- `minbuild` in `meta_info.dict` should track the oldest MD build we actually test (start at 2024 / build 5100+ unless we prove older).

Reference implementations: https://github.com/TheInfiniteKind/moneydance_open  
DevKit / API: https://infinitekind.com/developer and https://infinitekind.com/dev/apidoc/index.html

## UX bar for marketplace

- Settings: paste API key, **Save key** / **Refresh accounts**, clear 401/403 copy. Refresh is the connection test (no separate Test button).
- Account mapping: Lunch Flow `name` · `institution_name` → Moneydance account + From date. Persist on Import and window close, not a Save mappings button.
- **Import** and optional **Automatically import** on `md:file:opened` (per-file checkbox, default **off**). Status bar + Help → Console. Never log the API key. Cancel-in-progress is still missing.
- Help (**Setup guide**) opens [docs/user/setup.md](docs/user/setup.md) on GitHub. Keep that guide non-technical: we own the overall flow; link Lunch Flow’s pages for their UI instead of duplicating “click here” for their screens. User-facing docs live in `docs/user/` (not the architecture notes). Update them in the same change as UI copy.

## Verification

There is no headless Moneydance. GitHub Actions (`.github/workflows/ci.yml`) runs `./gradlew test` on PRs. On master it also uploads a 90-day workflow artifact and, the first time a given `module_build` appears, a GitHub Release tagged `vN` with the unsigned `.mxt` (permanent until someone deletes the release). Do not retag or overwrite an existing `vN`. That does **not** exercise the Swing window or Import. Dependabot (`.github/dependabot.yml`) files weekly PRs for Actions and Gradle; merge when CI is green — not a `module_build` bump.

1. Unit-test the API client and FITID / amount conversion **without** the Moneydance UI (mock HTTP).
2. Install the `.mxt` into the throwaway file **Lunch Flow testing** (or similar). Owner may also alpha the main file after **File → Export Backup**. Agents must not request production data or keys. Map, Import twice, confirm no extra blue dots.
3. Confirm the key is not present in logs or in any file under `dist/` except the user’s live data file.

## Doc map

| File | Audience |
| --- | --- |
| [README.md](README.md) | Short human pointer |
| [docs/user/](docs/user/README.md) | End-user setup, Moneydance steps, troubleshooting |
| [docs/product.md](docs/product.md) | Why this exists, user flow |
| [docs/architecture.md](docs/architecture.md) | Technical design |
| [docs/lunchflow-api.md](docs/lunchflow-api.md) | Personal API contract we code against |
| [CHANGELOG.md](CHANGELOG.md) | Notable changes per `module_build` |
| [SECURITY.md](SECURITY.md) | Secrets handling |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to work in the repo |
