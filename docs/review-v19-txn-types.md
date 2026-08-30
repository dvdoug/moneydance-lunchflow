# Critical review: v19 transaction types

**Superseded.** v19–v21 custom `ParentTxn` import was withdrawn. Current path (v22+) is `OnlineTxn` + `MoneydanceGUI.showDownloadedTxns`. Keep this file as history only; do not implement its “createUnconfirmed” recommendations.

**Scope:** read-only review of how this extension creates, matches, and deletes Moneydance transactions. Focus is whether we use the right types (`ParentTxn` / `SplitTxn` / `OnlineTxn`) in the right way.

**Tree reviewed:** current workspace at review time (`Version.MODULE_BUILD` / `meta_info.dict` = **20**; `createUnconfirmed` path is the v19 register import). Throwaway file only.

**APIs checked:** `lib/moneydance-dev.jar` via `javap` (`ParentTxn`, `SplitTxn`, `SplitTxn$Companion`, `AbstractTxn`, `TransactionSet`, `OnlineTxn`, `OnlineTxnList`, `LocalStorage`, `CurrencyType`). Infinite Kind apidoc + developer forum on unconfirmed txns.

---

## Verdict

**Fix-before-ship.** Not a cheerleading pass.

The v19 *direction* is right: bank accounts without OFX/Moneydance+ never surface `OnlineTxnList`, so the user-visible import must be unconfirmed register `ParentTxn`s. Types, FITID protocol, `setIsNew(true)`, and “HTTP off EDT / writes on EDT” are in the right neighbourhood.

The implementation is not safe to ship:

1. **`SplitTxn.makeSplitTxn` negates the parent amount.** Passing Lunch Flow’s signed amount through as both arguments very likely **inverts debit/credit** on the register (a £12.50 coffee becomes a deposit). This must be proven or fixed on the throwaway file before any more imports.
2. **Pending set-reconcile deletes confirmed register txns** (and can delete a *different account’s* parent when a split lands in the mapped account). That violates the product rule and is how you brick a data file in an Infinite Kind audit.
3. **The API key is written to plaintext `LocalStorage` as well as `cacheAuthentication`.** Marketplace audit will flag this.

Everything else is secondary until those three are closed.

---

## Explicit answer: are we using ParentTxn / SplitTxn / OnlineTxn correctly?

**Types: mostly yes. Usage: not yet.**

| Type | Intended role now | What the code does | Verdict |
| --- | --- | --- | --- |
| `ParentTxn` | Register row (bank/CC side) | `ParentTxn.makeParentTxn` + `setTransferType(BANK)` + `setFiTxnId(PROTO_TYPE_OFX)` + `setIsNew(true)` + `TransactionSet.addNewTxn` | **Right type, right skeleton.** Sean Reilly (IK): unconfirmed = `setIsNew(true)` on the parent. |
| `SplitTxn` | Other side (Unspecified income/expense) | `SplitTxn.makeSplitTxn(parent, amount, amount, 1.0, category, …)` then `parent.addSplit(split)` | **Right type, wrong amount convention** (factory negates parent amount). Also can attach to Root if Unspecified is not found. |
| `OnlineTxn` / `OnlineTxnList` | **Not** the user-visible queue. Convert leftovers, then stop writing them. | `SyncEngine` does not create new downloaded rows. `materializeHiddenDownloads` converts `STATUS_NEW` leftovers into register parents and **removes** them from the list. `MdAccess` still contains a full write facade (`newTxn`, `setNew`, `setAmount`, …) that nothing in `apply()` calls. | **Right policy.** Leftover conversion is correct in intent. Dead write helpers should go so we cannot regress. |
| `AbstractTxn` | Shared FITID / new flag | `setFiTxnId`, `setIsNew`, `isNew` (unused in pending cleanup — that is the bug) | FITID + new flag are correct; ownership checks are not. |

**Remaining wrong-type usage**

- We no longer *write* `OnlineTxn`s in the import path. Good.
- We still *treat* every register row whose parent has a `lunchflow:pending:` FITID as a disposable download, including **confirmed** parents and parents that merely *touch* the mapped account via a split. That is using register `ParentTxn` as if it were still an `OnlineTxnList`. That is the remaining type error.
- Mapping **investment** accounts and then creating `TRANSFER_TYPE_BANK` parents with an expense split is the wrong investment transaction shape. Do not offer those account types until holdings exist.
- Docs are split-brained: `AGENTS.md` still says “import via `OnlineTxn`, not `ParentTxn`”; `docs/architecture.md` describes both the new ParentTxn decision *and* the old OnlineTxn algorithm in the same file.

**Confirm / Merge / delete — what actually happens**

- **Confirm:** `setIsNew(true)` is the supported flag (`AbstractTxn.setIsNew` → `setNew` sets `ol.match-status`). Live test: v19 conversion produced register rows the user could confirm. We do **not** set `setOriginalOnlineTxn`. A 2013 IK bug NPEd Confirm without `ol.orig_txn`; Sean said it was fixed. Residual risk: Confirm-as-downloaded / Similar Payees may be weaker than real OFX. Not a type error.
- **Merge:** FITID lives on the parent (`ol_fitid_<PROTO_TYPE_OFX>`). Moneydance merge copies FiTxnId onto the survivor for bank/CC (not investment). Next import skips if that FITID is still on a live register parent. We never tested merge ourselves in this review.
- **Delete unconfirmed:** skip set is *live register FITIDs only*, so the next Import recreates. Desired.
- **Delete after confirm:** also recreates. Different from OFX (`OnlineTxnList` `STATUS_ACCEPTED` remembers). Product choice; document it. Not a blocker.
- **Empty parents:** we always `addSplit` before `addNewTxn`. No empty-parent path unless `makeSplitTxn` throws. Fallback can still split to **Root**, which is worse than empty.

---

## Findings

### Blocker 1 — `makeSplitTxn` likely inverts Lunch Flow signs

**File:** [`src/main/java/.../sync/MdAccess.java`](../src/main/java/com/moneydance/modules/features/lunchflow/sync/MdAccess.java) lines 247–258, 144–148 in `SyncEngine.addRegisterTxn`.

**Evidence**

We pass the Lunch Flow signed minor-unit amount as both longs:

```247:258:src/main/java/com/moneydance/modules/features/lunchflow/sync/MdAccess.java
        Account category = fallbackCategory(book, amount > 0);
        SplitTxn split = SplitTxn.makeSplitTxn(
            parent,
            amount,
            amount,
            1.0,
            category,
            note,
            -1L,
            AbstractTxn.STATUS_UNRECONCILED
        );
        parent.addSplit(split);
```

`javap -c` on `SplitTxn$Companion.makeSplitTxn(ParentTxn, long parentAmount, long splitAmount, double rate, …)`:

```
setAmount(splitAmount, rate, -parentAmount)
```

`SplitTxn.setAmount(long, double, long)` stores the **third** argument in `parentAmount`. `ParentTxn.getValue()` is the **sum of split `getParentAmount()`**.

So for a Lunch Flow coffee of `-12.50` (£, 2 decimals → `-1250`):

| | Value |
| --- | --- |
| `parentAmount` argument | `-1250` |
| stored split `parentAmount` | `-(-1250)` = `+1250` |
| `ParentTxn.getValue()` | `+1250` |
| Bank register | **deposit**, not payment |

IK apidoc for `makeSplitTxn`: *“parentAmount having a **negative** effect on the account of parentTxn, and splitAmount having a **positive** effect on the account of this SplitTxn.”* The factory implements that by negating the parent long. Passing an already-negative cashflow double-applies the sign.

`fallbackCategory(book, amount > 0)` still uses the **Lunch Flow** sign, so the coffee is booked as **Expense** while the bank side is a **deposit**. Reports, net income, and balances will all be wrong even if the row “appears”.

`snapshotFromParent` then reads `getValue()` back as the Lunch Flow amount (`SyncEngine.kt` 224–235). Pending→posted unique match requires **exact signed amount**. If the register is inverted, snapshot is `+12.50` vs posted `-12.50` → **promotions never fire**. The engine falls through to delete-pending + add-posted, which looks like set-reconcile “working” while discarding any category the user set.

**Why it matters:** silent inversion on every import. The throwaway file already has 133 Savings + 50 Lloyds register FITIDs; if those were built with this factory, the file is already inverted. “v19 appeared in the register” does not prove the Payment column is correct.

**Suggested fix**

1. On the throwaway file, open one known purchase. If it sits in **Deposit**, this finding is confirmed. Do not import again until fixed.
2. Pass the **negation** of the account cashflow into the factory, keep Lunch Flow sign for category selection:

```java
long cashflow = amount; // LF signed: negative = money leaving the bank/CC
Account category = fallbackCategory(book, cashflow > 0);
SplitTxn split = SplitTxn.makeSplitTxn(
    parent,
    -cashflow,  // factory will negate → Parent.getValue() == cashflow
    -cashflow,  // split.getValue() positive on expense, negative on income
    1.0,
    category,
    note,
    -1L,
    AbstractTxn.STATUS_UNRECONCILED
);
```

3. After the fix, **do not** re-import on top of inverted rows. Either reset the throwaway file or one-shot negate existing `lunchflow:` parents.
4. Add a unit-testable helper `registerAmounts(lfSigned) → (parentArg, splitArg, incomeCategory)` so this cannot regress. The factory itself cannot be unit-tested without MD, but the sign mapping can.
5. Separately confirm credit-card payloads. MD CC charges are also negative `getValue()`. If a Lunch Flow card feed sends charges as positive “amount spent”, we need an explicit per-type (or per-mapping) invert — do not guess.

---

### Blocker 2 — Pending cleanup deletes confirmed register txns

**File:** [`src/main/kotlin/.../sync/SyncEngine.kt`](../src/main/kotlin/com/moneydance/modules/features/lunchflow/sync/SyncEngine.kt) lines 46–52 and 119–123.

**Evidence**

```46:52:src/main/kotlin/com/moneydance/modules/features/lunchflow/sync/SyncEngine.kt
        val pendingRegister = mutableMapOf<String, ParentTxn>()
        for (txn in MdAccess.txnsForAccount(book, mdAccount)) {
            val parent = MdAccess.findByFitId(book, mdAccount, FitIds.PROTOCOL, MdAccess.registerFiTxnId(txn, FitIds.PROTOCOL))
                ?: continue
            val id = MdAccess.registerFiTxnId(parent, FitIds.PROTOCOL)
            if (FitIds.isPending(id) && id != null) pendingRegister[id] = parent
        }
```

```119:123:src/main/kotlin/com/moneydance/modules/features/lunchflow/sync/SyncEngine.kt
        for ((key, existing) in pendingRegister) {
            if (key in promotedPendingKeys) continue
            MdAccess.deleteTxn(existing)
            pendingRemoved++
        }
```

There is **no** `parent.isNew()` (or `getBooleanParameter("lunchflow.pending")`) check. Any live register parent whose OFX FITID starts with `lunchflow:pending:` is treated as a disposable hold.

Product / `AGENTS.md` / `docs/architecture.md` all say: *pending set-reconcile only our unconfirmed rows; never delete a confirmed register txn. If the user confirms a pending hold, leave it; the later posted row may appear as another blue dot to merge.*

What the code does when a confirmed pending disappears from Lunch Flow (posted, amount changed, or dropped):

1. Unique match → rewrite FITID/description **in place** (keeps confirmed — accidentally OK).
2. No unique match → **`deleteItem()` on the confirmed parent**, then insert a new unconfirmed posted row.

User loses the row they accepted, plus any category/memo they set. Infinite Kind will fail the audit on “does not brick data files”.

**Suggested fix**

- Index pending candidates as: `parent.account == mappedAccount` **and** `FitIds.isPending(fitId)` **and** `parent.isNew()`.
- If a pending FITID exists on a **confirmed** parent: leave it. If a posted twin arrives, add the posted unconfirmed row (user merges). Never `deleteItem` a non-new parent.
- Same guard on `labelRegisterPending`: do not rewrite description on confirmed rows (see High 2).

---

### Blocker 3 — Pending map can capture another account’s parent and delete it

**File:** `SyncEngine.kt` 46–52; `MdAccess.findByFitId` / `registerFiTxnId` at `MdAccess.java` 179–195.

**Evidence**

`TransactionSet.getTransactionsForAccount(account)` returns **every** `AbstractTxn` that touches the account: parents *in* the account **and** splits *in* the account whose parent lives elsewhere (transfers).

`registerFiTxnId` always walks to `txn.getParentTxn()`. `findByFitId` does the same. So while syncing Savings, a split whose parent is a Current `lunchflow:pending:…` parent is entered into **Savings’** `pendingRegister` as that Current parent.

Savings’ `desiredPending` will not contain Current’s key → the loop at 119–123 calls `deleteItem()` on the **Current** parent.

This fires as soon as the user (or merge) turns a Lunch Flow import into an inter-account transfer, or any mapped account receives a split from another mapped Lunch Flow parent.

**Suggested fix**

When iterating `txnsForAccount`, keep the txn only if it **is** a `ParentTxn` **and** `parent.account == mdAccount`. Do not follow splits to foreign parents. `findByFitId` should use the same filter (and should not be O(n²) — see Medium).

---

### High 1 — API key stored in plaintext local storage

**File:** [`src/main/kotlin/.../settings/SettingsStore.kt`](../src/main/kotlin/com/moneydance/modules/features/lunchflow/settings/SettingsStore.kt) lines 19–27.

```19:27:src/main/kotlin/com/moneydance/modules/features/lunchflow/settings/SettingsStore.kt
    fun setApiKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            clearApiKey()
            return
        }
        setAuth(API_KEY, trimmed)
        setPlain(API_KEY, trimmed)
    }
```

`fromBook` maps `setAuth` → `LocalStorage.cacheAuthentication` (the credential API) and `setPlain` → `storage.put` + `save()`. The unit test **asserts** the plaintext copy exists.

`AGENTS.md` / `SECURITY.md`: store the key with the most protected per-file mechanism; never world-readable sidecar. Dual-write is the opposite of that. Infinite Kind’s first audit question is “where is the key?”

**Suggested fix:** write only `cacheAuthentication`. Keep `getPlain` as a one-time migration read: if auth is empty and plain is set, move it to auth and `remove` the plain key. Stop asserting plaintext in tests.

---

### High 2 — Confirmed pending descriptions get `[PENDING]` re-applied

**File:** `SyncEngine.kt` 100–114, 208–213.

`labelRegisterPending` runs for every desired pending still on the register, including confirmed ones. If the user confirmed and edited the payee (or Confirm stripped the prefix), the next Import prepends `[PENDING]` again and sets `lunchflow.pending`.

`AbstractTxn.autoSyncsChanges()` is **false**. `setDescription` calls `syncItem()`; `setParameter` after that may **not** persist. So we mutate the visible payee (persisted) and maybe drop the hidden flag (not persisted). Worst of both.

**Suggested fix:** only label `isNew()` parents. After any parameter change, `syncItem()`. On promote, `removeParameter("lunchflow.pending")` and `syncItem()`.

---

### High 3 — Investment (and loan) accounts are mappable

**File:** [`src/main/kotlin/.../sync/MdAccounts.kt`](../src/main/kotlin/com/moneydance/modules/features/lunchflow/sync/MdAccounts.kt) lines 7–14.

`MAPPABLE` includes `INVESTMENT` and `LOAN`. Import always builds `TRANSFER_TYPE_BANK` + a single category split. That is not an investment txn (needs security split, `InvestTxnType`, quantity/price). IK docs: merge is **not** available on investment or loan.

A user mapping a brokerage Lunch Flow account into an investment register will get junk bank rows that are painful to undo.

**Suggested fix:** v1 mappable = `BANK`, `CREDIT_CARD`, maybe `ASSET`/`LIABILITY`. Reject the mapping with a clear error if the MD account type cannot take a bank split. Holdings are a later phase (`docs/roadmap.md`).

---

### High 4 — Fallback split can attach to Root

**File:** `MdAccess.java` 278–289, 297.

`walkCategory` looks for a name containing `"unspecif"`. That matches English “Unspecified”. It misses “Non spécifié”, “Sin especificar”, “Nicht spezifiziert”, locale-renamed categories, etc. Then first income/expense, then **`book.getRootAccount()`**.

A split to Root is a known Moneydance footgun (balances/reports/sidebar). Marketplace reviewers will try a non-English file.

**Suggested fix:** prefer `Account.AccountType` Unspecified via MD’s own default-category helpers if the jar exposes them; otherwise search case-insensitive `unspecif` **and** common translations; **never** return Root — fail the account with “Create an Unspecified expense category” instead of writing a bad txn.

---

### High 5 — Docs still describe the discarded OnlineTxn import

**Files:** `AGENTS.md` (hard rule: “Import via `OnlineTxn` / `account.getDownloadedTxns()`, not by creating `ParentTxn`s”); `docs/architecture.md` § “Sync algorithm” steps 4–6 still say `downloaded.newTxn()`; `docs/product.md` still says “unconfirmed downloaded transactions” / blue-dot OFX queue; `docs/roadmap.md` phase 3 still checked as OnlineTxn.

Agents following `AGENTS.md` will “fix” v19 back to hidden `OnlineTxn`s — which is exactly the Current-account failure mode (0 register rows, 3 NEW downloads, yellow Download = OFX signup).

**Suggested fix:** one import story everywhere: unconfirmed **register** `ParentTxn` + `SplitTxn`; `OnlineTxnList` is leftover conversion only. Do it in the same change as the sign/pending fixes.

---

### Medium 1 — Promote does not update date, amount, or pending flag

**File:** `SyncEngine.kt` 76–84.

Unique match rewrites description + FITID only. Pending vs posted may differ by up to 7 days. `lunchflow.pending` stays true. Amount is required equal so skip is OK **if** signs are fixed; date is not.

**Fix:** `setDateInt` / `setTaxDateInt` from posted; `removeParameter("lunchflow.pending")`; `syncItem()`.

---

### Medium 2 — `findByFitId` is O(n²) and used as an identity function

**File:** `SyncEngine.kt` 47–49; `MdAccess.java` 184–195.

For every txn in the account we re-scan the whole account to find the parent we already have from `registerFiTxnId(txn)`. With 133+ rows this is merely slow; with transfers it is wrong (Blocker 3). Replace with “if `txn` is `ParentTxn` and account matches, use it”.

---

### Medium 3 — Materialize skips undated OnlineTxns forever

**File:** `SyncEngine.kt` 176–180.

Blank FITID → remove. `dateInt <= 0` → `continue` **without** adding to `toRemove`. Those leftovers sit in `OnlineTxnList` forever (still invisible on non-OFX banks).

**Fix:** skip create, but still remove (or log and remove). Use `getAmount(account)` if we keep conversion at all — `getAmount()` is raw and `getAmount(Account)` applies OFX txn-type sign tables. Our leftovers were written with raw LF amounts, so raw is probably right **once** factory signs are fixed.

---

### Medium 4 — Two Lunch Flow accounts mapped onto one MD account

Nothing prevents it. FITIDs include `accountId` so posted skip still works, but pending cleanup is per mapped MD account and will delete the other mapping’s pending keys (they are not in *this* `desiredPending`). Combine with Blocker 2.

**Fix:** on Save mappings / Import, reject duplicate `moneydanceAccountUuid` (or merge pending sets across mappings that share a UUID).

---

### Medium 5 — Inactive Lunch Flow connections still import

`LunchFlowAccount.isActive` exists and is unused. Expired/paused connections will keep producing last-known (or error) data with no status in the mapping table.

**Fix:** show `status` in the mapping UI; skip or warn when not `ACTIVE`.

---

### Medium 6 — Txn-level currency ignored; double→minor is `Math.round`

Account-level mismatch is a hard error (`SyncEngine.kt` 32–38). Good. A single txn with a different `currency` is still imported as account currency. `CurrencyType.getLongValue` is `round(major * centMult)` — acceptable; not exact decimal. Prefer documenting that we trust Lunch Flow’s account currency.

---

### Medium 7 — Fetch window always starts at mapping start date

`fetchFromDate` returns `syncStartDate` whenever it is set, ignoring `lastPostedDate`. Intentional per `AGENTS.md` (FITID skip; don’t hide earlier history). Cost: every Import re-pulls the whole window. Parser ignores response `total`. If Lunch Flow ever pages or caps, pending set-reconcile will treat missing holds as vanished and **delete** them (Blocker 2 makes that lethal).

**Fix:** compare `transactions.size` to `total`; if short, **abort the account** (do not delete pending). Do not page-blind set-reconcile.

---

### Medium 8 — No `setOriginalOnlineTxn`; Confirm worked once

IK 2012–2013: Confirm NPE without `ol.orig_txn`. Claimed fixed. Live v19 Confirm worked without it. Merge/auto-categorise may still want an original payload.

**Fix (optional but cheap):** when materializing a leftover `OnlineTxn`, `parent.setOriginalOnlineTxn(row)` before removing it. For fresh imports, either skip (we are not OFX) or synthesise a minimal `OnlineTxn` with name/amount/date/FITID so Similar Payees has a snapshot.

---

### Medium 9 — Dead `OnlineTxn` write API

`MdAccess.newTxn` / `setNew` / `setProtocolType` / `setPending` / … is unused by `SyncEngine.apply`. Leaving it invites the next change to start writing hidden downloads again.

**Fix:** delete the write helpers; keep list read/remove/sync for leftover conversion only.

---

### Medium 10 — Marketplace / UX gaps that will stall listing

- Help still opens Lunch Flow destination docs, not a 101 setup guide (`LunchFlowWindow.DOCS_URL`). `AGENTS.md` says this is a stopgap, not final.
- No separate **Test connection** (Refresh accounts is the test). `docs/marketplace.md` checklist wants one.
- `showAccount` is computed after import and **never used** — we do not open the register.
- No progress, cancel, or per-account last-error persistence.
- `meta_info.dict` has no `vendor_url` (marketplace.md says IK expects it).
- `printStackTrace` on sync failure goes to stderr (OK) but there is no user-facing “open Help → Console”.
- Third-party disclaimer **is** on the window and in `module_desc`. Keep it.

None of these are txn-type bugs; they are why this is not a marketplace drop even after blockers.

---

### Low 1 — `FitIds.isOurs` treats pending as posted

`PREFIX_POSTED = "lunchflow:"` is a prefix of `lunchflow:pending:`. `isOurs` is unused in the engine today. If someone uses it for skip logic later, pending and posted collapse. Use `startsWith(PREFIX_POSTED) && !isPending(v)`.

---

### Low 2 — Synth pending keys churn

Null Lunch Flow pending ids hash date/amount/currency/merchant/description. Any field change is drop+recreate (documented). Fine for unconfirmed; lethal if combined with Blocker 2 on a confirmed synth pending.

---

### Low 3 — Posted rows with null `id` are silently dropped

`postedLf` filters `!id.isNullOrBlank()`. No counter. If the API ever posts without id, we skip forever with no UI.

---

### Low 4 — `PendingMatch` uses `Double` equality and `LocalDate.parse`

Exact `!=` on doubles; `parse` throws on a bad date and aborts the whole `done()` apply for remaining accounts. Dates from our parser are `YYYY-MM-DD`. Amounts should be compared in minor units once signs are fixed.

---

### Low 5 — Threading is actually fine

HTTP + parse in `SwingWorker.doInBackground`; `SyncEngine.apply` / `addNewTxn` / `deleteItem` / mapping saves in `done()` (EDT). Feature register in `init()`, window on `invokeLater`. `md:file:closing` / `unload` drop the window. This is the one area that matches the architecture doc.

Do **not** move `addNewTxn` off the EDT without following an IK sample; historical extensions wrapped it on the UI thread and disabled balance recalc for bulk inserts (`root.setRecalcBalances(false)`). Worth doing if we import thousands of rows; not a correctness bug at current sizes.

---

## Check list vs the ten review questions

| # | Question | Answer |
| --- | --- | --- |
| 1 | Parent / Split / Online vs IK conventions | Skeleton is IK-correct (`makeParentTxn` + `addSplit` + `addNewTxn` + `setIsNew` + OFX FITID). `makeSplitTxn` signs are not. OnlineTxn is leftover-only (good) with dead write helpers (bad). |
| 2 | Amount sign | **Likely inverted.** Factory stores `-parentAmount`. Category uses LF sign. CC vs bank not specialised. **Verify Payment vs Deposit on one known spend before any further import.** |
| 3 | Unconfirmed Confirm/Merge/delete | Confirm: `setIsNew(true)` is correct; worked in live test; no `originalOnlineTxn`. Merge: FITID on parent should copy; untested here. Delete unconfirmed: recreates (desired). Delete confirmed: also recreates (document). |
| 4 | Hidden OnlineTxn leftovers | `materializeHiddenDownloads` converts missing-from-register rows and removes them. Undated rows stick. We do not write new OnlineTxns in `apply()`. |
| 5 | Pending set-reconcile | Unique promote in place is the right idea. **Deletes confirmed.** **Can delete another account’s parent.** Label mutates confirmed payees. |
| 6 | FITID uniqueness | Posted `lunchflow:{accountId}:{txnId}`, pending `lunchflow:pending:{accountId}:{id\|synth:…}`, protocol OFX. Skip is live register only (correct for this product). `isOurs` is prefix-wrong. |
| 7 | Threading | HTTP off EDT, model writes on EDT. Good. |
| 8 | Currency / start date / Account Access | Currency mismatch = hard error. Start date is fetch floor. Account Access copy is in the mapping panel and empty-account status. Inactive LF status unused. |
| 9 | Duplicate / drop / wrong account / empty parent | Duplicates: FITID skip OK unless two mappings share an MD account. Drop: pending delete too aggressive. Wrong account: Blocker 3. Empty parent: no; Root split: yes. |
| 10 | IK marketplace audit | Key dual-write, deleting confirmed txns, sign inversion, investment mapping, stale “we import OnlineTxns” docs, missing 101 help, no Test connection, no `vendor_url`. Fail. |

---

## What is already right (so we do not “fix” it)

- **Register `ParentTxn`, not `OnlineTxnList`, as the user-visible import.** That was the actual v18/v19 bug. Keep it.
- FITID namespace and OFX protocol.
- `setIsNew(true)` + `STATUS_UNRECONCILED` + uncleared; do not force Cleared.
- `addNewTxn(parent)` (not split-only `syncItem()`).
- HTTP / JSON off the EDT.
- Currency mismatch refused.
- Start date honored as fetch floor; FITID skip for posted.
- Account Access called out in the mapping UI.
- Third-party disclaimer on the window and in `module_desc`.
- Key not logged (`System.err` prints exception class only).
- Pending unique-match rules (exact amount, merchant, ≤7 days, 1:1) are sensible **once amounts have the right sign**.
- Leftover `OnlineTxn` conversion then `removeTxn` + `syncList` + `downloadedTxnsUpdated`.

---

## Suggested fix order (do not ship between 1 and 3)

1. **Prove amount sign** on the throwaway file (one known purchase). If inverted, change `makeSplitTxn` args to `-cashflow, -cashflow` and reset or negate existing lunchflow parents. Do not keep importing on inverted data.
2. **Pending ownership:** only `isNew()` parents whose `getAccount()` is the mapped account. Never `deleteItem` confirmed. Never follow splits to foreign parents.
3. **Stop plaintext API key writes.**
4. Drop investment/loan from mappable; never split to Root.
5. Align `AGENTS.md` + architecture/product with the ParentTxn import. Delete unused OnlineTxn write helpers.
6. Only then: Help 101, Test connection, `vendor_url`, pagination/`total` guard, promote date/flag updates.

Until 1–3 are done, treat the throwaway file as contaminated: inverted amounts and any confirmed pending are one Import away from deletion.
