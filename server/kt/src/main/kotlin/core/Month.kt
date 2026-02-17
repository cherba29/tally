package com.cherba29.tally.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

// TODO: Use kotlinx.datetime.Month
enum class MonthName(val shortName: String) {
  JAN("Jan"),
  FEB("Feb"),
  MAR("Mar"),
  APR("Apr"),
  MAY("May"),
  JUN("Jun"),
  JUL("Jul"),
  AUG("Aug"),
  SEP("Sep"),
  OCT("Oct"),
  NOV("Nov"),
  DEC("Dec");

  companion object {
    private val monthNameToIndex = entries.associateBy { it.shortName }
    fun fromName(name: String): MonthName? = monthNameToIndex[name]
  }
}

data class Month(val year: Int, val month: Int) : Comparable<Month> {
  init {
    require(month in 0..11) { "Invalid value for month $month" }
  }

  override fun toString(): String = "${MonthName.entries[month].shortName}$year"

  fun next(amount: Int = 1): Month {
    val months = year * 12 + month + amount
    val nextMonth = months % 12
    return Month((months - nextMonth) / 12, nextMonth)
  }

  operator fun inc(): Month = next(1)

  fun previous(amount: Int = 1): Month = next(-amount)

  override fun compareTo(other: Month): Int {
    val yearDiff = year.compareTo(other.year)
    if (yearDiff == 0) {
      return month.compareTo(other.month)
    }
    return yearDiff
  }

  operator fun rangeTo(end: Month): ClosedRange<Month> {
    return object : ClosedRange<Month> {
      override val start: Month = this@Month
      override val endInclusive: Month = end
    }
  }

  /**
   * Number of months between two month dates.
   * Negative if provided date is larger
   */
  operator fun minus(other: Month): Int = (year - other.year) * 12 + month - other.month

  companion object {
    /** Convert string representation to internal representation. */
    fun fromString(name: String): Month {
      require(name.length > 3) { "Cant get month from small string '$name'" }
      val monthName = name.substring(0, 3)
      val month = MonthName.fromName(monthName)
        ?: throw IllegalArgumentException(
          "Bad month name '$monthName' for '$name', " +
              "valid names [${MonthName.entries.joinToString { it.shortName }}]")
      val year = name.substring(3).toIntOrNull()
        ?: throw IllegalArgumentException("Cant get year from '$name'")
      return Month(year, month.ordinal)
    }

    fun fromDate(date: LocalDate): Month = Month(date.year, date.month.number - 1)
    fun min(vararg args: Month): Month = args.min()
    fun max(vararg args: Month): Month = args.max()

    /** Creates generator spanning start and end (but not including) months. */
    fun generate(start: Month, end: Month): Sequence<Month> = sequence {
      var current = start
      while (current < end) {
        yield(current)
        current++
      }
    }
  }
}
