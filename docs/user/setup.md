# Set up Lunch Flow

Work through this **before** you paste anything into Moneydance. The goal is a Lunch Flow **API destination** whose **Account Access** list includes every bank account you want in Moneydance.

Lunch Flow’s own help is linked below. Their “API destination” page is written for programmers in places; you only need the key. Ignore anything about `x-api-key` headers.

## 1. Create a Lunch Flow account and connect your banks

Lunch Flow’s [QuickStart](https://www.lunchflow.app/docs/guides/get-started/quickstart) covers signing up and connecting a bank. In short: you log in at [lunchflow.app](https://www.lunchflow.app/), add a connection, and complete the bank’s own login (read-only). Lunch Flow never needs your bank password.

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

## Next

[Use the extension in Moneydance](moneydance.md).
