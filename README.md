# Lunch Flow for Moneydance

An **unofficial** Moneydance extension by **Doug Wright**. It pulls accounts, transactions, and balances from [Lunch Flow](https://www.lunchflow.app/) into Moneydance — the same job [Moneydance+](https://infinitekind.tenderapp.com/kb/moneydance/moneydance-overview) does with Plaid, but using Lunch Flow’s worldwide bank coverage instead of a US/Canada-only aggregator.

**Lunch Flow is a third-party service.** This extension’s author did not build it, does not run it, and is not affiliated with it. You need your own Lunch Flow account and API key. The Infinite Kind (makers of Moneydance) are similarly unaffiliated.

Moneydance+ is excellent if your banks are in the US or Canada. Everyone else still downloads OFX/CSV files by hand. This extension is for that gap.

## How it works

Lunch Flow already connects to 20,000+ institutions (UK/EU, US/Canada, Brazil, APAC, New Zealand, and more) and keeps a daily copy of your transactions. You create an **API destination** in the Lunch Flow dashboard, paste the API key into this extension’s settings, map each Lunch Flow account to a Moneydance account, and sync.

```text
Your banks  →  Lunch Flow (open banking, read-only)  →  this extension  →  Moneydance
```

The extension does **not** log into your bank and does **not** use Lunch Flow’s Platform/OAuth API. You stay the Lunch Flow customer; Moneydance just becomes another destination, the same pattern as Sure, Firefly III, Actual Budget, and monetr.

## Status

Phase 3 import is done; Phase 4 polish is in progress (`module_build` 31). Map Lunch Flow accounts, **Import**, confirm/merge blue dots like OFX. Optional import when the data file opens. See [docs/roadmap.md](docs/roadmap.md).

## Requirements

- Moneydance 2024 or newer (`minbuild` 5100)
- An active [Lunch Flow](https://www.lunchflow.app/) subscription with at least one bank connected
- A Lunch Flow **API destination** and its `x-api-key` (created under Destinations → Add Destination → API)

## User flow

1. In Lunch Flow, connect your banks as usual.
2. Destinations → Add Destination → **API**. Copy the key.
3. On that destination, open **Account Access** and enable every account you want in Moneydance. Dashboard connections are not enough; new accounts are often left off. If Refresh accounts shows too few rows, this is why.
4. In Moneydance: **Extensions → Lunch Flow**.
5. Paste the API key, **Save key**, then **Refresh accounts**.
6. Map each Lunch Flow account to a Moneydance account and set a start date (default first of this month; blank = all history Lunch Flow has). After a successful import, From walks forward to last posted date minus 31 days.
7. **Import**. Mappings save on Import and when you close the window (X / Alt+F4 / Close). New rows appear as unconfirmed downloads (blue dots). Pending holds are labelled `[PENDING]`. Optional: **Import when this file opens**.

## Marketplace goal

This is meant to be listed in both:

- **Moneydance** — Infinite Kind’s official extension directory (`Extensions → Manage Extensions`). They audit and counter-sign every `.mxt`.
- **Lunch Flow** — their destinations / integrations list (not a self-serve store; listing is coordinated with Lunch Flow).

API keys will always be entered in settings. Nothing is hardcoded. Details: [docs/marketplace.md](docs/marketplace.md).

## Documentation

| Doc | What it covers |
| --- | --- |
| [docs/product.md](docs/product.md) | Problem, scope, non-goals |
| [docs/architecture.md](docs/architecture.md) | Extension design |
| [docs/lunchflow-api.md](docs/lunchflow-api.md) | Personal API we call |
| [docs/marketplace.md](docs/marketplace.md) | Submission requirements |
| [docs/roadmap.md](docs/roadmap.md) | Build order |
| [CHANGELOG.md](CHANGELOG.md) | Notable changes per `module_build` |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents |
| [SECURITY.md](SECURITY.md) | How secrets are handled |
| [CONTRIBUTING.md](CONTRIBUTING.md) | How to work on the code |

## Development

Kotlin on the JVM, packaged as a signed `.mxt`. Needs JDK 21+ and two jars in `lib/` (see `lib/README.md`). First time only: copy `userconfig/user.properties.example` to `userconfig/user.properties` and run `gradlew genKeys`.

```text
gradlew.bat lunchflow
```

Install `dist/lunchflow.mxt` into the throwaway file **Lunch Flow testing** (drag onto the window, or **Extensions → Manage Extensions → Add from File…**). Same id replaces the previous build. Restarting Moneydance without that step does not pick up new code. The owner may also alpha the main file after **File → Export Backup**. Expect an unsigned/unrecognized-signature warning until Infinite Kind counter-signs a release.

## License

[MIT](LICENSE), © Doug Wright. Moneydance is a trademark of The Infinite Kind. Lunch Flow is a trademark of its owners. This extension is not affiliated with Lunch Flow or The Infinite Kind.
