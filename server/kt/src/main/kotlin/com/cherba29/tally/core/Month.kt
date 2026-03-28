package com.cherba29.tally.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.MonthNames.Companion.ENGLISH_ABBREVIATED
import kotlinx.datetime.number

enum class MonthName {
  JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC;
  operator fun div(year: Int) = Month(year, this.ordinal)
}

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

  operator fun plus(value: Int): Month = next(value)
  operator fun minus(value: Int): Month = next(-value)

  fun previous(amount: Int = 1): Month = next(-amount)

  override fun compareTo(other: Month): Int {
    val yearDiff = year.compareTo(other.year)
    if (yearDiff == 0) {
      return month.compareTo(other.month)
    }
    return yearDiff
  }

  operator fun rangeTo(that: Month): MonthRange = MonthRange(this, that)

  infix fun downTo(to: Month) = MonthProgression.fromClosedRange(this, to, -1)

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
  }
}

abstract class MonthIterator : Iterator<Month> {
  final override fun next(): Month = nextMonth()

  /**
   * Returns the next element in the iteration without boxing conversion.
   * @throws NoSuchElementException if the iteration has no next element.
   */
  public abstract fun nextMonth(): Month
}

/**
 * A progression of values of type `Mont`.
 */
open class MonthProgression
internal constructor
  (
  start: Month,
  endInclusive: Month,
  /**
   * The step of the progression.
   */
  val step: Int
) : Iterable<Month> {
  init {
    if (step == 0) throw kotlin.IllegalArgumentException("Step must be non-zero.")
    if (step == Int.MIN_VALUE) throw kotlin.IllegalArgumentException("Step must be greater than Int.MIN_VALUE to avoid overflow on negation.")
  }

  /**
   * The first element in the progression.
   */
  val first: Month = start

  /**
   * The last element in the progression.
   */
  val last: Month = getProgressionLastElement(start, endInclusive, step)

  override fun iterator(): MonthIterator = MonthProgressionIterator(first, last, step)

  infix fun step(step: Int): MonthProgression {
    checkStepIsPositive(step > 0, step)
    return fromClosedRange(first, last, if (this.step > 0) step else -step)
  }

  /**
   * Checks if the progression is empty.
   *
   * Progression with a positive step is empty if its first element is greater than the last element.
   * Progression with a negative step is empty if its first element is less than the last element.
   */
  open fun isEmpty(): Boolean = if (step > 0) first > last else first < last

  override fun equals(other: Any?): Boolean =
    other is MonthProgression && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last && step == other.step)

  override fun hashCode(): Int =
    if (isEmpty()) -1 else (31 * (31 * first.hashCode() + last.hashCode()) + step)

  override fun toString(): String = if (step > 0) "$first..$last step $step" else "$first downTo $last step ${-step}"

  companion object {
    /**
     * Creates IntProgression within the specified bounds of a closed range.
     *
     * The progression starts with the [rangeStart] value and goes toward the [rangeEnd] value not excluding it, with the specified [step].
     * In order to go backwards the [step] must be negative.
     *
     * [step] must be greater than `Int.MIN_VALUE` and not equal to zero.
     */
    fun fromClosedRange(rangeStart: Month, rangeEnd: Month, step: Int): MonthProgression = MonthProgression(rangeStart, rangeEnd, step)

    private fun getProgressionLastElement(start: Month, end: Month, step: Int): Month = when {
      step > 0 -> if (start >= end) end else end - differenceModulo(end, start, step)
      step < 0 -> if (start <= end) end else end + differenceModulo(start, end, -step)
      else -> throw IllegalArgumentException("Step is zero.")
    }
    private fun differenceModulo(a: Month, b: Month, c: Int): Int {
      return (a - b) % c
    }
    private fun checkStepIsPositive(isPositive: Boolean, step: Number) {
      if (!isPositive) throw IllegalArgumentException("Step must be positive, was: $step.")
    }
  }
}

class MonthProgressionIterator(first: Month, last: Month, val step: Int) : MonthIterator() {
  private val finalElement: Month = last
  private var hasNext: Boolean = if (step > 0) first <= last else first >= last
  private var next: Month = if (hasNext) first else finalElement

  override fun hasNext(): Boolean = hasNext

  override fun nextMonth(): Month {
    val value = next
    if (value == finalElement) {
      if (!hasNext) throw kotlin.NoSuchElementException()
      hasNext = false
    }
    else {
      next += step
    }
    return value
  }
}

/**
 * A range of values of type `Int`.
 */
class MonthRange(start: Month, endInclusive: Month) : MonthProgression(start, endInclusive, 1), ClosedRange<Month>, OpenEndRange<Month> {
  override val start: Month get() = first
  override val endInclusive: Month get() = last

  override val endExclusive: Month get() {
    return last.next()
  }

  override fun contains(value: Month): Boolean = first <= value && value <= last

  /**
   * Checks whether the range is empty.
   *
   * The range is empty if its start value is greater than the end value.
   */
  override fun isEmpty(): Boolean = first > last

  override fun equals(other: Any?): Boolean =
    other is MonthRange && (isEmpty() && other.isEmpty() ||
        first == other.first && last == other.last)

  override fun hashCode(): Int =
    if (isEmpty()) -1 else 31 * start.hashCode() + endInclusive.hashCode()

  override fun toString(): String = "$first..$last"

  companion object {
    /** An empty range of values of type Month. */
    val EMPTY: MonthRange = MonthRange(Month(0, 1), Month(0, 0))
  }
}
