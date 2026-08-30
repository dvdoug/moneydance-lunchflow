# Product

## Problem

Moneydance can download transactions automatically in two official ways:

1. **OFX Direct Connect** — free, but many banks have dropped it.
2. **Moneydance+** — Plaid aggregator, optional subscription, **US and Canada only**.

Everyone else (UK, EU, and any US user whose bank is not in Plaid’s Moneydance+ coverage) still visits each institution, downloads OFX/QFX/CSV, and imports by hand.

[Lunch Flow](https://www.lunchflow.app/) already solves the aggregator side: one personal subscription, 20,000+ institutions worldwide, daily sync, read-only open banking. It already has destinations for Lunch Money, Actual Budget, Sure, Firefly III, Google Sheets, CSV/OFX, MCP, and a REST API. It does not have a first-class Moneydance destination.

## Goal

Ship an unofficial Moneydance extension, authored by **Doug Wright**, that is **functionally equivalent to Moneydance+ for the “get my transactions into Moneydance” job**, using **Lunch Flow** (a third-party service, unaffiliated with the author) as the aggregator:

- User pastes their Lunch Flow **Personal API** key in Settings (never hardcoded).
- User maps Lunch Flow accounts to Moneydance accounts.
- User imports (manually, and optionally when the data file opens).
- **Posted** transactions import once (FITID) via Moneydance’s download converter. **Pending** authorisations appear as `[PENDING]` unconfirmed rows (Description only; merchant matching still runs). If a dropped pending uniquely matches a new posted row on date, merchant, and exact amount, **update that unconfirmed row in place**. Otherwise delete the unconfirmed pending and add a new posted **download** (not a hand-built `ParentTxn`).
- Quality is high enough to list in **both** the Moneydance extension directory and Lunch Flow’s destination list.

## What “functionally equivalent to Moneydance+” means here

| Moneydance+ | This extension |
| --- | --- |
| Plaid Link runs inside Moneydance | Bank linking stays in the Lunch Flow dashboard |
| Infinite Kind bills Moneydance+ | User already pays Lunch Flow |
| US/Canada | Wherever Lunch Flow has coverage |
| Online → Download All / per-account | Extensions → Lunch Flow → Import (and optional import on file open) |
| Match online accounts | Mapping UI in the extension |
| FITID / confirm / merge downloaded txns | Same idea: stable imported ids, then Moneydance’s own confirm/merge where we can hook it |
| Investment holdings via Plaid | Later: `GET /accounts/{id}/holdings` where the provider supports it |

We are **not** cloning Plaid Link inside Moneydance. That would be Lunch Flow’s **Platform API** (OAuth, app credentials, user-pays vs app-pays). The owner of this repo explicitly chose the **Personal API**. That matches every existing Lunch Flow destination that is not Lunch Flow itself (Sure, Firefly III, actual-flow, monetr): the finance app pulls with a user-supplied API key.

That is the right product for a marketplace extension:

- No Lunch Flow `client_secret` in the `.mxt` (which is a zip users can unpack).
- No OAuth redirect server inside a desktop Swing app.
- Each user keeps their own Lunch Flow billing and bank consents.

## User journey

1. User has Moneydance and a Lunch Flow account with banks connected.
2. In Lunch Flow: **Destinations → Add Destination → API**. Copy the key. On that destination’s **Account Access** tab, enable every account that should appear in Moneydance. Dashboard connections are not exposed automatically; new accounts are often left off. If the extension lists fewer accounts than Connections, this is why.

The in-app **Setup guide** opens [docs/user/setup.md](user/setup.md). That hub must stay written for a non-technical reader: we own the overall flow; Lunch Flow’s docs are linked for their screens. Do not send people only to the API destination page.
3. In Moneydance: install the extension, open **Extensions → Lunch Flow**.
4. Paste key → **Save key** → **Refresh accounts**. On success, we list accounts. (Refresh is the connection test.)
5. Map each Lunch Flow account (`name`, `institution_name`, `currency`) to a Moneydance **bank or credit-card** account. Do not map investment/loan yet.
6. Choose a **From** date (default: first of the current month; blank = all history Lunch Flow has). After a successful import, From becomes `max(current From, last posted − 31 days)` — it never moves earlier than you set. Type an older date to backfill again.
7. **Import**. Mappings also save when the window closes. New rows appear as **unconfirmed downloaded transactions** (solid blue dot). Confirm or merge them in the register the same way as OFX. Pending holds are `[PENDING]` until they post or vanish.
8. Later imports (including auto on open) fetch that 31-day overlap window and **always refresh the current pending set**. FITIDs skip anything already in the register.

## Success criteria

A reviewer at Infinite Kind or Lunch Flow can:

- Install from a signed `.mxt` with no extra JARs or scripts.
- Enter their own API key; nothing in the binary is a secret.
- Map two accounts, sync twice, and see each **posted** bank transaction once. Pending rows may appear, then disappear and be replaced by a posted row with a new id.
- Understand errors (bad key, expired Lunch Flow connection, unsupported currency) without a console dump.
- Find a short in-app explanation of how to create the Lunch Flow API destination.

## Out of scope

- Sending payments or changing anything at the bank (Lunch Flow is read-only).
- Becoming a Lunch Flow Platform app that onboards banks from inside Moneydance.
- Replacing Moneydance+ for users who already have it and like it.
- Full investment-lot accounting on day one. Holdings are a later phase.
- Mobile Moneydance. This is a desktop extension.
