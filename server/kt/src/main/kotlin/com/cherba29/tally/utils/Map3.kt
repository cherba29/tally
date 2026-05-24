package com.cherba29.tally.utils

data class Entry3<K1, K2, K3, V>(val key1: K1, val key2: K2, val key3: K3, val valueType: V)

class Map3<K1, K2, K3, V> : Iterable<Entry3<K1, K2, K3, V>> {
  private val store: MutableMap<K1, MutableMap<K2, MutableMap<K3, V>>> = mutableMapOf()

  operator fun get(key1: K1, key2: K2, key3: K3): V? = store[key1]?.get(key2)?.get(key3)

  operator fun get(key1: K1, key2: K2): MutableMap<K3, V>? = store[key1]?.get(key2)

  fun getDefault(key1: K1, key2: K2, key3: K3, defaultFactory: () -> V): V {
    var value = get(key1, key2, key3)
    if (value == null) {
      value = defaultFactory()
      set(key1, key2, key3, value)
    }
    return value!!
  }

  fun set(key1: K1, key2: K2, key3: K3, value: V): Map3<K1, K2, K3, V> {
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
  val keys: Map<K1, Set<K2>> get() = store.entries.associate { (a: K1, b: Map<K2, MutableMap<K3, V>>) -> a to b.keys }

  fun clear() = store.clear()

  fun merge(otherMap3: Map3<K1, K2, K3, V>) {
    for ((key1, key2, key3, value) in otherMap3) {
      set(key1, key2, key3, value)
    }
  }

  override fun iterator(): Iterator<Entry3<K1, K2, K3, V>> = sequence {
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
