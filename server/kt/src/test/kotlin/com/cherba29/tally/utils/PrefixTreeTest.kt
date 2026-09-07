package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PrefixTreeTest : DescribeSpec({
  describe("basic") {
    it("empty") {
      val prefixTree = PrefixTree<String>()
      prefixTree.isEmpty() shouldBe true
      prefixTree.sortedEntries shouldBe listOf()
    }

    it("single entry") {
      val prefixTree = PrefixTree<String>()
      val path = listOf("test")
      prefixTree.insert(path, "test", 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf("test" to prefixTree[path])
    }

    it("multiple entries") {
      val prefixTree = PrefixTree<String>()
      val path1 = listOf("test1")
      val path2 = listOf("test2")
      prefixTree.insert(path1, "test1", 1)
      prefixTree.insert(path2, "test2", 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test1" to prefixTree[path1],
        "test2" to prefixTree[path2]
      )
    }
  }

  describe("sets data") {
    it("root") {
      val prefixTree = PrefixTree<String>()
      val path = listOf<String>()
      prefixTree.insert(path, "test", 1)
      prefixTree.data shouldBe "test"
    }
    it("child") {
      val prefixTree = PrefixTree<String>()
      val path = listOf("test")
      prefixTree.insert(path, "test", 1)
      prefixTree[path]?.data shouldBe "test"
    }
    it("nested") {
      val prefixTree = PrefixTree<String>()
      val path = listOf("top", "test")
      prefixTree.insert(path, "test", 1)
      prefixTree.data shouldBe null
      prefixTree[path]?.data shouldBe "test"
    }
    it("deep nested") {
      val prefixTree = PrefixTree<String>()
      val path = listOf("top", "branch", "leaf")
      prefixTree.insert(path, "test", 1)
      prefixTree.data shouldBe null
      prefixTree[listOf("top", "branch")]?.data shouldBe null
      prefixTree[path]?.data shouldBe "test"
    }
  }

  describe("sorts entries") {
    it("by name") {
      val prefixTree = PrefixTree<String>()
      val path2 = listOf("test2")
      prefixTree.insert(path2, "test2", 1)
      val path1 = listOf("test1")
      prefixTree.insert(path1, "test1", 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test1" to prefixTree[path1],
        "test2" to prefixTree[path2]
      )
    }
    it("by rank") {
      val prefixTree = PrefixTree<String>()
      val path1 = listOf("test1")
      prefixTree.insert(path1, "test1", 2)
      val path2 = listOf("test2")
      prefixTree.insert(path2, "test2", 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test2" to prefixTree[path2],
        "test1" to prefixTree[path1]
      )
    }

    it("where rank is derived") {
      val prefixTree = PrefixTree<String>()
      val path1 = listOf("test1", "test11")
      prefixTree.insert(path1, "test1", 2)
      val path2 = listOf("test2", "test21")
      prefixTree.insert(path2, "test2", 1)
      prefixTree.isEmpty() shouldBe false
      prefixTree.sortedEntries shouldBe listOf(
        "test2" to prefixTree[listOf("test2")],
        "test1" to prefixTree[listOf("test1")]
      )
    }
  }

  describe("equal") {
    it("empty") {
      val prefixTree1 = PrefixTree<String>()
      val prefixTree2 = PrefixTree<String>()
      prefixTree1 shouldBe prefixTree2
    }

    it("with same entry") {
      val prefixTree1 = PrefixTree<String>()
      prefixTree1.insert(listOf("test"), "test1")
      val prefixTree2 = PrefixTree<String>()
      prefixTree2.insert(listOf("test"), "test1")
      prefixTree1 shouldBe prefixTree2
    }

    it("with different entry") {
      val prefixTree1 = PrefixTree<String>()
      prefixTree1.insert(listOf("test1"), "test1")
      val prefixTree2 = PrefixTree<String>()
      prefixTree2.insert(listOf("test2"), "test2")
      prefixTree1 shouldNotBe prefixTree2
    }
  }
})
