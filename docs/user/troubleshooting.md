# If something looks wrong

## Refresh accounts shows too few Lunch Flow accounts

The key only sees accounts enabled under **Account Access** on the API destination. Enable them there, then Refresh again. See [Set up Lunch Flow](setup.md#3-turn-on-account-access-for-each-account).

## Import says up to date but you expected new rows

- Already-imported posted transactions are skipped on purpose (they keep a hidden id). Importing the same month twice should not create duplicates.
- **From** may have walked forward after the last success. Type an older From and Import again if you want a longer window.
- Lunch Flow may not have the transaction yet. Check it inside Lunch Flow first ([missing transactions](https://www.lunchflow.app/docs/guides/troubleshooting/missing-transactions)). If it is not there, Moneydance cannot invent it.

## Gaps after you re-authorise a UK or EU bank

Open-banking connections often expire (commonly about 90 days). Lunch Flow’s [renewing connections](https://www.lunchflow.app/docs/guides/troubleshooting/renewing-connections) guide is the right place to reconnect. History from months you were not authorised may stay missing. That is a bank/Lunch Flow limit, not an Import error. Posted rows already in Moneydance stay put.

## Pending card holds

This extension asks Lunch Flow for pending transactions. They show as `[PENDING]` until they post. Lunch Flow’s own [pending](https://www.lunchflow.app/docs/guides/troubleshooting/pending-transactions) page explains why some banks barely expose holds. If a hold never appears, that is usually the bank, not the mapping.

## Wrong account or inverted amounts

Check the mapping row. Importing into the wrong Moneydance account puts real rows there — delete those unconfirmed downloads (or undo) and map again. Signs follow Moneydance’s usual download converter.

## The yellow Download button in the register

That is Moneydance’s own online-banking signup, not this extension. Ignore it. Confirm and Merge for Lunch Flow rows use the blue-dot confirmation panel.

## Still stuck

Open **Help → Console Window** and look for `lunchflow:` lines (no API key is logged). You can also [open an issue](https://github.com/dvdoug/moneydance-lunchflow/issues).
