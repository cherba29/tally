package com.cherba29.tally.statement

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.TreeNode

/**
 * For given account captures transaction entry within transaction statement.
 */
data class Transaction(
  val targetTreeNode: TreeNode,
  val balance: Balance,
  val description: String?,
  val type: Type,
  val balanceFromStart: Long?
)  : Comparable<Transaction> {
  /**
   * Order transactions by balance amount.
   */
  override fun compareTo(other: Transaction): Int {
    val eq: Int = balance.compareTo(other.balance)
    return if (eq != 0) eq
    else {
      if (targetTreeNode.name != other.targetTreeNode.name) {
        if (targetTreeNode.name < other.targetTreeNode.name) -1 else 1
      } else if (description != other.description) {
        if (description.orEmpty() < other.description.orEmpty()) -1 else 1
      } else if (type != other.type) {
        type.compareTo(other.type)
      } else {
        0
      }
    }
  }

  /**
   * Type of transaction to keep track of whether funds are incoming
   * or outgoing or just being reshuffled.
   */
  enum class Type {
    UNKNOWN,

    /**
     * Transfer between two internal accounts.
     */
    TRANSFER,

    /**
     * Transfer from external to internal account.
     */
    INCOME,

    /**
     * Transfer from internal to external account.
     */
    EXPENSE,
  }
}
