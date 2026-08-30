# -*- coding: utf-8 -*-
# Jython 2.7 -- Window > Show Developer Console > Open Script
# Read-only. Paste the output back.

from com.infinitekind.moneydance.model import Account, OnlineTxn, ParentTxn

def status_name(code):
    if code == OnlineTxn.STATUS_NEW:
        return "NEW"
    if code == OnlineTxn.STATUS_ACCEPTED:
        return "ACCEPTED"
    return "OTHER(%s)" % code

def walk(acct, out):
    t = acct.getAccountType()
    if t in (
        Account.AccountType.BANK,
        Account.AccountType.CREDIT_CARD,
        Account.AccountType.ASSET,
        Account.AccountType.LIABILITY,
        Account.AccountType.LOAN,
        Account.AccountType.INVESTMENT,
    ):
        if not acct.getAccountIsInactive():
            out.append(acct)
    for i in range(acct.getSubAccountCount()):
        walk(acct.getSubAccount(i), out)

def fitid_of_register(txn):
    parent = txn if isinstance(txn, ParentTxn) else txn.getParentTxn()
    return parent.getFiTxnId(OnlineTxn.PROTO_TYPE_OFX)

book = moneydance.getCurrentAccountBook()
print("FILE", book.getName())
print("====")

ls = book.getLocalStorage()
print("mappings present:", ls.get("lunchflow.mappings") is not None)
print("apiKey in localStorage:", bool(ls.get("lunchflow.apiKey")))
print("====")

accounts = []
walk(book.getRootAccount(), accounts)

for acct in accounts:
    name = acct.getFullAccountName()
    downloaded = acct.getDownloadedTxns()
    dl_count = downloaded.getTxnCount() if downloaded is not None else 0
    reg = book.getTransactionSet().getTransactionsForAccount(acct)
    reg_count = reg.getSize()

    reg_fit = []
    it = reg.iterator()
    while it.hasNext():
        txn = it.next()
        fid = fitid_of_register(txn)
        if fid:
            reg_fit.append(fid)

    dl_new = 0
    dl_acc = 0
    dl_other = 0
    dl_fit = []
    if downloaded is not None:
        for i in range(dl_count):
            row = downloaded.getTxn(i)
            st = row.getLocalStatus()
            if st == OnlineTxn.STATUS_NEW:
                dl_new += 1
            elif st == OnlineTxn.STATUS_ACCEPTED:
                dl_acc += 1
            else:
                dl_other += 1
            fid = row.getFITxnId()
            if fid:
                dl_fit.append("%s/%s" % (status_name(st), fid))

    lf_reg = [x for x in reg_fit if x.startswith("lunchflow:")]
    lf_dl = [x for x in dl_fit if "lunchflow:" in x]

    if reg_count == 0 and dl_count == 0 and not lf_reg and not lf_dl:
        continue

    print("ACCOUNT", name)
    print("  register txns:", reg_count, " with FITID:", len(reg_fit), " lunchflow FITID:", len(lf_reg))
    print("  downloaded rows:", dl_count, " NEW:", dl_new, " ACCEPTED:", dl_acc, " OTHER:", dl_other)
    print("  downloaded lunchflow FITIDs:", len(lf_dl))
    if lf_reg:
        print("  sample register FITIDs:", ", ".join(lf_reg[:5]))
    if lf_dl:
        print("  sample download FITIDs:", ", ".join(lf_dl[:8]))
    print("----")

print("DONE")
