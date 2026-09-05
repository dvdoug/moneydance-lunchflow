# If something looks wrong

## Refresh accounts shows too few Lunch Flow accounts

The key only sees accounts enabled under **Account Access** on the API destination. Enable them there, then Refresh again. See [Set up Lunch Flow](setup.md#3-turn-on-account-access-for-each-account).

## Import says up to date but you expected new rows

- Already-imported posted transactions are skipped on purpose (they keep a hidden id). Importing the same month twice should not create duplicates.
- **From** may have walked forward after the last success. Type an older From and Import again if you want a longer window.
- Lunch Flow may not have the transaction yet. Check it inside Lunch Flow first ([missing transactions](https://www.lunchflow.app/docs/guides/troubleshooting/missing-transactions)). If it is not there, Moneydance cannot invent it.

## Gaps after you re-authorise a UK or EU bank

Open-banking connections often expire (commonly about 90 days). Lunch Flow’s [renewing connections](https://www.lunchflow.app/docs/guides/troubleshooting/renewing-connections) guide is the right place to reconnect. History from months you were not authorised may stay missing. That is a bank/Lunch Flow limit, not an Import error. Posted rows already in Moneydance stay put.

If Import says a bank is **not Active**, renew it in Lunch Flow first. This extension cannot reconnect the bank for you.

## Pending card holds

This extension asks Lunch Flow for pending transactions. They import as unconfirmed downloads until they post. If Lunch Flow later changes the **amount** on that same hold, the next Import updates the register row. Lunch Flow’s own [pending](https://www.lunchflow.app/docs/guides/troubleshooting/pending-transactions) page explains why some banks barely expose holds. If a hold never appears, that is usually the bank, not the mapping.

## Wrong Moneydance account

That is the **Import into** column in this extension (which Moneydance account receives the row), not Lunch Flow’s own account settings.

If a row landed in the wrong register, delete those unconfirmed downloads (or undo) and change **Import into**, then Import again.

## Amounts backwards or dates off

Fix this in **Lunch Flow**, then Import again. The extension does not flip signs or pick dates itself.

- **Signs:** Lunch Flow’s [transaction amounts](https://www.lunchflow.app/docs/guides/configuration/transaction-amounts) page — **Reverse amounts** if spending and income are the wrong way around.
- **Dates:** in the same Lunch Flow account settings, choose **value** or **book**. Banks do not agree which of those is the authorisation (hold) date and which is the capture or settlement date. Compare a few transactions in Lunch Flow to your bank app and pick the one you want in Moneydance.

See also [Set up Lunch Flow](setup.md#4-optional-how-lunch-flow-formats-each-transaction).

## The yellow Download button in the register

That is Moneydance’s own online-banking signup, not this extension. Ignore it. Confirm and Merge for Lunch Flow rows use the blue-dot confirmation panel.

## Still stuck

Open **Help → Console Window** and look for `lunchflow:` lines (no API key is logged). You can also [open an issue](https://github.com/dvdoug/moneydance-lunchflow/issues).
