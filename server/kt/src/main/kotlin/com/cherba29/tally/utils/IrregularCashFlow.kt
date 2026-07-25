package com.cherba29.tally.utils

import kotlin.math.pow

class IrregularCashFlow {
  private val contributions = mutableListOf<Long>()
  private val gains = mutableListOf<Long>()
  private val percentChangeOnContributions = mutableListOf<Double>()
  private val percentChangeOnGains = mutableListOf<Double>()
  var totalContributions: Long = 0
    private set
  val contributionFraction: Double get() = if (total == 0L) 0.0 else totalContributions.toDouble() / total
  var totalGains: Long = 0
    private set
  val gainsFraction: Double get() = if (total == 0L) 0.0 else totalGains.toDouble() / total
  var total: Long = 0
    private set


  fun add(contribution: Long, gain: Long) {
    contributions.add(contribution)
    gains.add(gain)
    totalContributions += contribution
    totalGains += gain
    val totalWithNewContribution = total + contribution
    percentChangeOnContributions.add(1.0 + if (totalWithNewContribution == 0L) 0.0 else gain.toDouble() / totalWithNewContribution)
    val totalWithNewGain = total + gain
    percentChangeOnGains.add(1.0 + if (totalWithNewGain == 0L) 0.0 else contribution.toDouble() / totalWithNewGain)
    total += gain + contribution
  }

  fun effectiveRateOfReturnOnContributions(): Double {
    if (percentChangeOnContributions.size < 2) return 0.0
    return percentChangeOnContributions
      .reduce(Double::times)
      .pow(12.0 / (percentChangeOnContributions.size - 1)) - 1
  }
  fun effectiveRateOfReturnOnGains(): Double {
    if (percentChangeOnGains.size < 2) return 0.0
    return percentChangeOnGains
      .reduce(Double::times)
      .pow(12.0 / (percentChangeOnGains.size - 1)) - 1
  }
}
