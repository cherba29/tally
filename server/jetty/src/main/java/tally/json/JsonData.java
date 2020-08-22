package tally.json;

import java.util.List;
import java.util.Map;

import tally.core.Account;
import tally.core.Month;
import tally.statement.Statement;
import tally.statement.SummaryStatement;

public class JsonData {
  public final List<Month> months;
  public final Map<String, Account> accountNameToAccount;
  public final Map<String, Map<Month, Statement>> statements;
  public final Map<String, Map<Month, SummaryStatement>> summaries;

  public JsonData(
      List<Month> months,
      Map<String, Account> accountNameToAccount,
      Map<String, Map<Month, Statement>> statements,
      Map<String, Map<Month, SummaryStatement>> summaries) {
    this.accountNameToAccount = accountNameToAccount;
    this.months = months;
    this.statements = statements;
    this.summaries = summaries;
  }
}