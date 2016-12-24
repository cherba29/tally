package tally.load.yaml;

import java.text.ParseException;
import java.util.Map;

import tally.core.Balance;
import tally.core.Month;
import tally.core.Transfer;
import tally.load.LoadException;

public class TransferLoader {
    static Transfer.Builder loadFromRecord(
            String accountName, String accountToName, Map<String, Object> transferToRecord)
            throws LoadException {
        String groupName = (String) transferToRecord.get("grp");
        if (groupName == null) {
            throw new LoadException("Could not find 'grp' element");
        }
        Month fromMonth = Month.valueOf(groupName);
        String toMonthStr = (String) transferToRecord.get("dst");
        Month toMonth;
        if (toMonthStr == null) {
            toMonth = fromMonth;
        } else {
            toMonth = Month.valueOf(toMonthStr);
//      throw new LoadException("Transfer from " + accountName + " to " + accountToName + " for "
//          + fromMonth + " has deprecated 'dst' parameter.");
        }
        if (Math.abs(fromMonth.distance(toMonth)) > 1) {
            throw new LoadException("To and from months are too far apart");
        }
        Balance balance;
        try {
            balance = BalanceLoader.getFromRecord(transferToRecord);
        } catch (ParseException e) {
            throw new LoadException(
                    "Failed to parse transfer for account " + accountName
                            + " " + toMonthStr + ": " + e.getMessage());
        }
        Month datedMonth = Month.valueOf(balance.getDate());
        if (Math.abs(datedMonth.distance(toMonth)) > 2
                || Math.abs(datedMonth.distance(fromMonth)) > 2) {
            throw new LoadException(String.format(
                    "For %s account (transfer to %s) %s date and to month %s "
                            + "or from month %s are too far apart.",
                    accountName, accountToName, balance, toMonth, fromMonth));
        }

        Transfer.Builder transferBuilder = Transfer.newBuilder()
                .setFromMonth(fromMonth)
                .setToMonth(toMonth)
                .setAccountFromName(accountName)
                .setAccountToName(accountToName)
                .setBalance(balance);
        String description = (String) transferToRecord.get("desc");
        if (description == null) {
            description = "";
        }
        transferBuilder.setDescription(description);
        return transferBuilder;
    }
}
