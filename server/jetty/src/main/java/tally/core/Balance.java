package tally.core;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * Represents immutable monetary amount, its type and date of occurrence.
 */
public final class Balance implements Comparable<Balance> {
  public static enum Type {
    CONFIRMED,
    PROJECTED,
    AUTO_PROJECTED;

    public static Type combine(Type t1, Type t2) {
      return (t1.compareTo(t2) <= 0) ? t2 : t1;
    }
  }
  private final long amount;
  private final Date date;
  private final Type type;

  public static Balance negated(Balance balance) {
    return new Balance(-balance.amount, balance.date, balance.type);
  }

  public static Balance add(Balance balance1, Balance balance2) {
    Date maxDate = balance1.date.compareTo(balance2.date) < 0 ? balance2.date : balance1.date;
    return new Balance(balance1.amount + balance2.amount, maxDate,
        Type.combine(balance1.type, balance2.type));
  }

  public static Balance subtract(Balance balance1, Balance balance2) {
    return add(balance1, new Balance(-balance2.getAmount(), balance2.getDate(), balance2.getType()));
  }

  public Balance(long amount, Date date, Type type) {
    this.amount = amount;
    this.date = date;
    this.type = type;
  }

  public long getAmount() {
    return amount;
  }

  public Date getDate() {
    return date;
  }

  public Type getType() {
    return type;
  }

  public boolean isProjected() {
    return type.equals(Type.PROJECTED);
  }

  /**
   * Order by (date, amount, projected).
   */
  @Override
  public int compareTo(Balance other) {
    int eq = date.compareTo(other.date);
    if (eq != 0) {
      return eq;
    }
    eq = Long.compare(amount, other.amount);
    if (eq != 0) {
      return eq;
    }
    return type.compareTo(other.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, date, type);
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Balance)) {
      return false;
    }
    Balance other = (Balance) obj;
    return amount == other.amount && date.equals(other.date) && type.equals(other.type);
  }

  @Override
  public String toString() {
    return String.format("Balance{amount=%.2f, date=%2$tY-%2$tm-%2$te, projected=%3$s}",
        BigDecimal.valueOf(amount, 2), date, type);
  }
}
