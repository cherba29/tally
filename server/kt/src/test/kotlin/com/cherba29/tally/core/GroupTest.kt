package com.cherba29.tally.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe

class GroupTest : DescribeSpec({
  describe("Creation") {
    it("single root") {
      val tree = root {  }
      tree.name shouldBe ""
      tree.children.isEmpty() shouldBe true
    }

    it("root with children") {
      val tree = root {
        leaf("child1")
        leaf("child2")
      }
      tree.name shouldBe ""
      tree.children.map { it.name } shouldBe listOf("child1", "child2")
      tree.children[0].parent shouldBe tree
      tree.children[1].parent shouldBe tree
    }
  }

  describe("Builder") {
    it("empty") {
      val builder = Group.Companion.Builder()
      val tree = builder.build()
      tree shouldBe root {}
    }

    it("just leafs") {
      val builder = Group.Companion.Builder()
      builder.addPath(listOf("child1"))
      builder.addPath(listOf("child2"))
      val tree = builder.build()
      tree shouldBe root {
        leaf("child1")
        leaf("child2")
      }
    }

    it("single branch") {
      val builder = Group.Companion.Builder()
      builder.addPath(listOf("branch1", "child1"))
      builder.addPath(listOf("branch1", "child2"))
      val tree = builder.build()

      tree shouldBe root {
        branch("branch1") {
          leaf("child1")
          leaf("child2")
        }
      }
    }

    it("branch as subpath") {
      val builder = Group.Companion.Builder()
      builder.addPath(listOf("branch1", "child1"))
      builder.addPath(listOf("branch1"))
      val tree = builder.build()

      tree shouldBe root {
        branch("branch1") {
          leaf("child1")
        }
      }
    }

    it("branch plus leaf") {
      val builder = Group.Companion.Builder()
      builder.addPath(listOf("branch1", "child1"))
      builder.addPath(listOf("branch1", "child2"))
      builder.addPath(listOf("child3"))
      val tree = builder.build()

      tree shouldBe root {
        branch("branch1") {
          leaf("child1")
          leaf("child2")
        }
        leaf("child3")
      }
    }

    it("multiple branches") {
      val builder = Group.Companion.Builder()
      builder.addPath(listOf("branch1", "child11"))
      builder.addPath(listOf("branch1", "child12"))
      builder.addPath(listOf("branch1", "branch11", "child113"))
      builder.addPath(listOf("branch2", "child23"))
      val tree = builder.build()

      tree shouldBe root {
        branch("branch1") {
          branch("branch11") {
            leaf("child113")
          }
          leaf("child11")
          leaf("child12")
        }
        branch("branch2") {
          leaf("child23")
        }
      }
    }
  }

  describe("get") {
    it("empty") {
      val tree = root {}
      tree["branch"] shouldBe null
    }

    it("just leafs") {
      val tree = root {
        leaf("child1")
        leaf("child2")
      }
      tree["child1"]?.name shouldBe "child1"
      tree[listOf("child1")]?.name shouldBe "child1"
    }

    it("path via branch to get leaf") {
      val tree = root {
        branch("branch1") {
          leaf("child1")
        }
        leaf("child2")
      }
      tree[listOf("branch1", "child1")]?.name shouldBe "child1"
    }

    it("path via branch to get branch") {
      val tree = root {
        branch("branch1") {
          branch("branch11") {
            leaf("child1")
          }
        }
        leaf("child2")
      }
      tree[listOf("branch1", "branch11")]?.name shouldBe "branch11"
    }
  }

  describe("isExternal") {
    it("empty") {
      val tree = root {}
      tree.isExternal shouldBe false
    }

    it("just leafs") {
      val tree = root {
        leaf("external")
        leaf("internal")
      }
      tree.children[0].isExternal shouldBe true
      tree.children[1].isExternal shouldBe false
    }

    it("branched") {
      val tree = root {
        branch("external") {
          leaf("child1")
        }
        branch("internal") {
          leaf("child2")
        }
      }
      tree.children[0].isExternal shouldBe true
      tree.children[0].children[0].name shouldBe "child1"
      tree.children[0].children[0].isExternal shouldBe true
      tree.children[1].isExternal shouldBe false
      tree.children[1].children[0].isExternal shouldBe false
    }

    it("nested") {
      val tree = root {
        branch("branch1") {
          branch("external") {
            leaf("child1")
          }
        }
      }
      tree.children[0].isExternal shouldBe false
      tree.children[0].children[0].isExternal shouldBe true
      tree.children[0].children[0].children[0].isExternal shouldBe true
    }
  }

  describe("top") {
    it("empty") {
      val tree = root {}
      tree.top shouldBe tree
    }

    it("just leafs") {
      val tree = root {
        leaf("external")
        leaf("internal")
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[1].top shouldBe tree.children[1]
    }

    it("branched") {
      val tree = root {
        branch("external") {
          leaf("child1")
        }
        branch("internal") {
          leaf("child2")
        }
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[0].children[0].top shouldBe tree.children[0]
      tree.children[1].top shouldBe tree.children[1]
      tree.children[1].children[0].top shouldBe tree.children[1]
    }

    it("nested") {
      val tree = root {
        branch("branch1") {
          branch("external") {
            leaf("child1")
          }
        }
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[0].children[0].top shouldBe tree.children[0]
      tree.children[0].children[0].children[0].top shouldBe tree.children[0]
    }
  }

  describe("path") {
    it("empty") {
      val tree = root {}
      tree.path shouldBe listOf()
    }

    it("just leafs") {
      val tree = root {
        leaf("external")
        leaf("internal")
      }
      tree.children[0].name shouldBe "external"
      tree.children[0].path shouldBe listOf("external")
      tree.children[1].path shouldBe listOf("internal")
    }

    it("branched") {
      val tree = root {
        branch("external") {
          leaf("child1")
        }
        branch("internal") {
          leaf("child2")
        }
      }
      tree.children[0].path shouldBe listOf("external")
      tree.children[0].children[0].path shouldBe listOf("external", "child1")
      tree.children[1].path shouldBe listOf("internal")
      tree.children[1].children[0].path shouldBe listOf("internal", "child2")
    }

    it("nested") {
      val tree = root {
        branch("branch1") {
          branch("external") {
            leaf("child1")
          }
        }
      }
      tree.children[0].path shouldBe listOf("branch1")
      tree.children[0].children[0].path shouldBe listOf("branch1", "external")
      tree.children[0].children[0].children[0].path shouldBe listOf("branch1", "external", "child1")
    }
  }
})
