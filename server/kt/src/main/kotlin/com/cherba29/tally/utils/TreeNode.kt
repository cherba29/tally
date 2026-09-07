package com.cherba29.tally.utils

import kotlin.getValue
import kotlin.sequences.sequence

interface TreeNodeInterface<P, T> {
  val name: String
  val parent: T?
  val children: List<T>
  val data: P
  /**
   * Returns a child by name.
   */
  operator fun get(id: String): TreeNodeInterface<P, T>?

  operator fun get(path: List<String>): TreeNodeInterface<P, T>?

  val top: TreeNodeInterface<P, T>

  val path: List<String>

  val nLeaves: Int
}

sealed class TreeNode<P>: TreeNodeInterface<P, TreeNode<P>>, Comparable<TreeNode<*>> {
  class Root<P>(
    override val name: String = "",
    override val data: P,
    createChildren: ParentList<P>.() -> Unit
  ) : TreeNode<P>() {
    override val parent: TreeNode<P>? = null
    override val children: List<TreeNode<P>> = ParentList(this).apply(createChildren)
    override val nLeaves: Int by lazy { children.sumOf { it.nLeaves } }
    override fun get(id: String): TreeNode<P>? = children.firstOrNull { it.name == id }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Root<P>) return false // Type check
      return children == other.children
    }

    override fun hashCode(): Int = name.hashCode() * 31 + children.hashCode()

    override fun toString(): String {
      return "root { ${children.joinToString { it.toString() }} }"
    }
  }

  class Branch<P>(
    override val name: String,
    createChildren: ParentList<P>.() -> Unit,
    override val parent: TreeNode<P>,
    override val data: P,
  ) : TreeNode<P>() {
    override val children: List<TreeNode<P>> = ParentList(this).apply(createChildren)
    override val nLeaves: Int by lazy { children.sumOf { it.nLeaves } }
    override fun get(id: String): TreeNode<P>? = children.firstOrNull { it.name == id }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Branch<P>) return false // Type check
      return name == other.name && children == other.children
    }

    override fun hashCode(): Int = name.hashCode() * 31 + children.hashCode()

    override fun toString(): String {
      return "$name { ${children.joinToString { it.toString() }} }"
    }
  }

  data class Leaf<P>(
    override val name: String,
    override val parent: TreeNode<P>,
    override val data: P,
  ) : TreeNode<P>() {
    override val children: List<TreeNode<P>> = listOf()
    override val nLeaves = 1
    override fun get(id: String): TreeNode<P>? = null
    override fun toString() = name

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Leaf<P>) return false // Type check
      return name == other.name
    }
    override fun hashCode(): Int = name.hashCode()
  }

  abstract override fun get(id: String): TreeNode<P>?
  override operator fun get(path: List<String>): TreeNode<P>? =
    if (path.isEmpty()) this else get(path.first())?.get(path.subList(1, path.size))
  override val top: TreeNode<P> get () = if (parent?.parent == null) this else parent!!.top

  override val path: List<String> by lazy { if (parent == null || name.isEmpty()) listOf() else parent!!.path + name }
  val pathString: String by lazy { path.joinToString("/") }

  fun traverseBottomUp(): Sequence<TreeNode<P>> = sequence {
    for (child in children) {
      yieldAll(child.traverseBottomUp())
    }
    yield(this@TreeNode)
  }

  fun traverseDepthDown(): Sequence<TreeNode<P>> = sequence {
    yield(this@TreeNode)
    for (child in children) {
      yieldAll(child.traverseDepthDown())
    }
  }

  fun traverseLeaves(): Sequence<Leaf<P>> = sequence {
    if (children.isEmpty()) {
      yield(this@TreeNode as Leaf<P>)
    } else {
      for (child in children) {
        yieldAll(child.traverseLeaves())
      }
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

  override fun compareTo(other: TreeNode<*>): Int = indexPath.lexicographicCompareTo(other.indexPath)

  class Builder<P> {
    private val prefixTree = PrefixTree<P>()

    /** Add path from which tree containing it can be built. */
    fun addPath(path: List<String>, data: P?, rank: Int? = null) = prefixTree.insert(path, data, rank ?: Int.MAX_VALUE)

    fun build(): TreeNode<P> = root(
      prefixTree.data ?: throw IllegalArgumentException("Data is not set for prefixTree root.")
    ) { addChildren(prefixTree) }

    companion object {
      // Recursively build immutable tree nodes from prefix tree.
      context(parentList: ParentList<P>)
      private fun <P> addChildren(prefixTree: PrefixTree<P>) {
        for ((childName, childTree) in prefixTree.sortedEntries) {
          val childData = childTree.data ?: throw IllegalArgumentException("Data is not set for '$childName'")
          if (childTree.isEmpty()) {
            parentList.leaf(childName, childData)
          } else {
            parentList.branch(childName, childData) { addChildren(childTree) }
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

/** Context class for tree DSL. */
class ParentList<P>(
  val parent: TreeNode<P>,
  private val children: MutableList<TreeNode<P>> = mutableListOf()
) : List<TreeNode<P>> by children {

  fun branch(name: String, data: P, createChildren: ParentList<P>.() -> Unit) {
    children += TreeNode.Branch(name, createChildren, parent, data)
  }

  fun leaf(name: String, data: P) {
    children += TreeNode.Leaf(name, parent, data)
  }

  // Since this class is member of TreeNode, which has equals override it here as well.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true // Referential check
    if (other !is List<*>) return false // Type check
    return children == other
  }
  override fun hashCode(): Int = children.hashCode()
}

fun <P> root(data: P, createChildren: ParentList<P>.() -> Unit) = TreeNode.Root(data = data, createChildren = createChildren)
