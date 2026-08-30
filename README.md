# Lunch Flow for Moneydance

An **unofficial** Moneydance extension by **Doug Wright**. It imports bank transactions from [Lunch Flow](https://www.lunchflow.app/) into Moneydance using Open Banking — useful in the UK, EU, and other regions where you would otherwise download OFX/CSV files by hand.

**Lunch Flow is a third-party service.** This extension’s author did not build it, does not run it, and is not affiliated with it. You need your own Lunch Flow account and API key. The Infinite Kind (makers of Moneydance) are similarly unaffiliated.

```text
Your banks  →  Lunch Flow (open banking, read-only)  →  this extension  →  Moneydance
```

The extension never logs into your bank. You connect institutions in Lunch Flow; Moneydance becomes another destination, like Sure or Actual Budget.

## Install

1. Download the latest `lunchflow-unsigned-vN.mxt` from [Releases](https://github.com/dvdoug/moneydance-lunchflow/releases).
2. In Moneydance: **Extensions → Manage Extensions → Add from File…** and choose that file.
3. Confirm any “unrecognized signature” warning. That is expected until Infinite Kind list and counter-sign a store build. Same `id` replaces a previous install; restarting Moneydance alone does not pick up a new file.

Requires **Moneydance 2024** or newer.

## Set up

1. In Lunch Flow, connect your banks as usual.
2. **Destinations → Add Destination → API**. Copy the key.
3. On that destination, open **Account Access** and enable every account you want in Moneydance. Connections on the dashboard are not enough; new accounts are often left off. If **Refresh accounts** shows too few rows, this is why.
4. In Moneydance: **Extensions → Lunch Flow**.
5. Paste the API key, **Save key**, then **Refresh accounts**.
6. Map each Lunch Flow account to a Moneydance bank or credit-card account. **From** is how far back the first import looks (default: first of this month; blank: all history Lunch Flow has). Later imports only look back about a month so they stay fast; type an older date whenever you want a backfill.
7. Click **Import**. New rows show as unconfirmed downloads (blue dots) so you can Confirm or Merge in the register, the same way as OFX. Pending card holds are labelled `[PENDING]`.

Mappings are saved when you Import or close the window. Tick **Import when this file opens** if you want a fetch each time you open this data file (off until you opt in).

Take a **File → Export Backup** before the first import on a real data file.

## Help

- Status while importing appears in Moneydance’s bottom status bar.
- Details go to **Help → Console Window** (`lunchflow:` lines). The API key is never logged.
- Changes per version: [CHANGELOG.md](CHANGELOG.md).
- How keys are stored: [SECURITY.md](SECURITY.md).

## Building from source

See [CONTRIBUTING.md](CONTRIBUTING.md). Short version: JDK 21+, `gradlew lunchflow` → `dist/lunchflow.mxt`.

## License

[MIT](LICENSE), © Doug Wright. Moneydance is a trademark of The Infinite Kind. Lunch Flow is a trademark of its owners. This extension is not affiliated with Lunch Flow or The Infinite Kind.
