package com.cherba29.tally.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class NodeIdTest : DescribeSpec({
  describe("create") {
    it("path must be external or internal") {
      val exception = shouldThrow<IllegalArgumentException> { NodeId("test",path = listOf("out")) }
      exception.message shouldBe "Node should have empty, external or internal path but was [out]"
    }

    it("common case") {
      val nodeId = NodeId(
        name = "testAccount",
        path = listOf("external", "tax"),
        owners = listOf("bob", "alice")
      )
      nodeId.name shouldBe "testAccount"
      nodeId.isSummary shouldBe false
      nodeId.isExternal shouldBe true
      nodeId.path shouldBe listOf("external", "tax")
      nodeId.owners shouldBe listOf("bob", "alice")
    }
  }

  describe("type") {
    it("isSummary false if type is not summary") {
      val nodeId = NodeId("testAccount")
      nodeId.isSummary shouldBe false
    }

    it("isSummary when type is summary") {
      val nodeId = NodeId("/test/summary")
      nodeId.isSummary shouldBe true
    }

    it("isExternal false by default") {
      val nodeId = NodeId("testAccount")
      nodeId.isExternal shouldBe false
    }

    it("isExternal true when path starts with external") {
      val nodeId = NodeId(
        name = "testAccount",
        path = listOf("external"),
        owners = listOf("bob")
      )
      nodeId.isExternal shouldBe true
    }
  }

  describe("owner") {
    it("has common owner is false if no common owners") {
      val nodeId1 = NodeId(name = "testAccount", owners = listOf("bob"))
      val nodeId2 = NodeId(name = "testAccount", owners = listOf("john"))

      nodeId1.hasCommonOwner(nodeId2) shouldBe false
    }

    it("has common owner is true if common owners") {
      val nodeId1 = NodeId(name = "testAccount", owners = listOf("bob"))
      val nodeId2 = NodeId(name = "testAccount", owners = listOf("bob"))

      nodeId1.hasCommonOwner(nodeId2) shouldBe true
    }
  }

  describe("conversion") {
    it("toString no path no owners") {
      NodeId("test").toString() shouldBe "/test"
    }
    it("toString with path no owners") {
      NodeId("test", path=listOf("external")).toString() shouldBe "/external/test"
    }
    it("toString with long path no owners") {
      NodeId("test", path=listOf("external", "tax")).toString() shouldBe "/external/tax/test"
    }
    it("toString with path and owners") {
      NodeId("test", path=listOf("external"), owners=listOf("bob")).toString() shouldBe "/external/test"
    }
  }
})