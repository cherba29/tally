package com.cherba29.tally.core

import kotlinx.datetime.LocalDate

/**
 * Represents confirmed or projected balance at a specific date.
 */
data class Balance(
  val amount: Long,
  val date: LocalDate,
  val type: Type,
  val description: String = "",
) : Comparable<Balance> {
  enum class Type(val id: String) {
    UNKNOWN("UNKNOWN"),
    CONFIRMED("CONFIRMED"),
    PROJECTED("PROJECTED"),
    AUTO_PROJECTED("AUTO_PROJECTED");

    companion object {
      fun combineTypes(t1: Type, t2: Type): Type = if (t1.ordinal < t2.ordinal) t2 else t1
    }
  }

  private fun max(date1: LocalDate, date2: LocalDate) = if (date1 < date2) date2 else date1
  operator fun plus(other: Balance) = Balance(
    amount + other.amount,
    max(date,other.date),
    Type.combineTypes(type, other.type)
  )

  operator fun minus(other: Balance) = Balance(
    amount - other.amount,
    max(date,other.date),
    Type.combineTypes(type, other.type)
  )

  operator fun unaryMinus() = Balance(-amount, date, type)

  override fun compareTo(other: Balance): Int {
    val dateDiff = date.compareTo(other.date)
    if (dateDiff != 0) {
      return dateDiff
    }
    val amountDiff = amount.compareTo(other.amount)
    if (amountDiff != 0) {
      return amountDiff
    }
    return type.compareTo(other.type)
  }

  override fun toString(): String {
    return "Balance { amount: ${String.format("%.2f", amount / 100.0)}, date: $date, type: ${type.id} }"
  }

  companion object {
    // Helper constructor.
    fun confirmed(amount: Long, date: String): Balance {
      return Balance(amount, LocalDate.parse(date), Type.CONFIRMED)
    }

    fun projected(amount: Long, date: String): Balance {
      return Balance(amount, LocalDate.parse(date), Type.PROJECTED)
    }

    fun pickMinDate(first: Balance?, second: Balance?): Balance? {
      if (first == null) return second
      if (second == null) return first
      return if (first.date > second.date) second else first
    }

    fun pickMaxDate(first: Balance?, second: Balance?): Balance? {
      if (first == null) return second
      if (second == null) return first
      return if (first.date > second.date) first else second
    }
  }
}

operator fun Balance?.plus(other: Balance?): Balance? {
  if (this == null) return other
  if (other == null) return this
  return this + other
}

operator fun Balance?.minus(other: Balance?): Balance? {
  if (this == null) return if (other == null) null else -other
  if (other == null) return this
  return this - other
}
