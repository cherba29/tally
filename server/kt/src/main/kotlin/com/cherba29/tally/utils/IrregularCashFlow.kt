package com.cherba29.tally.utils

import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sign

class IrregularCashFlow {
  private val contributions = mutableListOf<Long>()
  private val gains = mutableListOf<Long>()
  private val percentChangeOnContributions = mutableListOf<Double>()
  private val percentChangeOnGains = mutableListOf<Double>()
  private val percentChange = mutableListOf<Double>()
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
    percentChange.add(1.0 + if (total == 0L) 0.0 else (gain + contribution).toDouble() / total)
    total += gain + contribution
  }

  fun effectiveRateOfReturnOnContributions() = effectiveRateOfReturn(percentChangeOnContributions)
  fun effectiveRateOfReturnOnGains() = effectiveRateOfReturn(percentChangeOnGains)
  fun effectiveRateOfReturn() = effectiveRateOfReturn(percentChange)

  /**
   * Average age of the amount = Sum age_i * amt_i / tot.
   */
  fun weightedAverageAmountAge(): Double {
    if (total == 0L) return 0.0
    var sum = 0.0
    var runningAmount = 0L
    for ((i, change) in contributions.zip(gains).reversed().withIndex()) {
      runningAmount += change.first + change.second
      if (runningAmount > 0) {
        sum += (i + 1) * runningAmount
        runningAmount = 0L
      }
    }
    return sum / total
  }

  fun weightedContributionsAge(): Double {
    if (totalContributions <= 0L) return 0.0
    var sum = 0.0
    var runningAmount = 0L
    for ((i, change) in contributions.reversed().withIndex()) {
      runningAmount += change
      if (runningAmount > 0) {
        sum += (i + 1) * runningAmount
        runningAmount = 0L
      }
    }
    return sum / totalContributions
  }

  fun weightedGainsAge(): Double {
    if (totalGains <= 0L) return 0.0
    var sum = 0.0
    var runningAmount = 0L
    for ((i, change) in gains.reversed().withIndex()) {
      runningAmount += change
      if (runningAmount > 0) {
        sum += (i + 1) * runningAmount
        runningAmount = 0L
      }
    }
    return sum / totalGains
  }

  companion object {
    fun effectiveRateOfReturn(rates: List<Double>): Double
      = if (rates.size < 2) 0.0
        else {
          val product = rates.reduce(Double::times)
          product.sign * (product.absoluteValue.pow(12.0 / (rates.size - 1)) - 1)
        }
  }
}
