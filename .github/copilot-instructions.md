# GitHub Copilot instructions

Follow **AGENTS.md** at the repository root. That file is the source of truth for this project.

Short version:

- This is a **Moneydance Kotlin extension** by Doug Wright that imports data from the **Lunch Flow Personal API**. Lunch Flow is a third-party service; user-facing copy must say so.
- Extension ID: `lunchflow`. Package: `com.moneydance.modules.features.lunchflow`.
- **Never hardcode API keys.** Settings UI only.
- Personal API only (`https://lunchflow.app/api/v1`, `x-api-key`). Not the Platform API.
- Import via `OnlineTxn` + `showDownloadedTxns`. Do not create `ParentTxn`s. Posted FITID `lunchflow:{accountId}:{transactionId}`. Pending set-reconcile unconfirmed `lunchflow:pending:…` only.
- Swing on the EDT; HTTP off the EDT. Release `AccountBook` listeners on file close.
