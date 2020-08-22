package tally.statement;

import tally.core.Account;
import tally.core.Balance;

import javax.annotation.Nullable;

public class Transaction {
  private final Balance balance;
  private final String description;
  private final Account account;
  private final Type type;
  @Nullable private Long balanceFromStart;
  @Nullable private Long balanceFromEnd;

  public enum Type {
    UNKNOWN,
    EXPENSE,
    INCOME,
    TRANSFER
  }

  public Transaction(Balance balance, String description, Account account, Type type) {
    this.balance = balance;
    this.description = description;
    this.account = account;
    this.balanceFromStart = null;
    this.balanceFromEnd = null;
    this.type = type;
  }

  public Balance getBalance() {
    return balance;
  }
  public String getDescription() {
    return description;
  }
  public Account getAccount() {
    return account;
  }

  @Nullable
  public Long getBalanceFromStart() {
    return balanceFromStart;
  }

  @Nullable
  public Long getBalanceFromEnd() {
    return balanceFromEnd;
  }

  public void setBalanceFromEnd(@Nullable Long balanceFromEnd) {
    this.balanceFromEnd = balanceFromEnd;
  }

  public void setBalanceFromStart(@Nullable Long balanceFromStart) {
    this.balanceFromStart = balanceFromStart;
  }

  public boolean isExpense() {
    return type == Type.EXPENSE;
  }

  public boolean isIncome() {
    return type == Type.INCOME;
  }

  @Override
  public String toString() {
    return String.format("Transaction [account=%s, balance=%s, description=%s,"
            +" balanceFromStart=%s, balanceFromEnd=%s]",
        account, balance, description, balanceFromStart, balanceFromEnd);
  }
}
