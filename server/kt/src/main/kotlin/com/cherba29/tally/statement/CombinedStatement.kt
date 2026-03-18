package com.cherba29.tally.statement

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Month
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

class CombinedStatement(account: Account, month: Month, val startMonth: Month) : Statement(account, month) {
  override val annualizedPercentChange: Double?
    get() {
      val prctChange = percentChange ?: return null
      val numberOfMonths = month - startMonth + 1
      val annualFrequency = 12.0 / numberOfMonths
      val result = (1 + abs(prctChange) / 100).pow(annualFrequency) - 1
      // Annualized percentage change is not that meaningful if large.
      return if (result < 10) 100 * prctChange.sign * result else null
    }

  override val isClosed: Boolean get() = false
}
