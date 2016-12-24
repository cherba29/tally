package tally.statement;

import tally.core.Balance;
import tally.core.BudgetException;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class SummaryStatement extends Statement {
  private final ImmutableList<Statement> statements;
  private final boolean isClosed;

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder extends Statement.Builder<Builder> {
    private List<Statement> statements = new ArrayList<>();
    private boolean isClosed = true;


    public Builder addStatement(Statement statement) throws BudgetException {
      if (statement.isClosed()) {
        return this;
      }
      if (statements.isEmpty()) {
        setMonth(statement.getMonth());
      } else if (getMonth() != statement.getMonth()) {
        throw new BudgetException(String.format(
            "Statement for month %s is being added to summary for month %s",
            getMonth(), statement.getMonth()));
      }

      Balance statementStartBalance = statement.getStartBalance();
      if (getStartBalance() == null) {
        setStartBalance(statementStartBalance);
      } else {
        if (statementStartBalance != null) {
          setStartBalance(Balance.add(getStartBalance(), statementStartBalance));
        }
      }
      Balance statementEndBalance = statement.getEndBalance();
      if (getEndBalance() == null) {
        setEndBalance(statementEndBalance);
      } else {
        if (statementEndBalance != null) {
          setEndBalance(Balance.add(getEndBalance(), statementEndBalance));
        }
      }
      addInFlow(statement.getInFlows());
      addOutFlow(statement.getOutFlows());
      addTransfer(statement.getTotalTransfers());
      addPayment(statement.getTotalPayments());
      addIncome(statement.getIncome());
      isClosed = isClosed && statement.isClosed();
      statements.add(statement);
      return this;
    }

    public SummaryStatement build() {
      Collections.sort(statements, new Comparator<Statement>() {
        @Override
        public int compare(Statement o1, Statement o2) {
          return o1.getName().compareTo(o2.getName());
        }
      });
      return new SummaryStatement(this);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }

  private SummaryStatement(Builder builder) {
    super(builder);
    statements = ImmutableList.copyOf(builder.statements);
    isClosed = builder.isClosed;
  }

  public ImmutableList<Statement> getStatements() {
    return statements;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }
}
