# Lunch Flow for Moneydance

An **unofficial** Moneydance extension by **Doug Wright**. It imports bank transactions from [Lunch Flow](https://www.lunchflow.app/) into Moneydance using Open Banking — useful in the UK, EU, and other regions where you would otherwise download OFX/CSV files by hand.

**Lunch Flow is a third-party service.** This extension’s author did not build it, does not run it, and is not affiliated with it. You need your own Lunch Flow account and API key. The Infinite Kind (makers of Moneydance) are similarly unaffiliated.

```text
Your banks  →  Lunch Flow (open banking, read-only)  →  this extension  →  Moneydance
```

The extension never logs into your bank. You connect institutions in Lunch Flow; Moneydance becomes another destination, like Sure or Actual Budget.

## Install and use

Full instructions (Lunch Flow account, API key, Account Access, mapping, Import): **[docs/user](docs/user/README.md)**.

Requires **Moneydance 2024** or newer. Download a build from [Releases](https://github.com/dvdoug/moneydance-lunchflow/releases), then **Extensions → Manage Extensions → Add from File…**. You will see an unrecognized-signature warning until Infinite Kind list the extension.

Take a **File → Export Backup** before the first import on a file you care about.

## Changes and privacy

- [CHANGELOG.md](CHANGELOG.md) — what each version changed  
- [SECURITY.md](SECURITY.md) — how the API key is stored  

## Building from source

See [CONTRIBUTING.md](CONTRIBUTING.md). Short version: JDK 21+, `gradlew lunchflow` → `dist/lunchflow.mxt`.

## License

[MIT](LICENSE), © Doug Wright. Moneydance is a trademark of The Infinite Kind. Lunch Flow is a trademark of its owners. This extension is not affiliated with Lunch Flow or The Infinite Kind.
