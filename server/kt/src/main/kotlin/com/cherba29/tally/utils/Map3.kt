package com.cherba29.tally.utils

data class Entry3<ValueType>(val key1: String, val key2: String, val key3: String, val valueType: ValueType)

class Map3<ValueType> : Iterable<Entry3<ValueType>> {
  private val store: MutableMap<String, MutableMap<String, MutableMap<String, ValueType>>> = mutableMapOf()

  operator fun get(key1: String, key2: String, key3: String): ValueType? = store[key1]?.get(key2)?.get(key3)

  fun get2(key1: String, key2: String): MutableMap<String, ValueType>? = store[key1]?.get(key2)

  fun getDefault(key1: String, key2: String, key3: String, defaultFactory: () -> ValueType): ValueType {
    var value = get(key1, key2, key3)
    if (value == null) {
      value = defaultFactory()
      set(key1, key2, key3, value)
    }
    return value!!
  }

  fun set(key1: String, key2: String, key3: String, value: ValueType): Map3<ValueType> {
    var subStore1 = store[key1]
    if (subStore1 == null) {
      subStore1 = mutableMapOf()
      store[key1] = subStore1
    }
    var subStore2 = subStore1[key2]
    if (subStore2 == null) {
      subStore2 = mutableMapOf()
      subStore1[key2] = subStore2
    }
    subStore2[key3] = value
    return this
  }

  val size: Int get() = store.values.sumOf { subStore -> subStore.values.sumOf { it.size } }

  val isEmpty: Boolean get() = size == 0
  val keys: Map<String, Set<String>> get() = store.entries.associate { (a: String, b: Map<String, MutableMap<String, ValueType>>) -> a to b.keys }

  fun clear() = store.clear()

  fun merge(otherMap3: Map3<ValueType>) {
    for ((key1, key2, key3, value) in otherMap3) {
      set(key1, key2, key3, value)
    }
  }

  override fun iterator(): Iterator<Entry3<ValueType>> = sequence {
    for ((key1, subStore1) in store) {
      for ((key2, subStore2) in subStore1) {
        for ((key3, value) in subStore2) {
          yield(Entry3(key1, key2, key3, value))
        }
      }
    }
  }.iterator()

  override fun toString(): String = store.toString()
}
