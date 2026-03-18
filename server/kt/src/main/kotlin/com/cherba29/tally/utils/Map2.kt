package com.cherba29.tally.utils

// TODO: use more kotlin idiomatic constructs.

data class Entry2<ValueType>(val key1: String, val key2: String, val value: ValueType)

class Map2<ValueType> : Iterable<Entry2<ValueType>> {
  private val store: MutableMap<String, MutableMap<String, ValueType>> = mutableMapOf()

  operator fun get(key1: String, key2: String): ValueType? = store[key1]?.get(key2)

  fun getDefault(key1: String, key2: String, defaultFactory: () -> ValueType): ValueType {
    var value = get(key1, key2)
    if (value == null) {
      value = defaultFactory()
      set(key1, key2, value)
    }
    return value!!
  }

  fun set(key1: String, key2: String, value: ValueType): Map2<ValueType> {
    var subStore = store[key1]
    if (subStore == null) {
      subStore = mutableMapOf()
      store[key1] = subStore
    }
    subStore[key2] = value
    return this
  }

  fun clear() = store.clear()

  val size: Int get() = store.values.sumOf { it.size }

  override fun iterator(): Iterator<Entry2<ValueType>> = sequence {
    for ((key1, subStore) in store) {
      for ((key2, value) in subStore) {
        yield(Entry2(key1, key2, value))
      }
    }
  }.iterator()
}
