# Marketplace submission

The bar is “a stranger can install this from an official directory, enter their own Lunch Flow key, and trust it with a data file.”

## Moneydance (Infinite Kind)

### What “listed” means

- Appears under **Extensions → Manage Extensions**.
- Also downloadable from https://infinitekind.com/extensions as a `.mxt`.
- Every extension is **audited and signed by The Infinite Kind**. Local DevKit signatures are for development. Users who force-load unsigned MXTs are not the marketplace audience.

### What Infinite Kind expects (from their developer docs and existing listings)

- One self-contained `.mxt` (jar/zip). No “also install this JRE” or sidecar scripts.
- Valid `meta_info.dict`: `id`, `module_name`, `module_desc`, `vendor`, `vendor_url`, `module_build`, `minbuild`.
- `id` stable forever (`lunchflow`).
- Kotlin/Java FeatureModule, not a Jython-only script, for a new official extension.
- Does not brick data files. Import path must be idempotent.
- Logging goes to Help → Show Console, not a surprise window.
- Reasonable `minbuild` so old Moneydance copies get a clear “update Moneydance” failure instead of a `NoClassDefFoundError`.

### How to get signed / listed

There is no fully self-serve developer console. Path:

1. Build and dogfood a stable `.mxt`.
2. Post in [Extension Development](https://infinitekind.tenderapp.com/discussions/moneydance-development) and/or contact Infinite Kind.
3. Send the unsigned (or locally signed) package plus source. They counter-sign.
4. Listing copy should match `meta_info.dict` `module_desc`: lead with **UK and EU bank accounts via Open Banking**, then say it uses **Lunch Flow** (third-party). Vendor **Doug Wright**. Do not mention Moneydance+ or Plaid. Always include the unaffiliated disclaimer.

Useful links:

- Developer kit: https://infinitekind.com/developer
- Core API: https://infinitekind.com/dev/apidoc/index.html
- Open-source reference extensions: https://github.com/TheInfiniteKind/moneydance_open
- Forum: https://infinitekind.tenderapp.com/discussions/moneydance-development

### Reviewer hot buttons for *this* extension

- Where is the API key stored, and is it in the git repo / MXT? It must not be.
- Can a sync run twice without duplicating transactions?
- Does unload leak listeners if the user switches data files?
- Network on the EDT (will freeze the UI) — do not.

## Lunch Flow

### What “listed” means

Lunch Flow’s destination catalog (Lunch Money, Actual, Sure, Firefly III, Sheets, MCP, API, CSV/OFX) is **first-party**. There is not a Chrome-Web-Store-style upload form. Listing as “Moneydance” means coordinating with Lunch Flow so they:

- Add **Moneydance** to Destinations in the app and/or docs, **or**
- Document this extension next to actual-flow / Sure as the supported way to use Moneydance.

Either way, users still create an **API destination** and paste the key into Moneydance unless Lunch Flow later adds a dedicated destination type.

Contact: [hello@lunchflow.app](mailto:hello@lunchflow.app). Public docs: https://www.lunchflow.app/docs/guides/destinations/overview

### What Lunch Flow will care about

- Personal API only, `x-api-key`, no key leakage.
- Clear setup: “create API destination → paste key in Moneydance”, same as Sure/Firefly.
- Respect **Account Access**. User docs must state this as a **required** step, not an optional privacy toggle: the API key only sees accounts enabled on that destination. New connections are often off until enabled. This is the #1 “Refresh still shows one account” failure.
- Pending is on, but implemented as a tagged set-reconcile (add/update/delete only our `lunchflow:pending:…` rows), not as a second posted insert that will duplicate. Be ready to explain this to Lunch Flow; their docs warn about naive pending import.
- Do not hammer the API (no tight polling loops; sync on user action / file open).
- Accurate coverage claims: we sync whatever Lunch Flow already synced, on their cadence.

### What we are not asking them for (v1)

- A custom destination that pushes *into* Moneydance. Desktop Moneydance is not a public URL. Pull is the correct model.
- Platform OAuth. That would make *us* the app; different listing, different security.

## Shared polish checklist before either submission

- [x] Settings UI for the API key; Refresh accounts is the connection test
- [ ] In-app help: screenshots-level copy for creating the API destination
- [x] Account mapping + From date (rolls forward after success)
- [x] Idempotent import (FITID skip; second import of the same window adds no extra blue dots)
- [x] Errors a non-developer can act on (status bar + window + console)
- [x] Extension icon (Lunch Flow site mark)
- [x] Marketplace blurb (`module_desc`) / LICENSE
- [x] No secrets in the repo, the MXT, or logs
- [ ] Tested on current Moneydance Windows (in progress) and at least one of macOS/Linux before Infinite Kind review
- [x] `module_desc` written for the Manage Extensions list, not for engineers
