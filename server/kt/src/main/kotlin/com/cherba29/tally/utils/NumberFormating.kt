package com.cherba29.tally.utils

import kotlin.math.pow
import kotlin.math.round

fun Long.asAmount(): String = "%.2f".format(this / 100.0)

fun Double.asRoundedPercent(decimalPlaces: Int): Float {
  val roundingFactor = 10.0.pow(decimalPlaces)
  return (round(100.0 * this * roundingFactor) / roundingFactor).toFloat()
}

fun Double.asRounded(decimalPlaces: Int): Float {
  val roundingFactor = 10.0.pow(decimalPlaces)
  return (round(this * roundingFactor) / roundingFactor).toFloat()
}
