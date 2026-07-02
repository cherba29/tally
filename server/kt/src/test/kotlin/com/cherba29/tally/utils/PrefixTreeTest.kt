package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PrefixTreeTest : DescribeSpec({
  describe("basic") {
    it("empty") {
      val prefixTree = PrefixTree()
      prefixTree.isEmpty() shouldBe true
      prefixTree.sortedEntries shouldBe listOf()
    }

    it("single entry") {
      val prefixTree = PrefixTree()
      val path = listOf("test")
      prefixTree.insert(path, 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf("test" to prefixTree[path])
    }

    it("multiple entries") {
      val prefixTree = PrefixTree()
      val path1 = listOf("test1")
      val path2 = listOf("test2")
      prefixTree.insert(path1, 1)
      prefixTree.insert(path2, 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test1" to prefixTree[path1],
        "test2" to prefixTree[path2],
      )
    }
  }
  describe("sorts entries") {
    it("by name") {
      val prefixTree = PrefixTree()
      val path2 = listOf("test2")
      prefixTree.insert(path2, 1)
      val path1 = listOf("test1")
      prefixTree.insert(path1, 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test1" to prefixTree[path1],
        "test2" to prefixTree[path2],
      )
    }
    it("by rank") {
      val prefixTree = PrefixTree()
      val path1 = listOf("test1")
      prefixTree.insert(path1, 2)
      val path2 = listOf("test2")
      prefixTree.insert(path2, 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test2" to prefixTree[path2],
        "test1" to prefixTree[path1],
      )
    }

    it("where rank is derived") {
      val prefixTree = PrefixTree()
      val path1 = listOf("test1", "test11")
      prefixTree.insert(path1, 2)
      val path2 = listOf("test2", "test21")
      prefixTree.insert(path2, 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test2" to prefixTree[listOf("test2")],
        "test1" to prefixTree[listOf("test1")],
      )
    }
  }

  describe("equal") {
    it("empty") {
      val prefixTree1 = PrefixTree()
      val prefixTree2 = PrefixTree()
      prefixTree1 shouldBe prefixTree2
    }

    it("with same entry") {
      val prefixTree1 = PrefixTree()
      prefixTree1.insert(listOf("test"))
      val prefixTree2 = PrefixTree()
      prefixTree2.insert(listOf("test"))
      prefixTree1 shouldBe prefixTree2
    }

    it("with different entry") {
      val prefixTree1 = PrefixTree()
      prefixTree1.insert(listOf("test1"))
      val prefixTree2 = PrefixTree()
      prefixTree2.insert(listOf("test2"))
      prefixTree1 shouldNotBe prefixTree2
    }
  }
})