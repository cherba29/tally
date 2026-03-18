package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import kotlin.text.clear
import kotlin.text.get
import kotlin.text.set

class Map2Test : DescribeSpec({
  describe("Map2") {
    it("get and set") {
      val map = Map2<Int>()
      map.set("key1", "value1", 1)
      map.set("key2", "value2", 2)
      map["key1", "value1"] shouldBe 1
      map["key2", "value2"] shouldBe 2
      map.size shouldBe 2
    }

    it("get default") {
      val map = Map2<Int>()
      map.set("key1", "value1", 1)
      map.set("key2", "value2", 2)
      map.getDefault("key1", "value3") { 3 } shouldBe 3
      map.size shouldBe 3
    }

    it("clear") {
      val map = Map2<Int>()
      map.set("key1", "value1", 1)
      map.set("key2", "value2", 2)
      map.clear()
      map.size shouldBe 0
    }

    it("iterator") {
      val map = Map2<Int>()
      map.set("key1", "value1", 1)
      map.set("key2", "value2", 2)
      val result = map.toList()
      result shouldBe listOf(
        Entry2("key1", "value1", 1),
        Entry2("key2", "value2", 2),
      )
    }
  }
})