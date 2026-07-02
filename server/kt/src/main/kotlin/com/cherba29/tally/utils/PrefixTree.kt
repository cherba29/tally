package com.cherba29.tally.utils

import kotlin.math.min

/**
 * Mutable prefix tree build out of paths.
 * Node rank is minimum over all its children ranks.
 */
class PrefixTree(var rank: Int = Int.MAX_VALUE) {
  private val children = mutableMapOf<String, PrefixTree>()

  private fun insert(name: String, rank: Int = Int.MAX_VALUE) = children.computeIfAbsent(name) { PrefixTree(rank) }

  fun insert(path: List<String>, rank: Int = Int.MAX_VALUE): PrefixTree {
    var node = this
    for (part in path.subList(0, path.size-1)) {
      node.rank = min(node.rank, rank)
      node = node.insert(part)
    }
    node.rank = min(node.rank, rank)
    return node.insert(path.last(), rank)
  }

  fun isEmpty() = children.isEmpty()

  operator fun get(path: List<String>): PrefixTree? {
    var node: PrefixTree = this
    for (part in path) {
      node = children[part] ?: return null
    }
    return node
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PrefixTree

    if (rank != other.rank) return false
    if (children != other.children) return false

    return true
  }

  override fun hashCode(): Int {
    var result = rank
    result = 31 * result + children.hashCode()
    return result
  }

  /**
   * Returns list of child nodes sorted by rank and then by name.
   */
  val sortedEntries get() = children.entries.map {
    Pair(it.key, it.value)
  }.sortedWith(
    compareBy<Pair<String, PrefixTree>> { it.second.rank }.thenBy { it.first }
  )
}
