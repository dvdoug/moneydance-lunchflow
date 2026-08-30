# Lunch Flow Personal API

This extension uses **only** the Personal API. Official docs:

- Index: https://www.lunchflow.app/docs/llms.txt
- Overview: https://www.lunchflow.app/docs/api/personal-api-overview
- Destination setup: https://www.lunchflow.app/docs/guides/destinations/api

Platform API docs exist at `/docs/api/platform-api-overview`. Do not call those endpoints.

## Setup the user must do

1. Connect banks in the Lunch Flow dashboard (open banking; Lunch Flow never sees bank passwords).
2. **Destinations → Add Destination → API**.
3. Copy the API key.
4. **Account Access (critical).** The API key only sees accounts enabled on that destination, not every connection on the dashboard. New banks often stay **off** until you turn them on. If Refresh accounts shows fewer accounts than Lunch Flow’s Connections page, this is the cause. After enabling, press **Refresh accounts** in the extension.

## Contract we code against

**Base URL:** `https://www.lunchflow.app/api/v1`

(`https://lunchflow.app/api/v1` 308-redirects here. Java `HttpClient` defaults to **not** following redirects; we follow `NORMAL` and call `www` directly.)

**Auth:** every request

```http
x-api-key: <user key from Settings>
```

### `GET /accounts`

List accounts exposed to this API destination.

```json
{
  "accounts": [
    {
      "id": 0,
      "connection_id": 0,
      "name": "string",
      "institution_name": "string",
      "institution_logo": "string",
      "provider": "gocardless",
      "currency": "string",
      "status": "ACTIVE"
    }
  ],
  "total": 0
}
```

`provider` is an aggregator id (`gocardless`, MX/Finicity, Pluggy, Snaptrade, etc.). `status` other than `ACTIVE` should not sync until the user renews the connection in Lunch Flow.

Docs: https://www.lunchflow.app/docs/api/personal-api/listAccounts

### `GET /accounts/{accountId}/transactions`

Query:

| Param | Default | Notes |
| --- | --- | --- |
| `include_pending` | `false` in Lunch Flow’s docs | **We pass `true`.** Pending is first-class; we set-reconcile it instead of inserting it like posted. |
| `from` | — | `YYYY-MM-DD`, inclusive. Docs do not list a 4xx for a date older than the bank/consent window; we treat a 200 with a shorter list as success (missing history is a Lunch Flow/bank gap, not an API error). |
| `to` | — | `YYYY-MM-DD`, inclusive |

```json
{
  "transactions": [
    {
      "id": "string",
      "accountId": 0,
      "amount": 0,
      "currency": "string",
      "date": "2019-08-24",
      "merchant": "string",
      "description": "string",
      "isPending": true
    }
  ],
  "total": 0
}
```

`id` may be **null** on pending transactions. For those we use a synthetic pending FITID (hash of date, amount, currency, merchant, description). If any of those fields change, the key changes and the row is deleted + recreated — same as a pending→posted transition. Posted rows with a null id are skipped (should not happen).

Docs: https://www.lunchflow.app/docs/api/personal-api/getAccountTransactions

### `GET /accounts/{accountId}/balance`

```json
{
  "balance": {
    "amount": 123,
    "currency": "<string>"
  }
}
```

Docs: https://www.lunchflow.app/docs/api/personal-api/getAccountBalance

### `GET /accounts/{accountId}/holdings`

Brokerage accounts only (SnapTrade, MX, Finicity, Pluggy). Expect 501 / “holdings not supported” otherwise. Phase 2+.

Docs: https://www.lunchflow.app/docs/api/personal-api/getAccountHoldings

## Errors to treat as user-facing

| HTTP | Meaning for Settings / Sync UI |
| --- | --- |
| 401 | Missing/invalid key. Ask the user to paste the API destination key. |
| 403 | Key valid but forbidden — often inactive Lunch Flow subscription. |
| 404 | Account id no longer exists or is hidden by Account Access. |
| 501 | Holdings not supported for this account. |

Never put the key in error text.

## Related Lunch Flow behaviour (not API fields)

- Sync on Lunch Flow’s side is typically **daily**. **Import** in Moneydance fetches whatever Lunch Flow already has; it does not force a new bank crawl unless Lunch Flow adds that later.
- Users can pause bank sync, fix inverted signs, and rename accounts in Lunch Flow. We should display Lunch Flow’s names, not invent ours.
- Bank connections expire and must be **renewed in Lunch Flow**. Surface that; we cannot renew from the Personal API.

## Live-test checklist (do not commit the key)

After the client exists, hit `GET /accounts` with a real key and save **redacted** fixtures (no real merchants/amounts if they are sensitive; synthetic fixtures are better) under `src/test/resources`.
