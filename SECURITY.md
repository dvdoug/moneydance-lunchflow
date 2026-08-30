# Security

This extension reads the user’s bank transactions via Lunch Flow and writes them into a Moneydance data file. Treat it like financial software.

## Secrets

- The only secret in v1 is the user’s **Lunch Flow Personal API key**.
- The user creates it in Lunch Flow (**Destinations → API**) and pastes it into **Extensions → Lunch Flow → Settings**.
- **Never** put a key in source, Gradle properties committed to git, `meta_info.dict`, CI logs, or issue trackers.
- Do not echo the key in `AppDebug` / `System.err` / exception messages. Mask in the UI.
- Store it in the **open Moneydance data file** via `LocalStorage.cacheAuthentication` only. Do not write the key to plaintext `LocalStorage.put`, a sidecar file, or logs. A one-time read of a legacy plaintext `lunchflow.apiKey` migrates it into the auth cache and deletes the plain copy.

If a key leaks: the user revokes/rotates it in the Lunch Flow dashboard. Document that in the Settings UI.

## What this extension must never do

- Call bank APIs or store bank passwords. Lunch Flow is the aggregator; access is read-only.
- Ship a Lunch Flow Platform `client_secret` inside the `.mxt` (the MXT is a zip).
- Send Moneydance data *to* Lunch Flow or anywhere else. Outbound traffic is GET requests to `https://lunchflow.app/api/v1` only.
- Delete Moneydance transactions we did not create. Pending cleanup only deletes **unconfirmed** `ParentTxn`s we tagged (`lunchflow:pending:…`, `isNew`). Never delete a confirmed register row. Never follow a split onto another account’s parent.

## Signing

- Local DevKit keys (`privkey*`) stay on the developer machine and are gitignored.
- Marketplace builds are signed by **The Infinite Kind** after audit. Do not ask users to disable signature checks.

## Reporting

Open a private report (GitHub security advisory once the repo is public, or contact the vendor listed in `meta_info.dict`: Doug Wright). Do not file a public issue that contains a live API key or a data-file dump.
