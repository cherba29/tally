package tally.statement;

import javax.annotation.Nullable;

import tally.core.Balance;
import tally.core.BudgetException;
import tally.core.Month;

/**
 * Abstraction for a financial statement for a period of time.
 */
public abstract class Statement {
  private final String name;
  private final Month month;  /** Period of time for the statement */
  @Nullable private final Balance startBalance;  /** Recorded start balance for the statement. */
  @Nullable private final Balance endBalance;  /** Recorded end balance for the statement. */
  private final long inFlows;  /** Total transaction inflows. */
  private final long outFlows;  /** Total transaction outflows. */
  private final long totalTransfers;  /** Amount transfered to other accounts by same owner. */
  private final long totalPayments;  /** Amount transfered to external entities. */
  private final long income;  /** Amount transfered from external entities. */

  protected Statement(Builder<?> builder) {
    name = builder.name;
    month = builder.month;
    inFlows = builder.inFlows;
    outFlows = builder.outFlows;
    totalTransfers = builder.totalTransfers;
    totalPayments = builder.totalPayments;
    income = builder.income;
    startBalance = builder.startBalance;
    endBalance = builder.endBalance;
  }

  public String getId() {
    return name.replaceAll("[ \n\t]", "_") + "_" + month;
  }

  public String getName() {
    return name;
  }

  public Month getMonth() {
    return month;
  }

  @Nullable
  public Balance getEndBalance() {
    return endBalance;
  }

  @Nullable
  public Balance getStartBalance() {
    return startBalance;
  }

  public long getInFlows() {
    return inFlows;
  }

  public long getOutFlows() {
    return outFlows;
  }

  /**
   * Total added/subtracted by existing transactions.
   */
  public long getAddSub() {
    return inFlows + outFlows;
  }

  @Nullable
  public Long getChange() {
    if (endBalance != null && startBalance != null) {
      return endBalance.getAmount() - startBalance.getAmount();
    }
    return null;
  }

  @Nullable
  public Double getPercentChange() {
    if (endBalance != null && startBalance != null) {
      return 100.0 * (endBalance.getAmount() - startBalance.getAmount()) / startBalance.getAmount();
    }
    return null;
  }

  @Nullable
  public Long getUnaccounted() {
    if (endBalance != null && startBalance != null) {
      return endBalance.getAmount() - startBalance.getAmount() - (inFlows + outFlows);
    }
    return null;
  }

  public abstract boolean isClosed();

  public long getTotalTransfers() {
    return totalTransfers;
  }

  public long getTotalPayments() {
    return totalPayments;
  }

  public long getIncome() {
    return income;
  }

  protected static abstract class Builder<B extends Builder<B>> {
    @Nullable private String name = null;
    @Nullable private Month month = null;
    private long inFlows = 0;
    private long outFlows = 0;
    private long totalTransfers = 0;
    private long totalPayments = 0;
    @Nullable private Balance startBalance = null;
    @Nullable private Balance endBalance = null;
    private long income = 0;

    /**
     * implementation must return this.
     */
    protected abstract B self();

    public B setName(String name) {
      this.name = name;
      return self();
    }

    public Month getMonth() {
      return month;
    }

    public B setMonth(Month month) {
      this.month = month;
      return self();
    }

    @Nullable public Balance getStartBalance() {
      return startBalance;
    }

    public B setStartBalance(@Nullable Balance startBalance) throws BudgetException {
      this.startBalance = startBalance;
      return self();
    }

    @Nullable public Balance getEndBalance() {
      return endBalance;
    }

    public B setEndBalance(@Nullable Balance endBalance) throws BudgetException {
      this.endBalance = endBalance;
      return self();
    }

    public B addInFlow(long inFlow) {
      if (inFlow > 0) {
        inFlows += inFlow;
      } else {
        outFlows += inFlow;
      }
      return self();
    }

    public B addOutFlow(long outFlow) {
      if (outFlow > 0) {
        inFlows += outFlow;
      } else {
        outFlows += outFlow;
      }
      return self();
    }

    public B addTransfer(long transfer) {
      totalTransfers += transfer;
      return self();
    }

    public B addPayment(long payment) {
      totalPayments += payment;
      return self();
    }

    public B addIncome(long cents) {
      income += cents;
      return self();
    }
  }
}
