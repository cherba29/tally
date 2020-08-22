package tally.load.yaml;

import tally.core.Account;
import tally.core.Balance;
import tally.core.Budget;
import tally.core.Month;
import tally.core.Transfer;
import tally.load.LoadException;
import tally.load.DataLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YamlDataLoader implements DataLoader {
    private static final Logger logger = LoggerFactory.getLogger(YamlDataLoader.class);

    @Override
    public void load(Map<String, Object> data, Budget.Builder budgetBuilder) throws LoadException {
        @SuppressWarnings("unchecked")

        Map<String, String> period = (Map<String, String>) data.get("budget_period");
        if (period != null) {
            String monthStr = period.get("start");
            if (monthStr == null) {
                throw new LoadException("Budget period is specified without start");
            }
            Month start = Month.valueOf(monthStr);
            monthStr = period.get("end");
            if (monthStr == null) {
                throw new LoadException("Budget period is specified without end");
            }
            Month end = Month.valueOf(monthStr);
            if (start.compareTo(end) > 0) {
                throw new LoadException(
                        String.format("Budget period is specified with end %s before start %s", end, start));
            }
            budgetBuilder.setPeriod(start, end);
        }

        if (data.containsKey("name")) {
            String accountName = (String) data.get("name");

            Account.Builder accountBuilder = AccountLoader.loadFromRecord(accountName, data);
            budgetBuilder.addAccountBuilder(accountBuilder);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> balanceRecords = (List<Map<String, Object>>) data.get("balances");

            if (balanceRecords != null) {
                budgetBuilder.addBalances(accountName, loadBalances(accountName, balanceRecords));
            } else {
                logger.warn("Account '{}' has no balances record", accountName);
            }

            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> transferToRecords =
                    (Map<String, List<Map<String, Object>>>) data.get("transfers_to");
            if (transferToRecords != null) {
                budgetBuilder.addAllTransferBuilders(loadTransfers(accountName, transferToRecords));
            }
        }
    }

    private static Map<Month, Balance> loadBalances(
            String accountName, List<Map<String, Object>> balanceRecords) throws LoadException {
        Map<Month, Balance> balances = new HashMap<>();
        for (Map<String, Object> balanceRecord : balanceRecords) {
            String monthStr = (String) balanceRecord.get("grp");
            if (monthStr == null) {
                throw new LoadException(
                        "Expected grp field in balance record for account " + accountName);
            }
            Month month = Month.valueOf(monthStr);
            try {
                Balance balance = BalanceLoader.getFromRecord(balanceRecord);
                int distance = Math.abs(Month.valueOf(balance.getDate()).distance(month));
                if (distance > 2) {
                    throw new LoadException(String.format(
                            "For %s account %s and month %s are %d months apart (2 max).",
                            accountName, balance, month, distance));
                }
                if (balances.containsKey(month)) {
                    throw new LoadException(String.format(
                            "Balance is specified twice for %s account and month %s.", accountName, month));
                }
                balances.put(month, balance);
            } catch (IllegalArgumentException | ParseException e) {
                throw new LoadException(
                        "Failed to add balance for account " + accountName
                                + " " + month + ": " + e.getMessage());
            }
        }
        return balances;
    }

    private static List<Transfer.Builder> loadTransfers(
            String accountName,
            Map<String, List<Map<String, Object>>> transferToRecords) throws LoadException {
        List<Transfer.Builder> builders = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : transferToRecords.entrySet()) {
            String accountToName = entry.getKey();
            List<Map<String, Object>> transferToAccountRecords = entry.getValue();
            if (transferToAccountRecords == null) {
                continue;
            }
            for (Map<String, Object> transferToRecord : transferToAccountRecords) {
                builders.add(TransferLoader.loadFromRecord(accountName, accountToName, transferToRecord));
            }
        }
        return builders;
    }
}
