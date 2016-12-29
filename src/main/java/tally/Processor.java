package tally;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

import tally.core.Account;
import tally.core.Budget;
import tally.core.BudgetException;
import tally.core.Month;
import tally.json.JsonData;
import tally.json.JsonResponse;
import tally.load.FileScanner;
import tally.load.LoadException;
import tally.render.ResponseRenderer;
import tally.statement.Statement;
import tally.statement.SummaryStatement;
import tally.statement.SummaryStatementTableBuilder;
import tally.statement.TransactionStatementTableBuilder;

public class Processor {
  private static final Logger logger = LoggerFactory.getLogger(Processor.class);

  private final FileScanner fileScanner;
  private final ResponseRenderer renderer;

  @Inject
  public Processor(FileScanner fileScanner, ResponseRenderer renderer) {
    this.fileScanner = fileScanner;
    this.renderer = renderer;
  }

  /**
   * Writes out json object representing processed budget at given path.
   */
  public void processBudgetAt(String path, OutputStream outputStream) {
    JsonResponse response = buildResponse(path);
    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();
    try {
      renderer.render(response, outputStream);
    } catch (Exception e) {
      response = buildExceptionResponse(e);
      try {
        renderer.render(response, outputStream);
      } catch (Exception e1) {
        logger.error("Failed to render any kind of respose", e1);
      }
    }
    logger.info("Rendered JSON in {} ms", stopwatch.elapsedMillis());
  }

  private JsonResponse buildResponse(String path) {
    Stopwatch stopwatch = new Stopwatch().start();
    Budget budget;
    try {
      Path dir = Paths.get(path);
      if (!Files.exists(dir)) {
        throw new LoadException("Directory " + path + " does not exists, while running in "
            + Paths.get("").toAbsolutePath());
      }
      Budget.Builder builder = new Budget.Builder();
      fileScanner.scan(dir, builder);
      budget = builder.build();
    } catch (LoadException | BudgetException e) {
      return buildExceptionResponse(e);
    }

    Map<String, Account> accountNameToAccount = new HashMap<>();
    for (Account account : budget.getOpenAccounts(budget.getMonths())) {
      if (accountNameToAccount.containsKey(account.getName())) {
        return buildExceptionResponse(
            new BudgetException("Account with duplicate name: " + account.getName()));
      }
      accountNameToAccount.put(account.getName(), account);
    }

    Map<String, Map<Month, Statement>> statementTable;
    try {
      statementTable = TransactionStatementTableBuilder.build(budget);
    } catch (BudgetException e) {
      return buildExceptionResponse(e);
    }

    Map<String, Map<Month, SummaryStatement>> summaryTable;
    try {
      summaryTable = SummaryStatementTableBuilder.build(accountNameToAccount, statementTable);
    } catch (BudgetException e) {
      return buildExceptionResponse(e);
    }

    logger.info("Loaded and constructed in {} ms", stopwatch.elapsedMillis());

    return new JsonResponse(
        true, "",
        new JsonData(budget.getMonths(), accountNameToAccount, statementTable, summaryTable));
  }

  private JsonResponse buildExceptionResponse(Exception exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return new JsonResponse(false, stringWriter.toString(), null);
  }
}
