package com.cherba29.tally.core

import com.cherba29.tally.utils.PrefixTree
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

sealed class TreeNode: TreeNodeInterface<TreeNode>, Comparable<TreeNode> {
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

  fun traverseDepthDown(): Sequence<TreeNode> = sequence {
    yield(this@TreeNode)
    for (child in children) {
      yieldAll(child.traverseDepthDown())
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

  val indexPath: List<Int> by lazy {
    var currParent = parent
    var childName = name
    val path = mutableListOf<Int>()
    while (currParent != null) {
      path.add(currParent.children.indexOfFirst { it.name == childName })
      childName = currParent.name
      currParent = currParent.parent
    }
    path.reversed()
  }

  override fun compareTo(other: TreeNode): Int = indexPath.lexicographicCompareTo(other.indexPath)

  class Builder {
    private val prefixTree = PrefixTree()

    /** Add path from which tree containing it can be built. */
    fun addPath(path: List<String>, rank: Int? = null) = prefixTree.insert(path, rank ?: Int.MAX_VALUE)

    fun build(): TreeNode = root { addChildren(prefixTree) }

    companion object {
      // Recursively build immutable tree nodes from prefix tree.
      context(parentList: ParentList)
      private fun addChildren(prefixTree: PrefixTree) {
        for ((childName, childTree) in prefixTree.sortedEntries) {
          if (childTree.isEmpty()) {
            parentList.leaf(childName)
          } else {
            parentList.branch(childName) { addChildren(childTree) }
          }
        }
      }
    }
  }
}

private fun <T : Comparable<T>> List<T>.lexicographicCompareTo(other: List<T>): Int {
  val minSize = minOf(this.size, other.size)
  for (i in 0 until minSize) {
    val cmp = this[i].compareTo(other[i])
    if (cmp != 0) return cmp
  }
  return this.size.compareTo(other.size)
}

private const val EXTERNAL_NAME = "external"

/** Context class for tree DSL. */
class ParentList(
  val parent: TreeNode,
  private val children: MutableList<TreeNode> = mutableListOf()
) : List<TreeNode> by children {

  private fun isExternal(name: String) = name == EXTERNAL_NAME || parent.isExternal

  fun branch(name: String, createChildren: ParentList.() -> Unit) {
    children += TreeNode.Branch(name, createChildren, parent, isExternal(name))
  }

  fun leaf(name: String) {
    children += TreeNode.Leaf(name, parent, isExternal(name))
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
