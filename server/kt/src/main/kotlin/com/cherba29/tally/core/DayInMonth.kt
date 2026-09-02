package com.cherba29.tally.core

sealed interface DayInMonth : Comparable<DayInMonth>  {
  enum class Relative {
    FIRST_OF_MONTH,
    LAST_OF_MONTH
  }

  @JvmInline value class AbsoluteDay(val value: Int): DayInMonth
  @JvmInline value class RelativeDay(val value: Relative): DayInMonth
  class Unknown(): DayInMonth {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is Unknown) return false
      return true
    }
    override fun hashCode(): Int = javaClass.hashCode()
  }

  // Define a ranking order for the type itself
  private val typeOrder: Int
    get() = when (this) {
      is AbsoluteDay -> 3
      is RelativeDay -> 2
      is Unknown -> 1
    }

  override fun compareTo(other: DayInMonth): Int {
    val typeCompare = this.typeOrder.compareTo(other.typeOrder)
    if (typeCompare != 0) return typeCompare

    return when (this) {
      is AbsoluteDay if other is AbsoluteDay -> this.value.compareTo(other.value)
      is RelativeDay if other is RelativeDay -> this.value.compareTo(other.value)
      else -> 0
    }
  }

  companion object {
    fun fromString(value: String?): DayInMonth {
      if (value == null) return Unknown()
      val intValue = value.toIntOrNull()
      if (intValue != null) {
        return AbsoluteDay(intValue)
      }
      val lookFor = value.uppercase()
      val entry = Relative.entries.firstOrNull { it.name == lookFor }
        ?: throw IllegalArgumentException("Cant parse '$value' as DayInMonth")
      return RelativeDay(entry)
    }
  }
}
