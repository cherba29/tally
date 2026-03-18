package com.cherba29.tally.core

enum class MonthName {
  JAN,
  FEB,
  MAR,
  APR,
  MAY,
  JUN,
  JUL,
  AUG,
  SEP,
  OCT,
  NOV,
  DEC;

  operator fun div(year: Int) = Month(year, this.ordinal)
}