# Set up Lunch Flow

Work through this **before** you paste anything into Moneydance. The goal is a Lunch Flow **API destination** whose **Account Access** list includes every bank account you want in Moneydance.

Lunch Flow’s own help is linked below. Their “API destination” page is written for programmers in places; you only need the key. Ignore anything about `x-api-key` headers.

## 1. Create a Lunch Flow account and connect your banks

Lunch Flow’s [QuickStart](https://www.lunchflow.app/docs/guides/get-started/quickstart) covers signing up and connecting a bank. In short: you log in at [lunchflow.app](https://www.lunchflow.app/), add a connection, and complete the bank’s own login (read-only). Lunch Flow never needs your bank password.

Lunch Flow charges **per connection**. A connection is one bank (that login), not each account at the bank. Current and savings at the same bank are one connection; a second bank is another. You still turn on every account you want in Moneydance under Account Access (step 3). Their prices are on the Lunch Flow site and can change.

If you are not sure your bank is covered, see their [bank coverage](https://www.lunchflow.app/docs/guides/connections/bank-coverage) and the regional pages (for example [UK and EU](https://www.lunchflow.app/docs/guides/connections/regions/uk-eu)).

Wait until the connection shows as **Active** and you can see transactions for that account **inside Lunch Flow**. If Lunch Flow does not have the transaction, Moneydance will not get it either.

## 2. Create an API destination and copy the key

A **destination** is where Lunch Flow sends data (a spreadsheet, a budgeting app, or this extension). For Moneydance you need the **API** destination, not Google Sheets or Lunch Money.

Follow Lunch Flow’s [API destination](https://www.lunchflow.app/docs/guides/destinations/api) page as far as **copying the generated API key**. Treat that key like a password. Do not email it or put it in a screenshot.

You do not need to “use the key in a header.” That sentence is for programmers. Paste it into Moneydance instead (next page).

## 3. Turn on Account Access for each account

This is the step people miss.

The API destination has an **Account Access** list. Each connected account is off until you enable it. New banks you add later are often left off as well.

Lunch Flow explains this on the same [API destination](https://www.lunchflow.app/docs/guides/destinations/api#account-access-control) page: if accounts do not appear through the API, check Account Access.

Enable every account you plan to map in Moneydance. If **Refresh accounts** in the extension later shows too few rows, come back here first.

## 4. Optional: how Lunch Flow formats each transaction

This is **Lunch Flow’s** per-account setup, not the mapping table in Moneydance. Change it in Lunch Flow if signs or dates look wrong *there* first; the extension imports what Lunch Flow already has.

- **Amount signs.** Some banks send spending as a positive number and income as a negative (or the reverse). Lunch Flow’s [transaction amounts](https://www.lunchflow.app/docs/guides/configuration/transaction-amounts) page covers **Reverse amounts** (and a currency override if the bank labelled the currency wrongly).
- **Which date to use.** Lunch Flow can date a transaction from **value** or **book**. Banks do not agree which of those is the authorisation (hold) date and which is the capture or settlement date. Look at a few rows in Lunch Flow against your bank app, then pick the field that matches how you want them to land in Moneydance. There is no single “correct” choice across banks.

You can also rename the account and tidy merchant/description in Lunch Flow ([account name](https://www.lunchflow.app/docs/guides/configuration/account-name), [merchant and description](https://www.lunchflow.app/docs/guides/configuration/merchant-and-description)).

## Next

[Use the extension in Moneydance](moneydance.md).
