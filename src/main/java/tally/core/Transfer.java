package tally.core;

import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class Transfer {
  private Account fromAccount;
  private Account toAccount;
  private Month fromMonth;
  private Month toMonth;
  private String description;
  private Balance balance;


  public static class Builder {
    @Nullable private Account fromAccount;
    @Nullable private Account toAccount;
    @Nullable private Month fromMonth;
    @Nullable private Month toMonth;
    @Nullable private String description;
    @Nullable private Balance balance;

    @Nullable private String accountToName;
    @Nullable private String accountFromName;

    private Builder() {
    }

    public Builder setAccountTo(Account account) {
      if (accountToName == null) {
        accountToName = account.getName();
      } else {
        checkArgument(accountToName.equals(account.getName()),
            "Account name " + account.getName() + " does not match previously set name "
                + accountToName);
      }
      this.toAccount = account;
      return this;
    }

    public Builder setAccountToName(String name) {
      this.accountToName = name;
      return this;
    }

    public String getAccountToName() {
      return accountToName;
    }

    public Builder setAccountFromName(String name) {
      this.accountFromName = name;
      return this;
    }

    public String getAccountFromName() {
      return accountFromName;
    }

    public Builder setAccountFrom(Account account) {
      if (accountFromName == null) {
        accountFromName = account.getName();
      } else {
        checkArgument(accountFromName.equals(account.getName()),
            "Account name " + account.getName() + " does not match previously set name "
                + accountFromName);
      }
      this.fromAccount = account;
      return this;
    }

    public Builder setFromMonth(Month month) {
      this.fromMonth = month;
      return this;
    }

    public Builder setToMonth(Month month) {
      this.toMonth = month;
      return this;
    }
    public Builder setBalance(Balance balance) {
      this.balance = balance;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    @Override
    public String toString() {
      return "Transfer.Builder{" +
          "fromAccount=" + fromAccount +
          ", toAccount=" + toAccount +
          ", fromMonth=" + fromMonth +
          ", toMonth=" + toMonth +
          ", description='" + description + '\'' +
          ", balance=" + balance +
          ", accountToName='" + accountToName + '\'' +
          ", accountFromName='" + accountFromName + '\'' +
          '}';
    }

    public Transfer build() {
      return new Transfer(this);
    }
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class OrderByDate implements Comparator<Transfer> {
    public int compare(Transfer o1, Transfer o2) {
      int eq = o1.balance.compareTo(o2.balance);
      if (eq != 0) {
        return eq;
      }
      eq = o1.fromMonth.compareTo(o2.fromMonth);
      if (eq != 0) {
        return eq;
      }
      eq = o1.toMonth.compareTo(o2.toMonth);
      if (eq != 0) {
        return eq;
      }
      eq = o1.fromAccount.getName().compareTo(o2.fromAccount.getName());
      if (eq != 0) {
        return eq;
      }
      eq = o1.toAccount.getName().compareTo(o2.toAccount.getName());
      if (eq != 0) {
        return eq;
      }
      return o1.description.compareTo(o2.description);
    }
  }

  private Transfer(Builder builder) {
    checkState(builder.fromAccount != null);
    fromAccount = builder.fromAccount;
    checkState(builder.fromMonth != null);
    fromMonth = builder.fromMonth;
    checkState(builder.toAccount != null);
    toAccount = builder.toAccount;
    checkState(builder.toMonth != null);
    toMonth = builder.toMonth;
    checkState(builder.description != null);
    description = builder.description;
    checkState(builder.balance != null);
    balance = builder.balance;
  }

  public Account getFromAccount() {
    return fromAccount;
  }

  public Account getToAccount() {
    return toAccount;
  }

  public Month getFromMonth() {
    return fromMonth;
  }

  public Month getToMonth() {
    return toMonth;
  }

  public String getDescription() {
    return description;
  }

  public Balance getBalance() {
    return balance;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Transfer transfer = (Transfer) o;

    return balance.equals(transfer.balance)
        && description.equals(transfer.description)
        && fromAccount.equals(transfer.fromAccount)
        && fromMonth.equals(transfer.fromMonth)
        && toAccount.equals(transfer.toAccount)
        && toMonth.equals(transfer.toMonth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromAccount, toAccount, fromMonth, toMonth, description, balance);
  }

  @Override
  public String toString() {
    return "Transfer [fromAccount=" + fromAccount + ", toAccount=" + toAccount
        + ", fromMonth=" + fromMonth + ", toMonth=" + toMonth + ", balance="
        + balance + "]";
  }
}
