package tally.load.yaml;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import tally.core.Balance;
import tally.load.LoadException;

public class BalanceLoader {
    private static final int TIMEZONE_OFFSET = Calendar.getInstance().getTimeZone().getRawOffset();


    static Balance getFromRecord(Map<String, Object> record) throws ParseException, LoadException {
        Balance.Type balanceType = Balance.Type.PROJECTED;
        Date date = checkNotNull(getDateFromObject(record.get("date")),
                "No date given for balance record:" + record);
        Object value = record.get("pamt");
        long amount;
        if (value != null) {
            amount = getDecimalFromObject(value);
        } else {
            value = record.get("camt");
            if (value != null) {
                balanceType = Balance.Type.CONFIRMED;
                amount = getDecimalFromObject(value);
            } else {
                throw new IllegalArgumentException("No balance amount (camt/pamt) specified for date "
                        + new SimpleDateFormat("yyyy-MM-dd").format(date));
            }
        }
        return new Balance(amount, date, balanceType);
    }

    private static Date getDateFromObject(Object value) throws ParseException {
        if (value instanceof Date) {
            // Dates are parsed from Yaml using UTC timezone, convert back to local.
            Date date = (Date) value;
            return new Date(date.getTime() - TIMEZONE_OFFSET);
        } else if (value instanceof String) {
            return new SimpleDateFormat("yyyy-MM-dd").parse((String) value);
        }
        throw new ParseException("Date value is specified "
                + value + " but it is neither a String or Date", 0);
    }

    private static long getDecimalFromObject(Object value) throws LoadException {
        checkNotNull(value);
        long parsedValue;
        if (value instanceof String) {
            try {
                parsedValue = new BigDecimal((String) value)
                        .multiply(BigDecimal.valueOf(100))
                        .round(new MathContext(0, RoundingMode.UP))
                        .longValue();
            } catch (NumberFormatException e) {
                throw new LoadException("Failed to parse value '" + value + "' as decimal");
            }
        } else if (value instanceof Double) {
            parsedValue = Math.round(((Double) value).doubleValue() * 100);
        } else if (value instanceof Integer) {
            parsedValue = ((Integer) value) * 100;
        } else {
            throw new LoadException("Amount value is specified "
                    + value + " but it is neither a String, Double or Integer");
        }
        return parsedValue;
    }

}
