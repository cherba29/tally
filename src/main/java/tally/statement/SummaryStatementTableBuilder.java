package tally.statement;

import java.util.HashMap;
import java.util.Map;

import tally.core.Account;
import tally.core.BudgetException;
import tally.core.Month;

public class SummaryStatementTableBuilder {
  /**
   * Constructs a table indexed by owner name + ' ' account type and month of statement summaries.
   *
   * @param accountNameToAccount map of account name to {@link Account}
   * @param statementTable table indexed by account name, month of statements
   * @return table of statements summaries
   * @throws BudgetException
   */
  public static Map<String, Map<Month, SummaryStatement>> build(
      Map<String, Account> accountNameToAccount,
      Map<String, Map<Month, Statement>> statementTable) throws BudgetException {
    Map<String, Map<Month, SummaryStatement.Builder>> summaryBuilderTable = new HashMap<>();

    // For each account construct history.
    for (Map.Entry<String, Map<Month, Statement>> accountStatements : statementTable.entrySet()) {
      Account account = accountNameToAccount.get(accountStatements.getKey());
      if (account == null) {
        throw new BudgetException("Account and statements are out of sync,"
            + " could not locate account " + accountStatements.getKey());
      }
      for (String owner : account.getOwners()) {
        // Summary per owner and account type
        for (String summaryName : new String[] {
            owner + " " + account.getType(), owner + " SUMMARY" }) {
          if (account.isExternal() && summaryName.indexOf("SUMMARY") > 0) {
            // Don't include in the summary.
            continue;
          }
          Map<Month, SummaryStatement.Builder> summaryStatements =
              summaryBuilderTable.get(summaryName);
          if (summaryStatements == null) {
            summaryStatements = new HashMap<>();
            summaryBuilderTable.put(summaryName, summaryStatements);
          }
          for (Map.Entry<Month, Statement> statements : accountStatements.getValue().entrySet()) {
            Month month = statements.getKey();
            SummaryStatement.Builder summaryBuilder = summaryStatements.get(month);
            if (summaryBuilder == null) {
              summaryBuilder = SummaryStatement.newBuilder();
              summaryBuilder.setName(summaryName);
              summaryStatements.put(month, summaryBuilder);
            }
            summaryBuilder.addStatement(statements.getValue());
          }
        }  // for each month
      }  // for each owner
    }  // for each account

    Map<String, Map<Month, SummaryStatement>> summaryTable = new HashMap<>();
    for (Map.Entry<String, Map<Month, SummaryStatement.Builder>> entry
        : summaryBuilderTable.entrySet()) {
      Map<Month, SummaryStatement> summaryStatements = new HashMap<>();
      for (Map.Entry<Month, SummaryStatement.Builder> stmtEntry : entry.getValue().entrySet()) {
        summaryStatements.put(stmtEntry.getKey(), stmtEntry.getValue().build());
      }
      summaryTable.put(entry.getKey(), summaryStatements);
    }
    return summaryTable;
  }

}
