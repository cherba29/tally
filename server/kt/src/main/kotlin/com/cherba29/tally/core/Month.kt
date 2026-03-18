package com.cherba29.tally.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames.Companion.ENGLISH_ABBREVIATED
import kotlinx.datetime.number

data class Month(val year: Int, val month: Int) : Comparable<Month> {
  init {
    require(month in 0..11) { "Invalid value for month $month" }
  }

  override fun toString(): String = "${ENGLISH_ABBREVIATED.names[month]}$year"

  fun toDate(): LocalDate = LocalDate(year, month + 1, 1)

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

  // TODO: perhaps merge Range and ReverseRange into one.
  class Range(
    override val start: Month,
    override val endInclusive: Month,
    val stepMonths: Int = 1,
  ) : ClosedRange<Month>, Iterable<Month> {
    override fun iterator(): Iterator<Month> = object : Iterator<Month> {
      private var current = start
      override fun hasNext(): Boolean = current <= endInclusive.previous(stepMonths - 1)
      override fun next(): Month {
        val next = current
        current = current.next(stepMonths)
        return next
      }
    }
    infix fun step(months: Int): Range {
      require(months > 0) { "Month step should be positive, but got '$months'"}
      return Range(start, endInclusive, months)
    }
  }

  class ReverseRange(
    override val start: Month,
    override val endInclusive: Month,
    val stepMonths: Int = 1,
  ) : ClosedRange<Month>, Iterable<Month> {
    override fun iterator(): Iterator<Month> = object : Iterator<Month> {
      private var current = start
      override fun hasNext(): Boolean = current >= endInclusive.next(stepMonths - 1)
      override fun next(): Month {
        val next = current
        current = current.previous(stepMonths)
        return next
      }
    }
    infix fun step(months: Int): ReverseRange {
      require(months > 0) { "Month step should be positive, but got '$months'"}
      return ReverseRange(start, endInclusive, months)
    }
  }

  operator fun rangeTo(end: Month) = Range(this, end)

  infix fun downTo(end: Month) = ReverseRange(this, end)

  /**
   * Number of months between two month dates. Negative if provided date is larger.
   */
  operator fun minus(other: Month): Int = (year - other.year) * 12 + month - other.month

  companion object {
    /** Convert string representation to internal representation. */
    fun fromString(name: String): Month {
      require(name.length > 3) { "Cant get month from small string '$name'" }
      val monthName = name.take(3)
      val month = ENGLISH_ABBREVIATED.names.indexOf(monthName)
      require(month >= 0) {
        "Bad month name '$monthName' for '$name', valid names [${ENGLISH_ABBREVIATED.names.joinToString()}]"
      }
      val year = name.substring(3).toIntOrNull()
        ?: throw IllegalArgumentException("Cant get year from '$name'")
      return Month(year, month)
    }

    fun fromDate(date: LocalDate): Month = Month(date.year, date.month.number - 1)
    fun min(vararg args: Month): Month = args.min()
    fun max(vararg args: Month): Month = args.max()
    const val JAN = 0
    const val FEB = 1
    const val MAR = 2
    const val APR = 3
    const val MAY = 4
    const val JUN = 5
    const val JUL = 6
    const val AUG = 7
    const val SEP = 8
    const val OCT = 9
    const val NOV = 10
    const val DEC = 11
  }
}
