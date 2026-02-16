package com.cherba29.tally.core

// TODO: Use kotlinx.datetime.Month
enum class MonthName(name: String) {
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
    private val monthNameToIndex = entries.associateBy { it.name }
    fun fromName(name: String): MonthName? = monthNameToIndex[name]
  }
}

data class Month(val year: Int, val month: Int) {
  override fun toString(): String ="${MonthName.entries[month].name}$year"

  companion object {
    fun fromString(name: String): Month {
      require(name.length > 3) { "Cant get month from small string '$name'" }
      val month = MonthName.fromName(name.substring(0, 3)) ?: throw IllegalArgumentException("Cant find month for '$name'")
      val year = name.substring(3).toIntOrNull() ?: throw IllegalArgumentException("Cant get year from '$name'")
      return Month(year, month.ordinal)
    }
  }
}
