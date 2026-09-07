package com.cherba29.tally.utils

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TreeNodeTest : DescribeSpec({
  describe("Creation") {
    it("single root") {
      val tree = root("test") { }
      tree.name shouldBe ""
      tree.children.isEmpty() shouldBe true
    }

    it("root with children") {
      val tree = root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
      tree.name shouldBe ""
      tree.children.map { it.name } shouldBe listOf("child1", "child2")
      tree.children[0].parent shouldBe tree
      tree.children[1].parent shouldBe tree
    }
  }

  describe("Builder") {
    it("empty") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf(), "test")
      val tree = builder.build()
      tree shouldBe root("test") {}
    }

    it("just leafs") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("child1"), "test1")
      builder.addPath(listOf("child2"), "test2")
      builder.addPath(listOf(), "test")
      val tree = builder.build()
      tree shouldBe root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
    }

    it("single branch") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("branch1", "child1"), "test11")
      builder.addPath(listOf("branch1", "child2"), "test12")
      builder.addPath(listOf("branch1"), "test1")
      builder.addPath(listOf(), "test")
      val tree = builder.build()

      tree shouldBe root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
          leaf("child2", "test12")
        }
      }
    }

    it("branch as subpath") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("branch1", "child1"), "test11")
      builder.addPath(listOf("branch1"), "test1")
      builder.addPath(listOf(), "test")
      val tree = builder.build()

      tree shouldBe root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
        }
      }
    }

    it("branch plus leaf") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("branch1", "child1"), "test11")
      builder.addPath(listOf("branch1", "child2"), "test12")
      builder.addPath(listOf("branch1"), "test1")
      builder.addPath(listOf("child3"), "test3")
      builder.addPath(listOf(), "test")
      val tree = builder.build()

      tree shouldBe root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
          leaf("child2", "test12")
        }
        leaf("child3", "test3")
      }
    }

    it("multiple branches") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("branch1", "child11"), "test-c11")
      builder.addPath(listOf("branch1", "child12"), "test12")
      builder.addPath(listOf("branch1", "branch11", "child113"), "test113")
      builder.addPath(listOf("branch1", "branch11"), "test-b11")
      builder.addPath(listOf("branch2", "child23"), "test23")
      builder.addPath(listOf("branch1"), "test1")
      builder.addPath(listOf("branch2"), "test2")
      builder.addPath(listOf(), "test")
      val tree = builder.build()

      tree shouldBe root("test") {
        branch("branch1", "test1") {
          branch("branch11", "test-b11") {
            leaf("child113", "test113")
          }
          leaf("child11", "test-c11")
          leaf("child12", "test12")
        }
        branch("branch2", "test2") {
          leaf("child23", "test23")
        }
      }
    }
  }

  describe("get") {
    it("empty") {
      val tree = root("test") {}
      tree["branch"] shouldBe null
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
      tree["child1"]?.name shouldBe "child1"
      tree[listOf("child1")]?.name shouldBe "child1"
    }

    it("path via branch to get leaf") {
      val tree = root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
        }
        leaf("child2", "test2")
      }
      tree[listOf("branch1", "child1")]?.name shouldBe "child1"
    }

    it("path via branch to get branch") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("branch11", "test11") {
            leaf("child1", "test111")
          }
        }
        leaf("child2", "test2")
      }
      tree[listOf("branch1", "branch11")]?.name shouldBe "branch11"
    }
  }

  describe("data") {
    it("empty") {
      val tree = root("test") {}
      tree.data shouldBe "test"
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.children[0].data shouldBe "test-external"
      tree.children[1].data shouldBe "test-internal"
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test2")
        }
      }
      tree.children[0].data shouldBe "test-external"
      tree.children[0].children[0].name shouldBe "child1"
      tree.children[0].children[0].data shouldBe "test1"
      tree.children[1].data shouldBe "test-internal"
      tree.children[1].children[0].data shouldBe "test2"
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test-external") {
            leaf("child1", "test1")
          }
        }
      }
      tree.children[0].data shouldBe "test1"
      tree.children[0].children[0].data shouldBe "test-external"
      tree.children[0]
        .children[0]
        .children[0]
        .data shouldBe "test1"
    }
  }

  describe("top") {
    it("empty") {
      val tree = root("test") {}
      tree.top shouldBe tree
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[1].top shouldBe tree.children[1]
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test2")
        }
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[0].children[0].top shouldBe tree.children[0]
      tree.children[1].top shouldBe tree.children[1]
      tree.children[1].children[0].top shouldBe tree.children[1]
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test1-external1")
          }
        }
      }
      tree.children[0].top shouldBe tree.children[0]
      tree.children[0].children[0].top shouldBe tree.children[0]
      tree.children[0]
        .children[0]
        .children[0]
        .top shouldBe tree.children[0]
    }
  }

  describe("path") {
    it("empty") {
      val tree = root("test") {}
      tree.path shouldBe listOf()
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.children[0].name shouldBe "external"
      tree.children[0].path shouldBe listOf("external")
      tree.children[1].path shouldBe listOf("internal")
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test2")
        }
      }
      tree.children[0].path shouldBe listOf("external")
      tree.children[0].children[0].path shouldBe listOf("external", "child1")
      tree.children[1].path shouldBe listOf("internal")
      tree.children[1].children[0].path shouldBe listOf("internal", "child2")
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
          }
        }
      }
      tree.children[0].path shouldBe listOf("branch1")
      tree.children[0].children[0].path shouldBe listOf("branch1", "external")
      tree.children[0]
        .children[0]
        .children[0]
        .path shouldBe listOf("branch1", "external", "child1")
    }
  }

  describe("index path") {
    it("on empty returns empty") {
      val tree = root("test") {}
      tree.indexPath shouldBe listOf()
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.children[0].indexPath shouldBe listOf(0)
      tree.children[1].indexPath shouldBe listOf(1)
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test2")
        }
      }
      tree.children[0].indexPath shouldBe listOf(0)
      tree.children[0].children[0].indexPath shouldBe listOf(0, 0)
      tree.children[1].indexPath shouldBe listOf(1)
      tree.children[1].children[0].indexPath shouldBe listOf(1, 0)
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
            leaf("child2", "test12")
          }
        }
      }
      tree.children[0].indexPath shouldBe listOf(0)
      tree.children[0].children[0].indexPath shouldBe listOf(0, 0)
      tree.children[0]
        .children[0]
        .children[0]
        .indexPath shouldBe listOf(0, 0, 0)
      tree.children[0]
        .children[0]
        .children[1]
        .indexPath shouldBe listOf(0, 0, 1)
    }
  }

  describe("traverse up") {
    it("empty") {
      val tree = root("test") {}
      tree.traverseBottomUp().toList() shouldBe listOf(tree)
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.traverseBottomUp().toList() shouldBe listOf(
        tree.children[0],
        tree.children[1],
        tree
      )
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test11")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test22")
        }
      }
      tree.traverseBottomUp().toList() shouldBe listOf(
        tree.children[0].children[0],
        tree.children[0],
        tree.children[1].children[0],
        tree.children[1],
        tree
      )
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
          }
        }
      }
      tree.traverseBottomUp().toList() shouldBe listOf(
        tree.children[0].children[0].children[0],
        tree.children[0].children[0],
        tree.children[0],
        tree
      )
    }
  }

  describe("traverse down") {
    it("empty") {
      val tree = root("test") {}
      tree.traverseDepthDown().toList() shouldBe listOf(tree)
    }

    it("just leafs") {
      val tree = root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
      tree.traverseDepthDown().toList() shouldBe listOf(
        tree,
        tree.children[0],
        tree.children[1]
      )
    }

    it("branched") {
      val tree = root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test")
        }
      }
      tree.traverseDepthDown().toList() shouldBe listOf(
        tree,
        tree.children[0],
        tree.children[0].children[0],
        tree.children[1],
        tree.children[1].children[0]
      )
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
          }
        }
      }
      tree.traverseDepthDown().toList() shouldBe listOf(
        tree,
        tree.children[0],
        tree.children[0].children[0],
        tree.children[0].children[0].children[0]
      )
    }
  }

  describe("builder") {
    it("empty") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf(), "test")
      builder.build() shouldBe root("test") {}
    }

    it("just leafs") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("external"), "test-external")
      builder.addPath(listOf("internal"), "test-internal")
      builder.addPath(listOf(), "test")
      builder.build() shouldBe root("test") {
        leaf("external", "test-external")
        leaf("internal", "test-internal")
      }
    }

    it("branched") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("external", "child1"), "test1")
      builder.addPath(listOf("external"), "test-external")
      builder.addPath(listOf("internal", "child2"), "test2")
      builder.addPath(listOf("internal"), "test-internal")
      builder.addPath(listOf(), "test")

      builder.build() shouldBe root("test") {
        branch("external", "test-external") {
          leaf("child1", "test1")
        }
        branch("internal", "test-internal") {
          leaf("child2", "test")
        }
      }
    }

    it("nested") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("branch1", "external", "child1"), "test11")
      builder.addPath(listOf("branch1", "external"), "test1-external")
      builder.addPath(listOf("branch1"), "test1")
      builder.addPath(listOf(), "test")

      builder.build() shouldBe root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
          }
        }
      }
    }

    it("sorted by name") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("child2"), "test2")
      builder.addPath(listOf("child1"), "test1")
      builder.addPath(listOf(), "test")
      builder.build() shouldBe root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
    }

    it("sorted by rank") {
      val builder = TreeNode.Builder<String>()
      builder.addPath(listOf("child2"), "test2", 1)
      builder.addPath(listOf("child1"), "test1", 2)
      builder.addPath(listOf(), "test")
      builder.build() shouldBe root("test") {
        leaf("child2", "test2")
        leaf("child1", "test1")
      }
    }
  }

  describe("pretty string") {
    it("single root") {
      val tree = root("test") { }
      tree.toPrettyString() shouldBe "└── \n"
    }

    it("root with children") {
      val tree = root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
      tree.toPrettyString() shouldBe
        """
        └── 
            ├── child1
            └── child2
        
        """.trimIndent()
    }

    it("nested") {
      val tree = root("test") {
        branch("branch1", "test1") {
          branch("external", "test1-external") {
            leaf("child1", "test11")
          }
        }
      }
      tree.toPrettyString() shouldBe
        """
        └── 
            └── branch1
                └── external
                    └── child1
        
        """.trimIndent()
    }
  }

  describe("comparable") {
    it("equal return zero") {
      val tree = root("test") {
        leaf("child1", "test1")
      }
      tree.children[0].compareTo(tree.children[0]) shouldBe 0
    }

    it("lesser returns negative") {
      val tree = root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
      tree.children[0].compareTo(tree.children[1]) shouldBe -1
    }

    it("greater returns positive") {
      val tree = root("test") {
        leaf("child1", "test1")
        leaf("child2", "test2")
      }
      tree.children[1].compareTo(tree.children[0]) shouldBe 1
    }

    it("same path uses tie breaker") {
      val tree = root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
        }
      }
      tree.children[0].compareTo(tree.children[0].children[0]) shouldBe -1
    }

    it("sorts correctly") {
      val tree = root("test") {
        branch("branch1", "test1") {
          leaf("child1", "test11")
        }
        branch("branch2", "test21") {
          leaf("child2", "test22")
        }
        leaf("child3", "test3")
      }
      val unsortedList = listOf(
        tree[listOf("branch1")]!!,
        tree[listOf("branch2")]!!,
        tree[listOf("child3")]!!,
        tree[listOf("branch1", "child1")]!!,
        tree[listOf("branch2", "child2")]!!,
      )
      unsortedList.sorted() shouldBe listOf(
        tree[listOf("branch1")]!!,
        tree[listOf("branch1", "child1")]!!,
        tree[listOf("branch2")]!!,
        tree[listOf("branch2", "child2")]!!,
        tree[listOf("child3")]!!,
      )
    }
  }
})
