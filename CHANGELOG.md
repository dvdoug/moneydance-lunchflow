# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
This project uses Moneydance **integer `module_build`** (shown as vN in Extensions), not Semantic Versioning.

Reconstructed from the 2026-08-29 development session. There are no git tags for these builds.

## Unreleased

## 47 - 2026-09-05

### Changed
- Checkbox label is **Automatically import**. Status bar and console say importing, not “on file open”.

## 46 - 2026-09-05

### Changed
- API key is stored only with `LocalStorage.put`, same as Moneydance+ Plaid tokens. Dropped `cacheAuthentication` (that cache is not written unless the file password and “store passwords” are both on).

## 45 - 2026-08-31

### Changed
- Import skips a mapped account whose Lunch Flow connection is not Active, and tells you to renew it there.

## 44 - 2026-08-31

### Changed
- Pending holds no longer get a `[PENDING]` prefix on Description. Unique settle still retargets that row and writes the posted payee and memo. Leftover prefixes from earlier builds are stripped on the next Import.

## 43 - 2026-08-30

### Changed
- Import lookback is last posted minus 7 days (clearing lag), or the oldest open hold if that is earlier. Dropped the blanket 31-day window.

## 42 - 2026-08-30

### Changed
- A Lunch Flow hold we imported is still ours after you Confirm the blue dot. If it leaves pending without a unique posted match (auth £100, capture £95), we delete that register row and import the settled amount. Reminder or typed rows are still never deleted.

## 41 - 2026-08-30

### Changed
- Confirming a pending hold to clear the blue dot no longer freezes `[PENDING]` on that row. When it settles and uniquely matches, we retarget that same register txn (posted FITID, drop the label). Reminder or typed rows are still left for you to Merge.

## 40 - 2026-08-30

### Fixed
- `[PENDING]` is only on the visible Description. The download name / `ol.orig-payee` stays the merchant so similar-payee category matching still works on holds. Existing Lunch Flow rows with a poisoned orig-payee are repaired on the next import.

## 39 - 2026-08-29

### Fixed
- An empty API key field no longer deletes the saved key (only Remove key does). Enter no longer triggers Save key.

## 38 - 2026-08-29

### Fixed
- Mapping-table hint under From wraps instead of being cut off.

## 37 - 2026-08-29

### Changed
- From date uses Moneydance’s date field and calendar. Clear it for all history.

## 36 - 2026-08-29

### Fixed
- Mapping table shows saved accounts when the window opens, even if Import-on-open is still running.

## 35 - 2026-08-29

### Fixed
- Keep the API key in the data file (not only Moneydance’s auth cache, which can be empty after a restart).

## 34 - 2026-08-29

### Added
- In-app **Setup guide** opens our user docs (Lunch Flow setup, Moneydance steps, troubleshooting).

## 33 - 2026-08-29

### Changed
- Extension icon is Lunch Flow’s site mark (gold F) instead of the DevKit smiley.

## 32 - 2026-08-29

### Changed
- **Import when this file opens** is off until you tick it.

## 31 - 2026-08-29

### Fixed
- After import, From only moves forward (`max` of the date you set and last posted minus 31 days). A first sync from last week no longer jumps back a month.

## 30 - 2026-08-29

### Removed
- Save mappings button. Mappings persist on Import and on Close / X / Alt+F4 / Escape.

## 29 - 2026-08-29

### Added
- Save mappings when the Lunch Flow window closes.

## 28 - 2026-08-29

### Changed
- Post-import fetch window is last posted minus 31 days (card-auth life plus a one-day buffer).

## 27 - 2026-08-29

### Changed
- After a successful import, walk From forward so later runs (including auto-import) do not re-download the original backfill.

## 26 - 2026-08-29

### Added
- Optional import when the data file opens, plus progress on the Moneydance status bar and `lunchflow:` lines in Help → Console.

## 25 - 2026-08-29

### Removed
- Homemade register-row factory used when Moneydance’s download converter left leftovers.

## 24 - 2026-08-29

### Removed
- One-time “ready to merge older imports” repair for homemade v19 rows.

## 23 - 2026-08-29

### Changed
- Quieter status wording for that one-time merge repair.

## 22 - 2026-08-29

### Changed
- Import writes downloaded transactions and calls Moneydance’s built-in converter so Confirm / Merge works like OFX.

## 21 - 2026-08-29

### Fixed
- Inverted amounts on homemade Current rows; pending cleanup only deletes unconfirmed holds we tagged.

### Security
- Stop writing the API key to plaintext local storage (migrate into the auth cache).

## 20 - 2026-08-29

### Changed
- Plain-English import status and marketplace blurb that leads with UK/EU Open Banking.

## 19 - 2026-08-29

### Fixed
- Hidden downloads on accounts without OFX now appear as unconfirmed register rows (split attached).

## 18 - 2026-08-29

### Changed
- Convert leftover NEW downloads into unconfirmed register transactions (incomplete without v19’s split attach).

## 17 - 2026-08-29

### Fixed
- Treat a FITID as imported only if it is still on a live register transaction, not because it sits in the download list.

## 16 - 2026-08-29

### Fixed
- After you delete imported rows, drop leftover accepted downloads so the next import can recreate them. Status text wraps.

## 15 - 2026-08-29

### Changed
- Compact settings window: mapping table, short status, Account Access hint, unofficial-extension footer.

## 14 - 2026-08-29

### Changed
- Help and docs spell out Lunch Flow Account Access (API key only sees enabled destination accounts).

## 13 - 2026-08-29

### Changed
- Status hint when Refresh returns a single Lunch Flow account.

## 11 - 2026-08-29

### Fixed
- Duplicate Window menu entry; window title stays Lunch Flow (not the data-file name).

## 10 - 2026-08-29

### Fixed
- Relabel existing pending register rows with `[PENDING]` on later imports instead of skipping them silently.

## 9 - 2026-08-29

### Fixed
- Fetch from the mapping start date, and persist downloads so they survive restart.

## 8 - 2026-08-29

### Changed
- Mapping table, persist the API key in the data file, show downloaded transactions after sync.

## 7 - 2026-08-29

### Added
- Map Lunch Flow accounts to Moneydance accounts, per-account start date, and Sync now.

## 6 - 2026-08-29

### Changed
- Drop the extra 0.0.x display version. Extensions only show integer vN.

## 5 - 2026-08-29

### Changed
- Align the (short-lived) dotted display string with `module_build`.

## 4 - 2026-08-29

### Fixed
- Call `www.lunchflow.app` and follow HTTPS redirects so Test connection is not a false 308 failure.

## 3 - 2026-08-29

### Added
- API key settings, Test connection, and Personal API account listing.

## 2 - 2026-08-29

### Changed
- Vendor and in-app copy: Doug Wright; Lunch Flow named as an unaffiliated third-party service.

## 1 - 2026-08-29

### Added
- Kotlin FeatureModule skeleton that loads as `lunchflow.mxt` under Extensions → Lunch Flow.

