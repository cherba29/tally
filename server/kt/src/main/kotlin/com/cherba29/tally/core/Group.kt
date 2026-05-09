package com.cherba29.tally.core

interface GroupInterface<T> {
  val name: String
  val parent: T?
  val children: List<T>

  /**
   * Returns a child by name.
   */
  operator fun get(id: String): GroupInterface<T>?

  operator fun get(path: List<String>): GroupInterface<T>?

  val top: GroupInterface<T>

  val path: List<String>

  /**
   * Nodes are divided into external and non-external (internal).
   * That is any node named "external" and all of its descendants are considered to be external.
   **/
  val isExternal: Boolean
}

sealed class Group(): GroupInterface<Group> {
  class Root(
    override val name: String = "",
    override val isExternal: Boolean = false,
    createChildren: ParentList.() -> Unit
  ) : Group() {
    override val parent: Group? = null
    override val children: List<Group> = ParentList(this).apply(createChildren)
    override fun get(id: String): Group? = children.firstOrNull { it.name == id }

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
    override val parent: Group,
    override val isExternal: Boolean = parent.isExternal
  ) : Group() {
    override val children: List<Group> = ParentList(this).apply(createChildren)
    override fun get(id: String): Group? = children.firstOrNull { it.name == id }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Group) return false // Type check
      return name == other.name && children == other.children
    }

    override fun hashCode(): Int = name.hashCode() * 31 + children.hashCode()

    override fun toString(): String {
      return "$name { ${children.joinToString { it.toString() }} }"
    }
  }

  data class Leaf(
    override val name: String,
    override val parent: Group,
    override val isExternal: Boolean = parent.isExternal
  ) : Group() {
    override val children: List<Group> = listOf()
    override fun get(id: String): Group? = null
    override fun toString() = name

    override fun equals(other: Any?): Boolean {
      if (this === other) return true // Referential check
      if (other !is Leaf) return false // Type check
      return name == other.name
    }
    override fun hashCode(): Int = name.hashCode()
  }

  abstract override fun get(id: String): Group?
  override operator fun get(path: List<String>): Group? =
    if (path.isEmpty()) this else get(path.first())?.get(path.subList(1, path.size))
  override val top: Group get () = if (parent?.parent == null) this else parent!!.top

  override val path: List<String> get() = if (parent == null || name.isEmpty()) listOf() else parent!!.path + name

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

      /** Builds a Group tree from provide paths. */
      fun build(): Group {
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
  val parent: Group,
  private val children: MutableList<Group> = mutableListOf()
) : List<Group> by children {

  fun branch(name: String, createChildren: ParentList.() -> Unit) {
    children += Group.Branch(name, createChildren, parent, name == EXTERNAL_NAME || parent.isExternal)
  }

  fun leaf(name: String) {
    children += Group.Leaf(name, parent, name == EXTERNAL_NAME || parent.isExternal)
  }

  // Since this class is member of Group, which has equals override it here as well.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true // Referential check
    if (other !is List<*>) return false // Type check
    return children == other
  }
  override fun hashCode(): Int = children.hashCode()
}

fun root(createChildren: ParentList.() -> Unit) = Group.Root(createChildren = createChildren)
