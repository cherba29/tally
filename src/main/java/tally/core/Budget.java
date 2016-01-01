package tally.core;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Immutable container for all raw financial data.
 */
public class Budget {
  private final ImmutableList<Month> months;
  private final ImmutableList<Account> accounts;
  private final ImmutableTable<Account, Month, Balance> balances;
  private final ImmutableTable<Account, Month, ImmutableSortedMultiset<Transfer>> transfers;

  private Budget(Collection<Month> months, Collection<Account> accounts,
                 Table<Account, Month, Balance> balances,
                 Table<Account, Month, ImmutableSortedMultiset<Transfer>> transfers) {
    this.months = ImmutableList.copyOf(months);
    this.accounts = ImmutableList.copyOf(accounts);
    this.balances = ImmutableTable.copyOf(balances);
    this.transfers = ImmutableTable.copyOf(transfers);
  }

  public static class Builder {
    private final List<Month> months = new ArrayList<>();
    private final Map<String, Account.Builder> accountBuilders = new HashMap<>();
    private final ArrayList<Transfer.Builder> transferBuilders = new ArrayList<>();
    private final Table<String, Month, Balance> balances = HashBasedTable.create();
    private final Table<String, Month, ImmutableSortedMultiset.Builder<Transfer>> transfers
        = HashBasedTable.create();

    public void setPeriod(Month start, Month end) {
      for (Month month = end; month.compareTo(start) >= 0; month = month.previous()) {
        months.add(month);
      }
    }

    public Builder addAccountBuilder(Account.Builder account) {
      accountBuilders.put(account.getName(), account);
      return this;
    }

    public Builder addBalance(String accountName, Month month, Balance balance) {
      balances.put(accountName, month, balance);
      return this;
    }

    public void addBalances(String accountName, Map<Month, Balance> monthlyBalances) {
      for (Map.Entry<Month, Balance> entry : monthlyBalances.entrySet()) {
        balances.put(accountName, entry.getKey(), entry.getValue());
      }
    }

    @Nullable
    public Account.Builder getAccountByName(String name) {
      return accountBuilders.get(name);
    }

    public Builder addTransfer(Transfer transfer) {
      addTransfer(transfer.getToAccount().getName(), transfer.getToMonth(), transfer);
      addTransfer(transfer.getFromAccount().getName(), transfer.getFromMonth(), transfer);
      return this;
    }

    public Builder addTransferBuilder(Transfer.Builder transferBuilder) {
      transferBuilders.add(transferBuilder);
      return this;
    }

    public Builder addAllTransferBuilders(List<Transfer.Builder> transferBuilders) {
      this.transferBuilders.addAll(transferBuilders);
      return this;
    }

    private Builder addTransfer(String accountName, Month month, Transfer transfer) {
      ImmutableSortedMultiset.Builder<Transfer> accountMonthTransfers
          = transfers.get(accountName, month);
      if (accountMonthTransfers == null) {
        accountMonthTransfers = new ImmutableSortedMultiset.Builder<>(
            new Transfer.OrderByDate());
        transfers.put(accountName, month, accountMonthTransfers);
      }
      accountMonthTransfers.add(transfer);
      return this;
    }

    public Budget build() throws BudgetException {
      Map<String, Account> accounts = new HashMap<>();
      for (Map.Entry<String, Account.Builder> entry : accountBuilders.entrySet()) {
        accounts.put(entry.getKey(), entry.getValue().build());
      }
      for (Transfer.Builder transferBuilder : transferBuilders) {
        // Map transfer to accountTo and accountFrom.
        String accountToName = transferBuilder.getAccountToName();
        checkState(accountToName != null,
            "Transfer.Builder does not have AccountTo name set %s", transferBuilder);
        String accountFromName = transferBuilder.getAccountFromName();
        checkState(accountFromName != null);
        Account accountTo = accounts.get(accountToName);
        if (accountTo == null) {
          throw new IllegalStateException("Account " + accountToName
            + " does not exist, yet transfer references it in "
            + accountFromName);
        } else {
          transferBuilder.setAccountTo(accountTo);
        }

        Account accountFrom = accounts.get(accountFromName);
        if (accountFrom == null) {
          throw new IllegalStateException("Account " + accountFromName
              + " does not exist, yet transfer references it in "
              + accountToName);
        } else {
          transferBuilder.setAccountFrom(accountFrom);
        }
        Transfer transfer = transferBuilder.build();
        Month month = transfer.getFromMonth();

        if (!accountFrom.isOpen(month)) {
          throw new BudgetException("Account " + accountFromName + " is not open on " + month
              + " for transfer " + transfer);
        }

//        if (!month.equals(transfer.getToMonth())) {
//          throw new BudgetException("From month " + month + " must be same as to month "
//              + transfer.getToMonth() + " for transfer " + transfer);
//        }
        if (!accountTo.isOpen(month)) {
          throw new BudgetException("Account " + accountToName + " is not open on " + month
              + " for transfer " + transfer);
        }

        addTransfer(transfer);
      }

      Table<Account, Month, ImmutableSortedMultiset<Transfer>> builtTransfers
          = HashBasedTable.create();
      for (Table.Cell<String, Month, ImmutableSortedMultiset.Builder<Transfer>> cell
          : transfers.cellSet()) {
        Account account = accounts.get(cell.getRowKey());
        if (account == null) {
          throw new IllegalArgumentException("Could not find account " + cell.getRowKey());
        }
        builtTransfers.put(account, cell.getColumnKey(), cell.getValue().build());
      }

      Table<Account, Month, Balance> builtBalances = HashBasedTable.create();
      for (Table.Cell<String, Month, Balance> cell : balances.cellSet()) {
        Account account = accounts.get(cell.getRowKey());
        if (account == null) {
          throw new IllegalArgumentException("Could not find account " + cell.getRowKey());
        }
        builtBalances.put(account, cell.getColumnKey(), cell.getValue());
      }

      Collections.sort(months, new Comparator<Month>() {
        @Override
        public int compare(Month o1, Month o2) {
          return -o1.compareTo(o2);
        }
      });

      return new Budget(months, accounts.values(), builtBalances, builtTransfers);
    }

    public void mergeIn(Builder budgetBuilder) {
      months.addAll(budgetBuilder.months);
      accountBuilders.putAll(budgetBuilder.accountBuilders);
      transferBuilders.addAll(budgetBuilder.transferBuilders);
      balances.putAll(budgetBuilder.balances);
      transfers.putAll(budgetBuilder.transfers);
    }
  }

  public List<Month> getMonths() {
    return months;
  }

  public List<Account> getOpenAccounts(Collection<Month> months) {
    List<Account> openAccounts = new ArrayList<>();
    for (Account account : accounts) {
      if (account.isOpen(months)) {
        openAccounts.add(account);
      }
    }
    return openAccounts;
  }

  @Nullable
  public Balance getBalance(Account account, Month month) {
    return balances.get(account, month);
  }

  @Nullable
  public ImmutableSortedMultiset<Transfer> getTransfers(Account account, Month month) {
    return transfers.get(account, month);
  }

  @Override
  public String toString() {
    return "Budget{" +
        "months=" + months +
        ", numAccounts=" + accounts.size() +
        ", numTransfers=" + transfers.size() +
        '}';
  }

  public String toDebugString() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("Budget\n");
    stringBuilder.append("Months: ");
    stringBuilder.append(months.toString());
    stringBuilder.append("\nAccounts:\n");
    for (Account account : accounts) {
      stringBuilder.append(account.toDebugString(2));
    }
    return stringBuilder.toString();
  }
}
