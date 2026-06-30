package com.cherba29.tally.core

import kotlin.sequences.sequence

interface TreeNodeInterface<T> {
  val name: String
  val parent: T?
  val children: List<T>

  /**
   * Returns a child by name.
   */
  operator fun get(id: String): TreeNodeInterface<T>?

  operator fun get(path: List<String>): TreeNodeInterface<T>?

  val top: TreeNodeInterface<T>

  val path: List<String>

  /**
   * Nodes are divided into external and non-external (internal).
   * That is any node named "external" and all of its descendants are considered to be external.
   **/
  val isExternal: Boolean
}

sealed class TreeNode: TreeNodeInterface<TreeNode> {
  class Root(
    override val name: String = "",
    override val isExternal: Boolean = false,
    createChildren: ParentList.() -> Unit
  ) : TreeNode() {
    override val parent: TreeNode? = null
    override val children: List<TreeNode> = ParentList(this).apply(createChildren)
    override fun get(id: String): TreeNode? = children.firstOrNull { it.name == id }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Root) return false // Type check
      return children == other.children
    }

    override fun hashCode(): Int = name.hashCode() * 31 + children.hashCode()

    override fun toString(): String {
      return "root { ${children.joinToString { it.toString() }} }"
    }
  }

  class Branch(
    override val name: String,
    createChildren: ParentList.() -> Unit,
    override val parent: TreeNode,
    override val isExternal: Boolean = parent.isExternal
  ) : TreeNode() {
    override val children: List<TreeNode> = ParentList(this).apply(createChildren)
    override fun get(id: String): TreeNode? = children.firstOrNull { it.name == id }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is TreeNode) return false // Type check
      return name == other.name && children == other.children
    }

    override fun hashCode(): Int = name.hashCode() * 31 + children.hashCode()

    override fun toString(): String {
      return "$name { ${children.joinToString { it.toString() }} }"
    }
  }

  data class Leaf(
    override val name: String,
    override val parent: TreeNode,
    override val isExternal: Boolean = parent.isExternal
  ) : TreeNode() {
    override val children: List<TreeNode> = listOf()
    override fun get(id: String): TreeNode? = null
    override fun toString() = name

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Leaf) return false // Type check
      return name == other.name
    }
    override fun hashCode(): Int = name.hashCode()
  }

  abstract override fun get(id: String): TreeNode?
  override operator fun get(path: List<String>): TreeNode? =
    if (path.isEmpty()) this else get(path.first())?.get(path.subList(1, path.size))
  override val top: TreeNode get () = if (parent?.parent == null) this else parent!!.top

  override val path: List<String> get() = if (parent == null || name.isEmpty()) listOf() else parent!!.path + name

  fun traverseBottomUp(): Sequence<TreeNode> = sequence {
    for (child in children) {
      yieldAll(child.traverseBottomUp())
    }
    yield(this@TreeNode)
  }

  fun traverseSortedDepthDown(): Sequence<TreeNode> = sequence {
    yield(this@TreeNode)
    for (child in children.sortedBy { it.name }) {
      yieldAll(child.traverseSortedDepthDown())
    }
  }

  fun toPrettyString(prefix: String = "", isLast: Boolean = true): String = buildString {
    // Print current node with its corresponding prefix markers
    append(prefix + (if (isLast) "└── " else "├── ") + name + "\n")

    // Pass the correct structural indentation to children
    val newPrefix = prefix + if (isLast) "    " else "│   "

    for ((i, element) in children.withIndex()) {
      val isLastChild = i == children.size - 1
      append(element.toPrettyString(newPrefix, isLastChild))
    }
  }


  companion object {
    class Builder {
      // TODO: perhaps use more efficient structure so the list does not have to be rescanned resulting in O(n^2).
      private val paths = mutableListOf<List<String>>()

      /** Add path from which tree containing it can be built. */
      fun addPath(path: List<String>) { paths.add(path) }

      /** Checks if given prefix is starting sublist of larger list. */
      private fun <T> isProperPrefix(fullList: List<T>, prefix: List<T>) = prefix.size < fullList.size &&
          prefix.indices.all { i -> prefix[i] == fullList[i] }

      /** Returns names of children and whether they are leaf under given path prefix. */
      private fun getChildrenOf(prefix: List<String>): List<Pair<String, Boolean>> = paths.mapNotNull {
        // Get the names of the child nodes and whether they are a leaf or a branch.
        if (isProperPrefix(it, prefix)) it[prefix.size] to (it.size - 1 == prefix.size) else null
      }.groupBy {
        // We can get duplicate entries and moreover some nodes can be both a leaf node and a branch.
        it.first
      }.map { (name, entries) ->
        // Collapse duplicate entries into one, and if there is at least one non-leaf consider it non-leaf.
        name to entries.all { it.second }
      }.sortedBy {
        // For consistency order elements by name.
        it.first
      }

      /** Builds a TreeNode tree from provide paths. */
      fun build(): TreeNode {
        return root {
          for ((part, isLeaf) in getChildrenOf(listOf())) {
            if (isLeaf) {
              leaf(part)
            } else {
              build(this, listOf(part))
            }
          }
        }
      }

      private fun build(list: ParentList, path: List<String>) {
        list.branch(path.last()) {
          for ((part, isLeaf) in getChildrenOf(path)) {
            if (isLeaf) {
              leaf(part)
            } else {
              build(this, path + part)
            }
          }
        }
      }
    }
  }
}

private const val EXTERNAL_NAME = "external"

/** Context class for tree DSL. */
class ParentList(
  val parent: TreeNode,
  private val children: MutableList<TreeNode> = mutableListOf()
) : List<TreeNode> by children {

  fun branch(name: String, createChildren: ParentList.() -> Unit) {
    children += TreeNode.Branch(name, createChildren, parent, name == EXTERNAL_NAME || parent.isExternal)
  }

  fun leaf(name: String) {
    children += TreeNode.Leaf(name, parent, name == EXTERNAL_NAME || parent.isExternal)
  }

  // Since this class is member of TreeNode, which has equals override it here as well.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true // Referential check
    if (other !is List<*>) return false // Type check
    return children == other
  }
  override fun hashCode(): Int = children.hashCode()
}

fun root(createChildren: ParentList.() -> Unit) = TreeNode.Root(createChildren = createChildren)
