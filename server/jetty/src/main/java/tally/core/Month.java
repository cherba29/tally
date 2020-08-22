package tally.core;

import com.google.common.collect.ImmutableList;

import java.lang.Comparable;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a financial month.
 */
public class Month implements Comparable<Month> {
  private static final ImmutableList<String> MONTH_NAMES = ImmutableList.copyOf(
      new java.text.DateFormatSymbols().getShortMonths());

  private final int year;
  private final int month;

  public static Month of(int year, int month) {
    return new Month(year, month);
  }

  public static Month valueOf(String monthStr) {
    int monthIndex = MONTH_NAMES.indexOf(monthStr.substring(0, 3));
    if (monthIndex < 0) {
      throw new IllegalArgumentException("Bad month name " + monthStr);
    }
    Integer year = Integer.valueOf(monthStr.substring(3));
    return new Month(year, monthIndex);
  }

  public static Month valueOf(Date date) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    return new Month(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH));
  }

  private Month(int year, int month) {
    checkArgument(month >= 0 && month < 12, "month must be non-negative integer less than 12");
    this.month = month;
    this.year = year;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public Month next() {
    if (month >= 11) {
      return new Month(year + 1, 0);
    }
    return new Month(year, month + 1);
  }

  public Month previous() {
    if (month == 0) {
      return new Month(year - 1, 11);
    }
    return new Month(year, month - 1);
  }

  @Override
  public int compareTo(Month other) {
    if (year == other.year) {
      return month - other.month;
    }
    return year - other.year;
  }

  @Override
  public int hashCode() {
    return Objects.hash(year, month);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Month)) {
      return false;
    }
    Month other = (Month) obj;
    return month == other.month && year == other.year;
  }

  public int distance(Month other) {
    return (other.year - year) * 12 + other.month - month;
  }

  @Override
  public String toString() {
    return String.format("%s%d", MONTH_NAMES.get(month), year);
  }

}
