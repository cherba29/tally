package com.cherba29.tally.core

import kotlinx.datetime.LocalDate

enum class BalanceType(val id: String) {
  UNKNOWN("UNKNOWN"),
  CONFIRMED("CONFIRMED"),
  PROJECTED("PROJECTED"),
  AUTO_PROJECTED("AUTO_PROJECTED");

  companion object {
    fun combineTypes(t1: BalanceType, t2: BalanceType): BalanceType = if (t1.ordinal < t2.ordinal) t2 else t1
  }
}

data class Balance(val amount: Int, val date: LocalDate, val type: BalanceType) : Comparable<Balance> {
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
    // Helper contructor.
    fun confirmed(amount: Int, date: String): Balance {
      return Balance(amount, LocalDate.parse(date), BalanceType.CONFIRMED)
    }

    fun projected(amount: Int, date: String): Balance {
      return Balance(amount, LocalDate.parse(date), BalanceType.PROJECTED)
    }

    fun negated(balance: Balance): Balance {
      return Balance(-balance.amount, balance.date, balance.type)
    }

    fun add(balance1: Balance, balance2: Balance): Balance {
      val maxDate = if (balance1.date < balance2.date) balance2.date else balance1.date
      return Balance(
        balance1.amount + balance2.amount, maxDate, BalanceType.combineTypes(balance1.type, balance2.type)
      )
    }

    fun subtract(balance1: Balance, balance2: Balance): Balance {
      return add(balance1, Balance(-balance2.amount, balance2.date, balance2.type))
    }
  }
}
