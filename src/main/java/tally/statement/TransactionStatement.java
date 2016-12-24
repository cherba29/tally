package tally.statement;

import static com.google.common.base.Preconditions.checkNotNull;
import tally.core.Account;
import tally.core.Balance;
import tally.core.BudgetException;
import tally.core.MissDatedBalanceException;
import tally.core.Transfer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TransactionStatement extends Statement {
  private final Account account;

  private final ImmutableList<Transaction> transactions;
  private final boolean coversPrevious;
  private final boolean coversProjectedPrevious;
  private final boolean hasProjectedTransfer;
  private final boolean isCovered;
  private final boolean isProjectedCovered;

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder extends Statement.Builder<Builder> {
    @Nullable private Account account = null;
    private boolean coversPrevious = false;
    private boolean coversProjectedPrevious = false;
    private boolean isCovered = false;
    private boolean isProjectedCovered = false;
    private boolean hasProjectedTransfer = false;
    private List<Transaction> transactions = new ArrayList<>();

    public Builder setAccount(Account account) {
      this.account = account;
      setName(account.getName());
      return this;
    }

    @Override
    public Builder setStartBalance(@Nullable Balance startBalance) throws BudgetException {
      super.setStartBalance(startBalance);

      if (startBalance != null) {
        long amount = startBalance.getAmount();
        for (int i = transactions.size() - 1; i >= 0; --i) {
          Transaction t = transactions.get(i);
          if (t.getBalance().getDate().compareTo(startBalance.getDate()) < 0) {
            throw new MissDatedBalanceException(String.format(
                "Balance %s %s for account %s starts after transaction %s/%s desc{%s}",
                getMonth(), startBalance, account.getName(),
                t.getAccount().getName(), t.getBalance(), t.getDescription()));
          }
          amount += t.getBalance().getAmount();
          t.setBalanceFromStart(amount);
        }
      }
      return this;
    }

    @Override
    public Builder setEndBalance(@Nullable Balance endBalance) throws BudgetException {
      if (endBalance != null) {
        if (!transactions.isEmpty()) {
          Transaction lastTransaction = transactions.get(transactions.size() - 1);
          if (endBalance.getDate().compareTo(lastTransaction.getBalance().getDate()) < 0) {
            throw new MissDatedBalanceException(String.format(
                "Account %s has %s end balance %s "
                    + "statement yet last transaction %s is past that.",
                account.getName(), getMonth(), endBalance, lastTransaction));
          }
          long amount = endBalance.getAmount();
          for (Transaction t : transactions) {
            amount -= t.getBalance().getAmount();
            t.setBalanceFromEnd(amount);
          }
        }
      }
      super.setEndBalance(endBalance);
      return this;
    }

    /**
     * @param transfers - sorted by date in increasing order.
     */
    public Builder setTransfers(ImmutableSortedMultiset<Transfer> transfers) {
      checkNotNull(account);
      hasProjectedTransfer = false;
      coversPrevious = false;
      coversProjectedPrevious = false;
      transactions = new ArrayList<>();

      for (Transfer t : transfers.descendingMultiset()) {
        hasProjectedTransfer |= t.getBalance().isProjected();
        Account otherAccount;
        Balance balance;
        Transaction.Type transactionType = Transaction.Type.UNKNOWN;
        if (t.getToAccount().equals(account)) {
          balance = t.getBalance();
          otherAccount = t.getFromAccount();
          super.addInFlow(balance.getAmount());
          transactionType = getTransactionType(otherAccount, account, balance.getAmount());
          attributeTransfer(transactionType, balance.getAmount());
        } else if (t.getFromAccount().equals(account)) {
          balance = Balance.negated(t.getBalance());
          otherAccount = t.getToAccount();
          super.addOutFlow(balance.getAmount());
          transactionType = getTransactionType(account, otherAccount, balance.getAmount());
          attributeTransfer(transactionType, balance.getAmount());
        } else {
          throw new IllegalStateException(
              String.format("Setting transfer (%s to %s) for %s account statement!",
                  t.getFromAccount(), t.getToAccount(), account));
        }
        if (!coversPrevious && balance.getAmount() > 0
            && t.getFromAccount().hasCommonOwner(account)) {
          coversProjectedPrevious = true;
          if (!balance.isProjected()) {
            coversPrevious = true;
          }
        }
        transactions.add(
            new Transaction(balance, t.getDescription(), otherAccount, transactionType));
      }

      return this;
    }

    public Builder setIsCovered(boolean isCovered) {
      this.isCovered = isCovered;
      return this;
    }

    public Builder setIsProjectedCovered(boolean isCovered) {
      this.isProjectedCovered = isCovered;
      return this;
    }

    public TransactionStatement build() {
      checkNotNull(account);
      setName(account.getName());
      return new TransactionStatement(this);
    }

    private Transaction.Type getTransactionType(
        Account fromAccount, Account toAccount, long amount) {
      if (toAccount.hasCommonOwner(fromAccount)
          && !toAccount.isExternal()
          && !fromAccount.isExternal()) {
        return Transaction.Type.TRANSFER;
      } else {
        return (amount > 0) ? Transaction.Type.INCOME : Transaction.Type.EXPENSE;
      }
    }

    private void attributeTransfer(Transaction.Type transactionType, long amount) {
      switch (transactionType) {
        case EXPENSE:
          super.addPayment(amount);
          break;
        case INCOME:
          super.addIncome(amount);
          break;
        case UNKNOWN:
          break;
        case TRANSFER:
          super.addTransfer(amount);
          break;
      }
    }

    @Override
    protected Builder self() {
      return this;
    }
  }

  private TransactionStatement(Builder builder) {
    super(builder);
    account = builder.account;
    if (builder.transactions != null) {
      transactions = ImmutableList.copyOf(builder.transactions);
    } else {
      transactions = ImmutableList.of();
    }
    hasProjectedTransfer = builder.hasProjectedTransfer;
    coversPrevious = builder.coversPrevious;
    coversProjectedPrevious = builder.coversProjectedPrevious;
    isCovered = builder.isCovered;
    isProjectedCovered = builder.isProjectedCovered;
  }

  public Account getAccount() {
    return account;
  }

  public boolean isHasProjectedTransfer() {
    return hasProjectedTransfer;
  }


  /**
   * True if there exist positive non-projected transfer in this statement.
   */
  public boolean coversPrevious() {
    return coversPrevious;
  }

  /**
   * True if there exist positive transfer in this statement.
   */
  public boolean coversProjectedPrevious() {
    return coversProjectedPrevious;
  }

  /**
   * True if does not need to be covered or is covered.
   */
  public boolean isCovered() {
    return getEndBalance() == null
        || getEndBalance().getAmount() >= 0
        || isCovered;
  }

  /**
   * True if covered or is covered with projected transfer.
   */
  public boolean isProjectedCovered() {
    return isCovered()
        || isProjectedCovered;
  }

  /**
   * True if Account is closed for this month.
   */
  @Override
  public boolean isClosed() {
    return !account.isOpen(getMonth());
  }

  /**
   * @return List of transactions.
   */
  public ImmutableList<Transaction> getTransactions() {
    return transactions;
  }
}
