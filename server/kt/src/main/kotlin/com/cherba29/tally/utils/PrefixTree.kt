package com.cherba29.tally.utils

import kotlin.math.min

/**
 * Mutable prefix tree build out of paths.
 * Node rank is minimum over all its children ranks.
 */
class PrefixTree<P>(
  var data: P? = null,
  var rank: Int = Int.MAX_VALUE
) {
  private val children = mutableMapOf<String, PrefixTree<P>>()

  private fun insert(name: String, data: P?, rank: Int = Int.MAX_VALUE) = children.computeIfAbsent(name) { PrefixTree(data, rank) }

  fun insert(path: List<String>, data: P?, rank: Int = Int.MAX_VALUE): PrefixTree<P> {
    if (path.isNotEmpty()) {
      var node = this
      for (part in path.subList(0, path.size - 1)) {
        node.rank = min(node.rank, rank)
        node = node.insert(part, null)
      }
      node.data = data
      node.rank = min(node.rank, rank)
      return node.insert(path.last(), data, rank)
    }
    this.data = data
    this.rank = rank
    return this
  }

  fun isEmpty() = children.isEmpty()

  operator fun get(path: List<String>): PrefixTree<P>? {
    var node: PrefixTree<P> = this
    for (part in path) {
      node = children[part] ?: return null
    }
    return node
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PrefixTree<P>

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
    compareBy<Pair<String, PrefixTree<P>>> { it.second.rank }.thenBy { it.first }
  )
}
