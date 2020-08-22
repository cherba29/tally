package tally.statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMultiset;

import tally.core.Account;
import tally.core.Balance;
import tally.core.Budget;
import tally.core.BudgetException;
import tally.core.Month;
import tally.core.Transfer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class TransactionStatementTableBuilder {
  private static final Logger logger =
      LoggerFactory.getLogger(TransactionStatementTableBuilder.class);

  private static TransactionStatement.Builder getTransactionStatementBuilder(
      Account account, Month month, Budget budget) throws BudgetException {
    TransactionStatement.Builder statementBuilder = TransactionStatement.newBuilder();
    statementBuilder
        .setAccount(account)
        .setMonth(month);
    ImmutableSortedMultiset<Transfer> transfers = budget.getTransfers(account, month);
    if (transfers != null) {
      statementBuilder.setTransfers(transfers);
    }
    Balance balance = budget.getBalance(account, month);
    if (balance != null) {
      statementBuilder.setStartBalance(balance);
    }
    return statementBuilder;
  }

  /**
   * Builds accountName-month-Statement map.
   * @throws tally.core.MissDatedBalanceException
   */
  public static Map<String, Map<Month, Statement>> build(Budget budget)
      throws BudgetException {
    Map<String, Map<Month, Statement>> statementTable = new HashMap<>();
    List<Month> months = budget.getMonths();
    checkArgument(months.size() > 0, "Budget must have at least one month to build statement for.");

    List<Account> accounts = budget.getOpenAccounts(months);
    logger.info("Number of accounts {}",  accounts.size());

    // For each account construct history.
    for (Account account : accounts) {
      Map<Month, Statement> accountStatements = statementTable.get(account.getName());
      if (accountStatements == null) {
        accountStatements = new HashMap<>();
        statementTable.put(account.getName(), accountStatements);
      }
      TransactionStatement nextMonthStatement = getTransactionStatementBuilder(
          account, months.get(0).next(), budget).build();
      for (Month month : months) {
        TransactionStatement.Builder statementBuilder =
            getTransactionStatementBuilder(account, month, budget);
        Balance nextStartBalance = nextMonthStatement.getStartBalance();
        if (nextStartBalance != null) {
          statementBuilder.setEndBalance(nextStartBalance);
        }
        statementBuilder.setIsCovered(nextMonthStatement.coversPrevious());
        statementBuilder.setIsProjectedCovered(nextMonthStatement.coversProjectedPrevious());

        nextMonthStatement = statementBuilder.build();

        accountStatements.put(month, nextMonthStatement);
      }  // for each month
    }  // for each account

    return statementTable;
  }

}
