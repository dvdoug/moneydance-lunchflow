# Roadmap

Do these in order. Do not start marketplace outreach until the checkboxes in phase 4 are real.

## Phase 0 — Context (this commit)

- [x] Product intent, architecture, API notes, agent files

## Phase 1 — Extension skeleton

- [x] Gradle layout (Kotlin 1.9 / Java 17, DevKit jars in `lib/`)
- [x] `FeatureModule` + `meta_info.dict` (`id = lunchflow`)
- [x] Menu item opens a stub **SecondaryDialog**
- [x] Produce `dist/lunchflow.mxt` (local signature; Infinite Kind must counter-sign for the store)
- [x] Load `dist/lunchflow.mxt` in the throwaway Moneydance file (**Lunch Flow testing**)

## Phase 2 — Settings and Personal API client

- [x] API key field, persist in the open data file (`LocalStorage.cacheAuthentication`), never log
- [x] `LunchFlowClient.listAccounts()`; Refresh accounts is the connection test
- [x] Fixture-based unit tests for JSON DTOs and HTTP error mapping
- [x] User-visible 401/403/network errors

## Phase 3 — Mapping and import

- [x] Map Lunch Flow accounts to Moneydance accounts
- [x] Per-mapping sync start date (default first of month; blank = all available)
- [x] Posted → `OnlineTxn` on `getDownloadedTxns()` with FITID `lunchflow:{accountId}:{txnId}`
- [x] Pending set-reconcile on unconfirmed downloaded rows (`lunchflow:pending:…`, `setPending`, `lunchflow.pending` param)
- [x] Unique pending→posted promote on the downloaded row; otherwise drop pending downloaded + add posted
- [x] Second sync of the same posted window creates zero extra blue dots (verify in **Lunch Flow testing**)
- [x] Currency mismatch is a hard error, not a silent import

## Phase 4 — Marketplace polish

- [x] Import progress on the Moneydance status bar + Help → Console (`lunchflow:` lines); per-account summary in the window
- [ ] Cancel in-progress import
- [x] Optional auto-import on `md:file:opened` (per-file checkbox, default on)
- [x] Mappings save on Import and window close (X / Alt+F4 / Close); no Save mappings button
- [x] After success, roll From to last posted − 31 days
- [x] `module_desc` leads with UK/EU Open Banking
- [ ] Help 101-level setup guide (button still opens Lunch Flow destination docs)
- [ ] README screenshots of the real UI
- [ ] Custom icon
- [x] Windows dogfood on throwaway **Lunch Flow testing**; owner may alpha the main file after a backup

## Phase 5 — Submit

- [ ] Contact Infinite Kind for audit/sign/list
- [ ] Contact Lunch Flow (`hello@lunchflow.app`) for destination/docs listing

## Later

- Home page widget (last sync, errors)
- Show Lunch Flow balance next to Moneydance (no silent rewrite)
- Holdings for brokerage accounts
- Only if product direction changes: Platform API / in-app bank link
