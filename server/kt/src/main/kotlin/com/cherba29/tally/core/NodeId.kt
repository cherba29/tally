package com.cherba29.tally.core

// Captures tree structure of accounts into summaries and summaries of summaries.
data class NodeId(
  val name: String,
  val isSummary: Boolean,
  // List of node owner ids.
  val owners: Set<String> = setOf(),
  // Path of this node in the tree.
  val path: List<String> = listOf(),
) {
  // TODO: this is too specific. Need better notion of external.
  val isExternal: Boolean = "external" in path || name == "external"
  val parentPath: List<String> get() = if (path.size < 2) listOf("") else path.slice(0..path.size - 2)


  fun hasCommonOwner(other: NodeId): Boolean = owners.intersect(other.owners).isNotEmpty()

  fun hasSameOwners(other: NodeId): Boolean = owners == other.owners

  override fun toString(): String = if (path.isNotEmpty()) "/${path.joinToString("/")}/$name" else "/$name"

  init {
    require(path.isEmpty() || isExternal || path.firstOrNull() == "internal" || path.size == 1 && path.first() == "") {
      "Node should have empty, external or internal path but was $path"
    }
  }
}
