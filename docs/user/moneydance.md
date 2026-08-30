# Use the extension in Moneydance

Take a **File → Export Backup** before the first import on a file you care about.

## Install

1. Download the latest `.mxt` from [Releases](https://github.com/dvdoug/moneydance-lunchflow/releases).
2. **Extensions → Manage Extensions → Add from File…** and choose that file.
3. Accept the unrecognized-signature warning until Infinite Kind list the extension in the store.

Requires Moneydance 2024 or newer. Installing a newer file with the same id replaces the old one. Restarting Moneydance without Add from File does not pick up a new build.

## Open the window and save your key

1. **Extensions → Lunch Flow**.
2. Paste the API key into **API key**.
3. Click **Save key**.
4. Click **Refresh accounts**.

You should see one row per Lunch Flow account that Account Access allows. Saved mappings appear as soon as the window opens; **Refresh accounts** updates names from Lunch Flow. If the list is too short after a refresh, go back to [Account Access](setup.md#3-turn-on-account-access-for-each-account).

**Remove key** forgets the key stored in this data file.

## Map accounts and choose From

Each Lunch Flow row has a Moneydance account menu and a **From** date.

- Map only **bank** or **credit card** accounts for now (not investments or loans).
- **From** is how far back the *next* Import asks Lunch Flow to look. It uses the same date format as the rest of Moneydance (click the cell for a calendar).
  - Default for a new mapping is the first day of this month.
  - Clear the date (so it shows **All history**) for as much history as Lunch Flow still has (often about 90 days, depending on the bank).
  - After a successful Import, From only moves **forward** (it will not jump to an older date than you set). Later Imports then use about a week of overlap, or longer while a card hold is still open. Pick an older date any time you want a longer backfill.

Mappings are saved when you **Import** or when you close the window (the X, Alt+F4, Escape, or Close). There is no separate Save button.

## Import

Click **Import**.

New rows appear in the Moneydance register as **unconfirmed downloads** (a solid blue dot). That is the same Confirm / Merge process as when you import a file you downloaded from your bank:

- **Confirm** keeps the new row.
- **Merge** combines it with a matching row you already typed.

Pending card holds show with a `[PENDING]` prefix on the Description until they settle. You can Confirm the blue dot while it is still a hold. When it posts for the same amount, a later Import updates that same row. If the settled amount is different, Import removes the hold and brings in the posted amount as a new download (so you are not left with both). Category matching still uses the merchant, same as a posted row.

If you already typed the spend (or a reminder created it), **Merge** the download into that row. Moneydance keeps your original Description.

The bottom status bar shows progress. More detail is in **Help → Console Window** (lines starting `lunchflow:`). The API key is never written there.

**Import when this file opens** is **off** until you tick it. Tick it only after mappings look right.

## Next

If a row is missing or the list of Lunch Flow accounts is wrong, see [troubleshooting](troubleshooting.md).
