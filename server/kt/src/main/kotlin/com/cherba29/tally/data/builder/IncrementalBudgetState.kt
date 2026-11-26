package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.ParsedYamlAccount
import com.cherba29.tally.data.Profile
import com.cherba29.tally.data.TransfersFingerprint
import com.cherba29.tally.data.extractYamlAccountData
import com.cherba29.tally.data.yaml.YamlData
import com.cherba29.tally.recurring.TransferCandidate
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.TreeNode
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

/**
 * Maintains per-file and per-leaf statement/transfer state so edits to balances or transfers in one
 * or a few YAML files only rebuild the affected `(leaf, month)` transaction statements and their
 * ancestor summary statements, falling back to a full rebuild on structural or range changes.
 */
internal class IncrementalBudgetState private constructor(
  var budget: Budget,
  private val pathToLeaf: Map<List<String>, TreeNode.Leaf<Profile>>,
  private val accountNameToPaths: Map<String, List<List<String>>>,
  private val sharedAccountPaths: Set<List<String>>,
  private val files: LinkedHashMap<String, FileState>,
  private val otherResolvedTransfers: List<BudgetBuilder.ResolvedTransfer>,
  private val leafMonthFileTransactions:
    MutableMap<TreeNode.Leaf<Profile>, MutableMap<Month, MutableMap<String, List<Transaction>>>>,
  private val leafBalances: MutableMap<TreeNode.Leaf<Profile>, Map<Month, Balance>>,
  private var totalFingerprint: TransfersFingerprint,
  private val timeSource: TimeSource
) {
  internal data class FileState(
    val relativeFilePath: Path,
    val account: Account,
    val accountPaths: List<List<String>>,
    val balances: Map<Month, Balance>,
    val resolvedTransfers: List<BudgetBuilder.ResolvedTransfer>,
    val transferFingerprint: TransfersFingerprint,
    val minMonth: Month,
    val maxMonth: Month,
    val leafContributions: Map<TreeNode.Leaf<Profile>, Map<Month, List<Transaction>>>
  )

  fun hasFile(fileKey: String): Boolean = fileKey in files

  /**
   * Attempts an incremental rebuild for [dirtyYamlFiles].
   *
   * Returns the updated [Budget] on success, or `null` if a full rebuild is required (e.g.,
   * account metadata changed, month range expanded or may shrink, closed external account edited,
   * or file added/removed/became non-account). Validation errors propagate directly without
   * mutating internal state.
   */
  fun tryApplyDirtyFiles(
    dirtyYamlFiles: Map<String, YamlData>,
    recurringContext: BudgetBuilder.RecurringBuildContext?
  ): Budget? {
    if (dirtyYamlFiles.isEmpty()) {
      return budget
    }

    val range = budget.months
    val cachedNameToLeaf = mutableMapOf<String, TreeNode.Leaf<Profile>>()
    val pendingFiles = LinkedHashMap<String, FileState>(dirtyYamlFiles.size)

    // Phase 1: Eligibility checks, YAML conversion, and balance/transfer validation (no state mutation).
    for ((fileKey, yamlData) in dirtyYamlFiles) {
      val oldFile = files[fileKey] ?: return null
      val parsed = extractYamlAccountData(yamlData, oldFile.relativeFilePath) ?: return null

      if (parsed.account != oldFile.account) return null
      if (oldFile.account.isExternal && oldFile.account.closedOn != null) return null
      if (oldFile.accountPaths.any { it in sharedAccountPaths }) return null

      val (newMinMonth, newMaxMonth) = computeMonthBounds(parsed)
      if (newMinMonth < range.first || newMaxMonth > range.last) return null
      if (oldFile.minMonth == range.first && newMinMonth > range.first) return null
      if (oldFile.maxMonth == range.last && newMaxMonth < range.last) return null

      for (accountPath in oldFile.accountPaths) {
        BudgetBuilder.validateAccountBalances(accountPath, parsed.balances, oldFile.relativeFilePath)
      }

      val resolved = ArrayList<BudgetBuilder.ResolvedTransfer>(parsed.transfers.size * oldFile.accountPaths.size)
      for (transfer in parsed.transfers) {
        for (fromAccountPath in oldFile.accountPaths) {
          val record = BudgetBuilder.TransferRecord(
            toAccountName = transfer.toAccountName,
            fromAccountPath = fromAccountPath,
            month = transfer.month,
            balance = transfer.balance,
            description = transfer.description,
            tags = transfer.tags,
            sourceFile = oldFile.relativeFilePath
          )
          resolved.add(
            BudgetBuilder.resolveTransferRecord(
              record = record,
              treeRoot = budget.tree,
              accountNameToPaths = accountNameToPaths,
              cachedNameToTreenodeMap = cachedNameToLeaf,
              fallbackSourceFile = oldFile.relativeFilePath
            )
          )
        }
      }

      val contributions = buildLeafContributions(resolved, warnCrossOwner = true)
      val fileFingerprint = BudgetBuilder.computeResolvedTransfersFingerprint(resolved)
      pendingFiles[fileKey] = FileState(
        relativeFilePath = oldFile.relativeFilePath,
        account = parsed.account,
        accountPaths = oldFile.accountPaths,
        balances = parsed.balances,
        resolvedTransfers = resolved,
        transferFingerprint = fileFingerprint,
        minMonth = newMinMonth,
        maxMonth = newMaxMonth,
        leafContributions = contributions
      )
    }

    // Phase 2: Diff dirty (leaf, month) pairs and rebuild only affected TransactionStatements.
    val dirtyLeafMonths = mutableMapOf<TreeNode.Leaf<Profile>, MutableSet<Month>>()
    val pendingLeafBalances = mutableMapOf<TreeNode.Leaf<Profile>, Map<Month, Balance>>()

    fun markDirtyMonth(
      leaf: TreeNode.Leaf<Profile>,
      month: Month
    ) {
      val set = dirtyLeafMonths.getOrPut(leaf) { mutableSetOf() }
      set.add(month)
      val prev = month.previous()
      if (prev >= range.first) {
        set.add(prev)
      }
    }

    for ((fileKey, newFile) in pendingFiles) {
      val oldFile = files.getValue(fileKey)
      for (accountPath in oldFile.accountPaths) {
        val leaf = pathToLeaf.getValue(accountPath)
        pendingLeafBalances[leaf] = newFile.balances
        val allBalanceMonths = oldFile.balances.keys + newFile.balances.keys
        for (month in allBalanceMonths) {
          if (oldFile.balances[month] != newFile.balances[month]) {
            markDirtyMonth(leaf, month)
          }
        }
      }

      val touchedLeaves = oldFile.leafContributions.keys + newFile.leafContributions.keys
      for (leaf in touchedLeaves) {
        val oldByMonth = oldFile.leafContributions[leaf].orEmpty()
        val newByMonth = newFile.leafContributions[leaf].orEmpty()
        val touchedMonths = oldByMonth.keys + newByMonth.keys
        for (month in touchedMonths) {
          if (oldByMonth[month] != newByMonth[month]) {
            markDirtyMonth(leaf, month)
          }
        }
      }
    }

    val (rebuiltLeafStatements, elapsedLeafTime) = timeSource.measureTimedValue {
      val rebuilt = mutableMapOf<TreeNode.Leaf<Profile>, MutableMap<Month, TransactionStatement>>()
      for ((leaf, monthsSet) in dirtyLeafMonths) {
        val sortedDesc = monthsSet.sortedDescending()
        val balancesForLeaf = pendingLeafBalances[leaf] ?: leafBalances[leaf].orEmpty()
        val leafRebuilt = mutableMapOf<Month, TransactionStatement>()
        rebuilt[leaf] = leafRebuilt

        for (month in sortedDesc) {
          val nextMonth = month.next()
          val nextStmt: TransactionStatement? = if (nextMonth > range.last) {
            null
          } else {
            leafRebuilt[nextMonth]
              ?: (budget.nodeToStatement[leaf]?.get(nextMonth) as? TransactionStatement)
          }

          val stmt = try {
            transactionStatement {
              this.month = month
              startBalance = balancesForLeaf[month]
              val committedByFile = leafMonthFileTransactions[leaf]?.get(month)
              if (committedByFile != null) {
                for ((srcFileKey, txList) in committedByFile) {
                  if (srcFileKey !in pendingFiles) {
                    for (tx in txList) {
                      addTransfer(tx)
                    }
                  }
                }
              }
              for ((_, newFile) in pendingFiles) {
                val pendingTxList = newFile.leafContributions[leaf]?.get(month)
                if (pendingTxList != null) {
                  for (tx in pendingTxList) {
                    addTransfer(tx)
                  }
                }
              }
              endBalance = nextStmt?.startBalance
              isCovered = endBalance == null || endBalance!!.amount >= 0 || (nextStmt?.coversPrevious ?: false)
              isProjectedCovered = isCovered || (nextStmt?.coversProjectedPrevious ?: false)
            }
          } catch (e: Exception) {
            e.addSuppressed(Exception("while processing ${leaf.pathString}"))
            throw e
          }
          leafRebuilt[month] = stmt
        }
      }
      rebuilt
    }

    // Phase 3: Rebuild only dirty ancestor (branch, month) SummaryStatements bottom-up.
    val (summaryResult, elapsedSummaryTime) = timeSource.measureTimedValue {
      val newNodeToStatement = LinkedHashMap(budget.nodeToStatement)
      for ((leaf, rebuiltMonths) in rebuiltLeafStatements) {
        val updatedLeafMap = LinkedHashMap(budget.nodeToStatement.getValue(leaf))
        updatedLeafMap.putAll(rebuiltMonths)
        newNodeToStatement[leaf] = updatedLeafMap
      }

      val dirtyBranchMonths = mutableMapOf<TreeNode<Profile>, MutableSet<Month>>()
      for ((leaf, rebuiltMonths) in rebuiltLeafStatements) {
        var ancestor: TreeNode<Profile>? = leaf.parent
        while (ancestor != null && ancestor.parent != null) {
          dirtyBranchMonths.getOrPut(ancestor) { mutableSetOf() }.addAll(rebuiltMonths.keys)
          ancestor = ancestor.parent
        }
      }

      var rebuiltSummaries = 0
      val branchesBottomUp = dirtyBranchMonths.entries.sortedByDescending { it.key.path.size }
      for ((branchNode, dirtyMonths) in branchesBottomUp) {
        val updatedBranchMap = LinkedHashMap(budget.nodeToStatement.getValue(branchNode))
        for (month in dirtyMonths) {
          val summaryBuilder = MonthSummaryStatementBuilder()
          for (child in branchNode.children) {
            val childStmt = newNodeToStatement[child]?.get(month)
              ?: throw IllegalStateException(
                "Failed to build summary for ${branchNode.pathString} for month $month: missing ${child.pathString}"
              )
            summaryBuilder.addStatement(child, childStmt)
          }
          updatedBranchMap[month] = summaryBuilder.build()
          rebuiltSummaries++
        }
        newNodeToStatement[branchNode] = updatedBranchMap
      }
      newNodeToStatement to rebuiltSummaries
    }
    val (newNodeToStatement, rebuiltSummaryCount) = summaryResult

    // Phase 4: Compute updated TransfersFingerprint and commit state transactionally.
    var count = totalFingerprint.count
    var sumHash = totalFingerprint.sumHash
    var xorHash = totalFingerprint.xorHash
    for ((fileKey, newFile) in pendingFiles) {
      val oldFile = files.getValue(fileKey)
      count = count - oldFile.transferFingerprint.count + newFile.transferFingerprint.count
      sumHash = sumHash - oldFile.transferFingerprint.sumHash + newFile.transferFingerprint.sumHash
      xorHash = xorHash xor oldFile.transferFingerprint.xorHash xor newFile.transferFingerprint.xorHash
    }
    val newFingerprint = TransfersFingerprint(count, sumHash, xorHash)

    for ((fileKey, newFile) in pendingFiles) {
      val oldFile = files.getValue(fileKey)
      removeContributions(leafMonthFileTransactions, fileKey, oldFile.leafContributions)
      addContributions(leafMonthFileTransactions, fileKey, newFile.leafContributions)
      files[fileKey] = newFile
    }
    leafBalances.putAll(pendingLeafBalances)
    totalFingerprint = newFingerprint

    val transferListsSnapshot: List<List<BudgetBuilder.ResolvedTransfer>>? =
      if (recurringContext?.previousFingerprint == newFingerprint &&
        recurringContext.previousDeferred != null &&
        !recurringContext.previousDeferred.isCancelled
      ) {
        null
      } else {
        ArrayList<List<BudgetBuilder.ResolvedTransfer>>(files.size + 1).apply {
          for (file in files.values) {
            if (file.resolvedTransfers.isNotEmpty()) {
              add(file.resolvedTransfers)
            }
          }
          if (otherResolvedTransfers.isNotEmpty()) {
            add(otherResolvedTransfers)
          }
        }
      }

    val (recurringDeferred, recurringLogDetail, elapsedRecurring) = BudgetBuilder.scheduleRecurringIndex(
      candidatesProvider = { buildTransferCandidatesFromLists(transferListsSnapshot.orEmpty()) },
      fingerprint = newFingerprint,
      recurringContext = recurringContext,
      timeSource = timeSource
    )

    val rebuiltLeafCount = rebuiltLeafStatements.values.sumOf { it.size }
    logger.info {
      "Incremental rebuild ${pendingFiles.size} file(s), " +
        "$rebuiltLeafCount leaf statements in $elapsedLeafTime, " +
        "$rebuiltSummaryCount summary statements in $elapsedSummaryTime, " +
        "$recurringLogDetail, " +
        "total in ${elapsedLeafTime + elapsedSummaryTime + elapsedRecurring}"
    }

    val newBudget = Budget(
      months = range,
      tree = budget.tree,
      nodeToStatement = newNodeToStatement,
      recurringIndexDeferred = recurringDeferred,
      transfersFingerprint = newFingerprint
    )
    budget = newBudget
    return newBudget
  }

  companion object {
    private val logger = KotlinLogging.logger {}
    private const val SYNTHETIC_FILE_KEY = ""

    fun fromFullBuild(
      snapshot: BudgetBuilder.BuildSnapshot,
      parsedFiles: Map<String, Pair<Path, ParsedYamlAccount>>,
      timeSource: TimeSource
    ): IncrementalBudgetState {
      val pathOwnerCounts = mutableMapOf<List<String>, Int>()
      val transfersByFile = mutableMapOf<String, MutableList<BudgetBuilder.ResolvedTransfer>>()
      val otherTransfers = mutableListOf<BudgetBuilder.ResolvedTransfer>()

      for (transfer in snapshot.resolvedTransfers) {
        val key = transfer.sourceFileKey
        if (key != null && key in parsedFiles) {
          transfersByFile.getOrPut(key) { mutableListOf() }.add(transfer)
        } else {
          otherTransfers.add(transfer)
        }
      }

      val files = LinkedHashMap<String, FileState>(parsedFiles.size)
      val leafMonthFileTransactions =
        mutableMapOf<TreeNode.Leaf<Profile>, MutableMap<Month, MutableMap<String, List<Transaction>>>>()

      for ((fileKey, entry) in parsedFiles) {
        val (relativePath, parsed) = entry
        val accountPaths = parsed.account.owners.map { owner ->
          listOf(owner) + parsed.account.path + listOf(parsed.account.name)
        }
        for (path in accountPaths) {
          pathOwnerCounts[path] = (pathOwnerCounts[path] ?: 0) + 1
        }
        val resolved = transfersByFile[fileKey].orEmpty()
        val contributions = buildLeafContributions(resolved, warnCrossOwner = false)
        addContributions(leafMonthFileTransactions, fileKey, contributions)
        val (minMonth, maxMonth) = computeMonthBounds(parsed)
        val fileFingerprint = BudgetBuilder.computeResolvedTransfersFingerprint(resolved)

        files[fileKey] = FileState(
          relativeFilePath = relativePath,
          account = parsed.account,
          accountPaths = accountPaths,
          balances = parsed.balances,
          resolvedTransfers = resolved,
          transferFingerprint = fileFingerprint,
          minMonth = minMonth,
          maxMonth = maxMonth,
          leafContributions = contributions
        )
      }

      if (otherTransfers.isNotEmpty()) {
        val otherContributions = buildLeafContributions(otherTransfers, warnCrossOwner = false)
        addContributions(leafMonthFileTransactions, SYNTHETIC_FILE_KEY, otherContributions)
      }

      val sharedAccountPaths = pathOwnerCounts.filterValues { it > 1 }.keys

      return IncrementalBudgetState(
        budget = snapshot.budget,
        pathToLeaf = snapshot.pathToLeaf,
        accountNameToPaths = snapshot.accountNameToPaths,
        sharedAccountPaths = sharedAccountPaths,
        files = files,
        otherResolvedTransfers = otherTransfers,
        leafMonthFileTransactions = leafMonthFileTransactions,
        leafBalances = snapshot.leafBalances.toMutableMap(),
        totalFingerprint = snapshot.transfersFingerprint,
        timeSource = timeSource
      )
    }

    private fun computeMonthBounds(parsed: ParsedYamlAccount): Pair<Month, Month> {
      var minMonth = parsed.account.openedOn
      var maxMonth = parsed.account.openedOn
      val closedOn = parsed.account.closedOn
      if (closedOn != null) {
        if (closedOn < minMonth) minMonth = closedOn
        if (closedOn > maxMonth) maxMonth = closedOn
      }
      for (month in parsed.balances.keys) {
        if (month < minMonth) minMonth = month
        if (month > maxMonth) maxMonth = month
      }
      for (transfer in parsed.transfers) {
        if (transfer.month < minMonth) minMonth = transfer.month
        if (transfer.month > maxMonth) maxMonth = transfer.month
      }
      return minMonth to maxMonth
    }

    private fun buildLeafContributions(
      resolvedTransfers: List<BudgetBuilder.ResolvedTransfer>,
      warnCrossOwner: Boolean
    ): Map<TreeNode.Leaf<Profile>, Map<Month, List<Transaction>>> {
      val result = mutableMapOf<TreeNode.Leaf<Profile>, MutableMap<Month, MutableList<Transaction>>>()
      for ((fromNode, toNode, month, balance, description) in resolvedTransfers) {
        if (warnCrossOwner) {
          val fromOwner = fromNode.top.name
          val toOwner = toNode.top.name
          if (fromOwner != toOwner) {
            logger.warn {
              "WARNING: Transaction in $month has " +
                "to account ${toNode.name} from ${fromNode.name} with different owners " +
                "$fromOwner vs $toOwner"
            }
          }
        }
        result
          .getOrPut(fromNode) { mutableMapOf() }
          .getOrPut(month) { mutableListOf() }
          .add(
            Transaction(
              targetTreeNode = toNode,
              balance = -balance,
              description = description,
              type = Transaction.typeOf(fromNode, toNode, -balance.amount)
            )
          )
        result
          .getOrPut(toNode) { mutableMapOf() }
          .getOrPut(month) { mutableListOf() }
          .add(
            Transaction(
              targetTreeNode = fromNode,
              balance = balance,
              description = description,
              type = Transaction.typeOf(toNode, fromNode, balance.amount)
            )
          )
      }
      return result
    }

    private fun addContributions(
      index: MutableMap<TreeNode.Leaf<Profile>, MutableMap<Month, MutableMap<String, List<Transaction>>>>,
      fileKey: String,
      contributions: Map<TreeNode.Leaf<Profile>, Map<Month, List<Transaction>>>
    ) {
      for ((leaf, byMonth) in contributions) {
        val leafMap = index.getOrPut(leaf) { mutableMapOf() }
        for ((month, txList) in byMonth) {
          leafMap.getOrPut(month) { mutableMapOf() }[fileKey] = txList
        }
      }
    }

    private fun removeContributions(
      index: MutableMap<TreeNode.Leaf<Profile>, MutableMap<Month, MutableMap<String, List<Transaction>>>>,
      fileKey: String,
      contributions: Map<TreeNode.Leaf<Profile>, Map<Month, List<Transaction>>>
    ) {
      for ((leaf, byMonth) in contributions) {
        val leafMap = index[leaf] ?: continue
        for (month in byMonth.keys) {
          val monthMap = leafMap[month] ?: continue
          monthMap.remove(fileKey)
          if (monthMap.isEmpty()) {
            leafMap.remove(month)
          }
        }
        if (leafMap.isEmpty()) {
          index.remove(leaf)
        }
      }
    }

    private fun buildTransferCandidatesFromLists(
      transferLists: List<List<BudgetBuilder.ResolvedTransfer>>
    ): List<TransferCandidate> {
      val totalSize = transferLists.sumOf { it.size }
      val result = ArrayList<TransferCandidate>(totalSize)
      var idx = 0
      for (transfers in transferLists) {
        for (transfer in transfers) {
          val fromNode = transfer.fromAccountNode
          val toNode = transfer.toAccountNode
          result.add(
            TransferCandidate(
              transferIndex = idx++,
              owner = fromNode.top.name,
              fromAccountPath = fromNode.pathString,
              fromAccountName = fromNode.name,
              toAccountPath = toNode.pathString,
              toAccountName = toNode.name,
              month = transfer.month,
              balance = transfer.balance,
              description = transfer.description,
              type = BudgetBuilder.canonicalTransferType(fromNode, toNode, transfer.balance.amount)
            )
          )
        }
      }
      return result
    }
  }
}
