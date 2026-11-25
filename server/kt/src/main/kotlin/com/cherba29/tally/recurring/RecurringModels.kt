package com.cherba29.tally.recurring

import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transaction
import kotlinx.datetime.LocalDate

/**
 * Configuration parameters controlling recurring transaction detection.
 */
data class RecurringDetectionConfig(
  val similarityThreshold: Double = 0.72,
  val minDescriptionSimilarity: Double = 0.45,
  val descWeight: Double = 0.55,
  val accountWeight: Double = 0.20,
  val amountWeight: Double = 0.15,
  val timingWeight: Double = 0.10,
  val minOccurrences: Int = 2,
  val minSpanDays: Int = 6
) {
  init {
    require(similarityThreshold in 0.0..1.0) { "similarityThreshold must be in 0..1" }
    require(minDescriptionSimilarity in 0.0..1.0) { "minDescriptionSimilarity must be in 0..1" }
    require(descWeight >= 0.0 && accountWeight >= 0.0 && amountWeight >= 0.0 && timingWeight >= 0.0) {
      "Similarity weights must be non-negative"
    }
    require(minOccurrences >= 2) { "minOccurrences must be at least 2" }
    require(minSpanDays >= 0) { "minSpanDays must be non-negative" }
  }
}

/**
 * Inferred cadence for a group of recurring transactions.
 */
enum class RecurringCadence {
  WEEKLY,
  BIWEEKLY,
  MONTHLY,
  QUARTERLY,
  YEARLY,
  IRREGULAR
}

/**
 * Canonical (pre-mirroring) transfer candidate fed into recurring detection.
 *
 * [balance] is expressed from the outgoing transfer perspective (`fromAccount -> toAccount`),
 * matching the transfer record in YAML before it is mirrored onto the source (`-balance`)
 * and destination (`+balance`) statements.
 */
data class TransferCandidate(
  val transferIndex: Int,
  val owner: String,
  val fromAccountPath: String,
  val fromAccountName: String,
  val toAccountPath: String,
  val toAccountName: String,
  val month: Month,
  val balance: Balance,
  val description: String?,
  val type: Transaction.Type
)

/**
 * A single transaction occurrence belonging to a detected [RecurringGroup].
 */
data class RecurringOccurrence(
  val fromAccountPath: String,
  val fromAccountName: String,
  val toAccountPath: String,
  val toAccountName: String,
  val month: Month,
  val balance: Balance,
  val description: String,
  val type: Transaction.Type
)

/**
 * A detected series of similar recurring transactions with summary statistics.
 *
 * [averageAmount], [minAmount], [maxAmount], and [totalAmount] represent non-negative
 * magnitudes in cents so that `minAmount <= averageAmount <= maxAmount` always holds.
 */
data class RecurringGroup(
  val id: String,
  val label: String,
  val cadence: RecurringCadence,
  val typicalDayOfMonth: Int?,
  val averageAmount: Long,
  val minAmount: Long,
  val maxAmount: Long,
  val totalAmount: Long,
  val isFixedAmount: Boolean,
  val occurrences: List<RecurringOccurrence>
) {
  val occurrenceCount: Int get() = occurrences.size
}

/**
 * Lookup key matching a statement [Transaction] on either the source or destination account
 * back to its detected [RecurringGroup].
 */
data class TransactionLookupKey(
  val accountPath: String,
  val targetAccountPath: String,
  val month: Month,
  val date: LocalDate,
  val amount: Long,
  val balanceType: Balance.Type,
  val description: String
)

/**
 * Indexed detection results attached to a built budget for fast O(1) lookups.
 */
data class RecurringTransactionIndex(
  val groupsById: Map<String, RecurringGroup>,
  private val lookupByKey: Map<TransactionLookupKey, RecurringGroup>
) {
  val groups: Collection<RecurringGroup> get() = groupsById.values

  fun findGroupById(groupId: String): RecurringGroup? = groupsById[groupId]

  fun findGroup(
    accountPath: String,
    targetAccountPath: String,
    month: Month,
    balance: Balance,
    description: String?
  ): RecurringGroup? =
    lookupByKey[
      TransactionLookupKey(
        accountPath = accountPath,
        targetAccountPath = targetAccountPath,
        month = month,
        date = balance.date,
        amount = balance.amount,
        balanceType = balance.type,
        description = description.orEmpty()
      )
    ]

  companion object {
    val EMPTY = RecurringTransactionIndex(emptyMap(), emptyMap())

    /**
     * Builds an index from detected groups, indexing both mirrored sides
     * (`fromAccount` with `-balance` and `toAccount` with `+balance`) of every occurrence.
     */
    fun fromGroups(groups: List<RecurringGroup>): RecurringTransactionIndex {
      if (groups.isEmpty()) return EMPTY
      val groupsById = LinkedHashMap<String, RecurringGroup>(groups.size)
      val lookupByKey = HashMap<TransactionLookupKey, RecurringGroup>()
      for (group in groups) {
        groupsById[group.id] = group
        for (occ in group.occurrences) {
          val fromKey = TransactionLookupKey(
            accountPath = occ.fromAccountPath,
            targetAccountPath = occ.toAccountPath,
            month = occ.month,
            date = occ.balance.date,
            amount = -occ.balance.amount,
            balanceType = occ.balance.type,
            description = occ.description
          )
          val toKey = TransactionLookupKey(
            accountPath = occ.toAccountPath,
            targetAccountPath = occ.fromAccountPath,
            month = occ.month,
            date = occ.balance.date,
            amount = occ.balance.amount,
            balanceType = occ.balance.type,
            description = occ.description
          )
          lookupByKey[fromKey] = group
          lookupByKey[toKey] = group
        }
      }
      return RecurringTransactionIndex(groupsById, lookupByKey)
    }
  }
}
