package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class UtilsTest : DescribeSpec({
  describe("Map3") {
    it("get and set") {
      val map = Map3<Int>()
      map.set("key1", "key2", "key3", 1)
      map["key1", "key2", "key3"] shouldBe 1
      map.size shouldBe 1
    }
    it("get default") {
      val map = Map3<Int>()
      map.set("key1", "key2", "key3", 1)
      map.getDefault("key1", "key2", "key3") { 3 } shouldBe 1
      map.size shouldBe 1
    }

    it("clear") {
      val map = Map3<Int>()
      map.set("key1", "key2", "key3", 1)
      map.clear()
      map.size shouldBe 0
    }

    it("isEmpty") {
      val map = Map3<Int>()
      map.isEmpty shouldBe true
      map.set("key1", "key2", "key3", 1)
      map.isEmpty shouldBe false
    }

    it("get2") {
      val map = Map3<Int>()
      map.set("key1", "key2", "key3", 1)
      val map2 = map.get2("key1", "key2")
      map2?.size shouldBe 1
      map.get2("key1", "key2")?.get("key3") shouldBe 1
    }

    it("iterator") {
      val map = Map3<Int>()
      map.set("key1", "key2", "key3", 1)
      map.set("key4", "key5", "key6", 2)
      val result = map.toList()
      result shouldBe listOf(
        Entry3("key1", "key2", "key3", 1),
        Entry3("key4", "key5", "key6", 2),
      )
    }

    it("merge") {
      val map1 = Map3<Int>()
      map1.set("key1", "key2", "key3", 1)
      val map2 = Map3<Int>()
      map2.set("key4", "key5", "key6", 2)
      map1.merge(map2)
      map1.size shouldBe 2
      map2.size shouldBe 1
      val result = map1.toList()
      result shouldBe listOf(
        Entry3("key1", "key2", "key3", 1),
        Entry3("key4", "key5", "key6", 2),
      )
    }
  }
})