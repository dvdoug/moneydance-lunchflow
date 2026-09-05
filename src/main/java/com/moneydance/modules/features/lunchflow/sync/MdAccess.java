package com.moneydance.modules.features.lunchflow.sync;

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.infinitekind.moneydance.model.AbstractTxn;
import com.infinitekind.moneydance.model.CurrencyType;
import com.infinitekind.moneydance.model.OnlineTxn;
import com.infinitekind.moneydance.model.OnlineTxnList;
import com.infinitekind.moneydance.model.ParentTxn;
import com.infinitekind.moneydance.model.SplitTxn;
import com.infinitekind.moneydance.model.TransactionSet;
import com.infinitekind.moneydance.model.TxnSet;

/** Java facades for Kotlin-private properties that still have public getters on the bytecode. */
public final class MdAccess {
    private MdAccess() {}

    public static Account rootAccount(AccountBook book) {
        return book.getRootAccount();
    }

    public static Account.AccountType accountType(Account account) {
        return account.getAccountType();
    }

    public static boolean isInactive(Account account) {
        return account.getAccountIsInactive();
    }

    public static int subAccountCount(Account account) {
        return account.getSubAccountCount();
    }

    public static String fullAccountName(Account account) {
        return account.getFullAccountName();
    }

    public static String uuid(Account account) {
        return account.getUUID();
    }

    public static CurrencyType currency(Account account) {
        return account.getCurrencyType();
    }

    public static String currencyId(Account account) {
        return account.getCurrencyType().getIDString();
    }

    public static OnlineTxnList downloadedTxns(Account account) {
        return account.getDownloadedTxns();
    }

    public static int txnCount(OnlineTxnList list) {
        return list.getTxnCount();
    }

    public static OnlineTxn txnAt(OnlineTxnList list, int index) {
        return list.getTxn(index);
    }

    public static OnlineTxn newTxn(OnlineTxnList list) {
        int before = list.getTxnCount();
        OnlineTxn txn = list.newTxn();
        if (list.getTxnCount() == before) {
            list.addNewTxn(txn);
        }
        return txn;
    }

    public static void notifyDownloaded(OnlineTxnList list) {
        list.notifyOnlineListeners();
    }

    public static void removeTxn(OnlineTxnList list, OnlineTxn txn) {
        list.removeTxn(txn);
    }

    public static void sortTxns(OnlineTxnList list) {
        list.sortTransactions();
    }

    public static void syncList(OnlineTxnList list) {
        list.syncItem();
    }

    public static void downloadedUpdated(Account account) {
        account.downloadedTxnsUpdated();
    }

    public static TransactionSet transactionSet(AccountBook book) {
        return book.getTransactionSet();
    }

    public static TxnSet txnsForAccount(AccountBook book, Account account) {
        return book.getTransactionSet().getTransactionsForAccount(account);
    }

    public static String fiTxnId(OnlineTxn txn) {
        return txn.getFITxnId();
    }

    public static int localStatus(OnlineTxn txn) {
        return txn.getLocalStatus();
    }

    public static void setNew(OnlineTxn txn) {
        txn.setLocalStatus(OnlineTxn.STATUS_NEW);
    }

    public static boolean isAcceptedDownload(OnlineTxn txn) {
        return txn.getLocalStatus() == OnlineTxn.STATUS_ACCEPTED;
    }

    public static void setFiTxnId(OnlineTxn txn, String id) {
        txn.setFITxnId(id);
    }

    public static void setProtocolType(OnlineTxn txn, int type) {
        txn.setProtocolType(type);
    }

    public static void setAmount(OnlineTxn txn, long amount) {
        txn.setAmount(amount);
    }

    public static long getAmount(OnlineTxn txn) {
        return txn.getAmount();
    }

    public static void setName(OnlineTxn txn, String name) {
        txn.setName(name);
    }

    public static String getName(OnlineTxn txn) {
        return txn.getName();
    }

    public static void setMemo(OnlineTxn txn, String memo) {
        txn.setMemo(memo);
    }

    public static String getMemo(OnlineTxn txn) {
        return txn.getMemo();
    }

    public static void setMerchantName(OnlineTxn txn, String name) {
        txn.setMerchantName(name);
    }

    public static String getMerchantName(OnlineTxn txn) {
        return txn.getMerchantName();
    }

    public static void setDatePostedInt(OnlineTxn txn, int dateInt) {
        txn.setDatePostedInt(dateInt);
    }

    public static int getDatePostedInt(OnlineTxn txn) {
        return txn.getDatePostedInt();
    }

    public static void setDateInitiatedInt(OnlineTxn txn, int dateInt) {
        txn.setDateInitiatedInt(dateInt);
    }

    public static void setPending(OnlineTxn txn, boolean pending) {
        txn.setPending(pending);
    }

    public static void setIsoCurrency(OnlineTxn txn, String iso) {
        txn.setISOCurrencyCode(iso);
    }

    public static String getIsoCurrency(OnlineTxn txn) {
        return txn.getISOCurrencyCode();
    }

    public static OnlineTxn addDownload(
        Account account,
        int dateInt,
        long amount,
        String name,
        String memo,
        String fitId,
        boolean pending,
        String isoCurrency
    ) {
        OnlineTxnList list = account.getDownloadedTxns();
        OnlineTxn txn = newTxn(list);
        fillDownload(txn, dateInt, amount, name, memo, fitId, pending, isoCurrency);
        setNew(txn);
        return txn;
    }

    public static void fillDownload(
        OnlineTxn txn,
        int dateInt,
        long amount,
        String name,
        String memo,
        String fitId,
        boolean pending,
        String isoCurrency
    ) {
        String payee = name == null ? "" : name;
        txn.setProtocolType(OnlineTxn.PROTO_TYPE_OFX);
        txn.setFITxnId(fitId);
        txn.setAmount(amount);
        txn.setName(payee);
        txn.setMerchantName(payee);
        txn.setMemo(memo == null ? "" : memo);
        txn.setDatePostedInt(dateInt);
        txn.setDateInitiatedInt(dateInt);
        txn.setPending(pending);
        if (isoCurrency != null && !isoCurrency.isEmpty()) {
            txn.setISOCurrencyCode(isoCurrency);
        }
    }

    public static String registerFiTxnId(AbstractTxn txn, int protocol) {
        ParentTxn parent = txn instanceof ParentTxn ? (ParentTxn) txn : txn.getParentTxn();
        return parent.getFiTxnId(protocol);
    }

    public static ParentTxn findByFitId(AccountBook book, Account account, int protocol, String fitId) {
        if (fitId == null || fitId.isEmpty()) {
            return null;
        }
        for (AbstractTxn txn : book.getTransactionSet().getTransactionsForAccount(account)) {
            if (!(txn instanceof ParentTxn)) {
                continue;
            }
            ParentTxn parent = (ParentTxn) txn;
            if (!sameAccount(parent.getAccount(), account)) {
                continue;
            }
            if (fitId.equals(parent.getFiTxnId(protocol))) {
                return parent;
            }
        }
        return null;
    }

    public static Account accountOf(ParentTxn txn) {
        return txn.getAccount();
    }

    public static boolean isNew(ParentTxn txn) {
        return txn.isNew();
    }

    public static boolean sameAccount(Account a, Account b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        String left = a.getUUID();
        String right = b.getUUID();
        return left != null && left.equals(right);
    }

    public static void clearPendingFlag(ParentTxn txn) {
        txn.removeParameter("lunchflow.pending");
        txn.syncItem();
    }

    public static void setDescription(ParentTxn txn, String description) {
        txn.setDescription(description);
        txn.syncItem();
    }

    public static String getDescription(ParentTxn txn) {
        return txn.getDescription();
    }

    public static void setMemo(ParentTxn txn, String memo) {
        txn.setMemo(memo == null ? "" : memo);
        txn.syncItem();
    }

    public static int getDateInt(ParentTxn txn) {
        return txn.getDateInt();
    }

    public static long getValue(ParentTxn txn) {
        return txn.getValue();
    }

    /** One-split category only. Three-arg {@code setAmount}; the two-arg form negates parent. */
    public static boolean updatePendingParent(
        ParentTxn parent,
        long newParentAmount,
        String description,
        String memo
    ) {
        if (parent.getSplitCount() != 1) {
            return false;
        }
        SplitTxn split = parent.getSplit(0);
        Account dest = split.getAccount();
        if (dest != null && !dest.getAccountType().isCategory()) {
            return false;
        }
        parent.setEditingMode();
        split.setAmount(-newParentAmount, 1.0d, newParentAmount);
        parent.setDescription(description == null ? "" : description);
        parent.setMemo(memo == null ? "" : memo);
        parent.syncItem();
        return true;
    }

    public static long toMinorUnits(Account account, double major) {
        return account.getCurrencyType().getLongValue(major);
    }

    public static double toMajorUnits(Account account, long minor) {
        return account.getCurrencyType().getDoubleValue(minor);
    }

    public static void deleteTxn(ParentTxn txn) {
        txn.deleteItem();
    }

    public static void setRegisterFitId(ParentTxn txn, String fitId) {
        txn.setFiTxnId(OnlineTxn.PROTO_TYPE_OFX, fitId);
        txn.syncItem();
    }
}
