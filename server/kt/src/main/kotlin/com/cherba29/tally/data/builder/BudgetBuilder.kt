package com.cherba29.tally.data.builder

import com.cherba29.tally.core.Account
import com.cherba29.tally.core.Account.Companion.EXTERNAL_NAME
import com.cherba29.tally.core.Account.Companion.INACTIVE_NAME
import com.cherba29.tally.core.Balance
import com.cherba29.tally.core.Month
import com.cherba29.tally.core.MonthRange
import com.cherba29.tally.core.Transaction
import com.cherba29.tally.core.plus
import com.cherba29.tally.data.Budget
import com.cherba29.tally.data.BudgetLoadingException
import com.cherba29.tally.data.Profile
import com.cherba29.tally.data.TransfersFingerprint
import com.cherba29.tally.recurring.RecurringTransactionDetector
import com.cherba29.tally.recurring.RecurringTransactionIndex
import com.cherba29.tally.recurring.TransferCandidate
import com.cherba29.tally.statement.Statement
import com.cherba29.tally.statement.TransactionStatement
import com.cherba29.tally.utils.TreeNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

class BudgetBuilder {
  // Account name -> month -> balance map.
  private val balances: MutableMap<List<String>, MutableMap<Month, Balance>> = mutableMapOf()
  private val transferRecordList: MutableList<TransferRecord> = mutableListOf()
  private val accountSourceFiles: MutableMap<List<String>, Path> = mutableMapOf()
  private val timeSource: TimeSource = TimeSource.Monotonic

  private var monthRange: MonthRange? = null

  data class TransferRecord(
    val toAccountName: String, // Full path is unknown at time of record.
    val fromAccountPath: List<String>,
    val month: Month,
    val balance: Balance,
    val description: String?,
    val tags: List<String>,
    val sourceFile: Path? = null,
    val lineNumber: Int? = null
  )

  internal data class ResolvedTransfer(
    val fromAccountNode: TreeNode.Leaf<Profile>,
    val toAccountNode: TreeNode.Leaf<Profile>,
    val month: Month,
    val balance: Balance,
    val description: String?,
    val sourceFileKey: String? = null
  )

  internal data class BuildSnapshot(
    val budget: Budget,
    val pathToLeaf: Map<List<String>, TreeNode.Leaf<Profile>>,
    val accountNameToPaths: Map<String, List<List<String>>>,
    val leafBalances: Map<TreeNode.Leaf<Profile>, Map<Month, Balance>>,
    val resolvedTransfers: List<ResolvedTransfer>,
    val transfersFingerprint: TransfersFingerprint
  )

  private val treeNodeBuilder = TreeNode.Builder<Profile>()
  private val pathToAccount = mutableMapOf<List<String>, Account>()

  fun addAccount(
    account: Account,
    accountBalances: Map<Month, Balance>,
    sourceFile: Path? = null
  ) {
    for (owner in account.owners) {
      val fullPath = listOf(owner) + account.path + listOf(account.name)
      if (sourceFile != null) {
        accountSourceFiles[fullPath] = sourceFile
      }
      setAccount(fullPath, account)
      for ((month, balance) in accountBalances) {
        setBalance(fullPath, month, balance)
      }
    }
    if (account.isExternal && account.closedOn != null) {
      val inactiveAccount = account.cloneInactive()
      val inactiveAccountBalances = mutableMapOf<Month, Balance>()

      val closingBalance = accountBalances[account.closedOn]
      if (closingBalance != null) {
        inactiveAccountBalances[inactiveAccount.openedOn] = Balance(
          0,
          inactiveAccount.openedOn.toDate(),
          Balance.Type.CONFIRMED
        )
        inactiveAccountBalances[inactiveAccount.openedOn.next()] = closingBalance
      }
      for (owner in inactiveAccount.owners) {
        val fullPath = listOf(owner) + inactiveAccount.path + listOf(inactiveAccount.name)
        setAccount(fullPath, inactiveAccount)
        for ((month, balance) in inactiveAccountBalances) {
          setBalance(fullPath, month, balance)
        }
      }
      // Zero out closed external account as its balance is transferred to virtual inactive.
      val nextMonth = account.closedOn.next()
      val zeroBalance = Balance(0, nextMonth.toDate(), Balance.Type.CONFIRMED)
      for (owner in account.owners) {
        val fullPath = listOf(owner) + account.path + listOf(account.name)
        setBalance(fullPath, nextMonth, zeroBalance)
      }

      if (closingBalance != null) {
        addAccountTransfer(
          account,
          inactiveAccount.name,
          account.closedOn,
          closingBalance.copy(date = inactiveAccount.openedOn.toDate()),
          "Moving ending balance to corresponding inactive account"
        )
      }
    }
  }

  private val processedPath = mutableSetOf<List<String>>()

  fun setAccount(
    fullPath: List<String>,
    account: Account
  ): BudgetBuilder {
    for (i in fullPath.indices) {
      val subPath = fullPath.subList(0, i)
      if (processedPath.add(subPath)) {
        treeNodeBuilder.addPath(
          subPath,
          Profile(
            isExternal = subPath.contains(EXTERNAL_NAME),
            isInactive = subPath.contains(INACTIVE_NAME)
          )
        )
      }
    }
    treeNodeBuilder.addPath(
      fullPath,
      Profile(
        account,
        isExternal = fullPath.contains(EXTERNAL_NAME),
        isInactive = fullPath.contains(INACTIVE_NAME)
      ),
      account.rank
    )

    pathToAccount[fullPath] = account
    monthRange += account.openedOn
    monthRange += account.closedOn
    return this
  }

  fun setBalance(
    accountPath: List<String>,
    month: Month,
    balance: Balance
  ): BudgetBuilder {
    val accountBalances = balances.getOrPut(accountPath) { mutableMapOf() }
    if (accountBalances.put(month, balance) != null) {
      throw IllegalArgumentException(
        "Balance for '${accountPath.joinToString("/")}' '$month' is already set to $balance"
      )
    }
    monthRange += month
    return this
  }

  fun addAccountTransfer(
    fromAccount: Account,
    toAccountName: String,
    month: Month,
    balance: Balance,
    description: String? = null,
    tags: List<String> = listOf(),
    sourceFile: Path? = null,
    lineNumber: Int? = null
  ) {
    for (owner in fromAccount.owners) {
      val fromAccountPath = listOf(owner) + fromAccount.path + listOf(fromAccount.name)
      addTransfer(
        TransferRecord(toAccountName, fromAccountPath, month, balance, description, tags, sourceFile, lineNumber)
      )
    }
  }

  /**
   * Add a record of transfer from given account to potentially yet unknown account name.
   */
  fun addTransfer(record: TransferRecord) {
    transferRecordList.add(record)
    monthRange += record.month
  }

  private fun buildTransactionStatements(
    treeRoot: TreeNode<Profile>,
    resolvedTransfers: List<ResolvedTransfer>
  ): Map<TreeNode.Leaf<Profile>, Map<Month, TransactionStatement>> {
    // Backfill external inactive account balances.
    for (accountNode in treeRoot.traverseLeaves()) {
      if (accountNode.data.isInactive) {
        var lastBalance: Balance? = null
        for (month in monthRange!!) {
          val accountBalances = balances.getOrPut(accountNode.path) { mutableMapOf() }
          val currentBalance = accountBalances[month]
          if (currentBalance == null) {
            if (lastBalance != null) {
              accountBalances[month] = lastBalance.copy(date = month.toDate())
            }
          } else {
            lastBalance = currentBalance
          }
        }
      }
    }
    val leafToBalances = balances.mapKeys {
      treeRoot[it.key] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path ${it.key}")
    }

    val statementBuilders = mutableMapOf<TreeNode.Leaf<Profile>, MonthTransactionStatementBuilder>()

    statementBuilders.putAll(
      treeRoot.traverseLeaves().associateWith {
        val builder = MonthTransactionStatementBuilder()
        builder.months = monthRange!!
        builder.monthlyBalances = leafToBalances[it] ?: mapOf()
        builder
      }
    )

    for ((fromAccountNode, toAccountNode, month, balance, description) in resolvedTransfers) {
      val fromOwner = fromAccountNode.top.name
      val toOwner = toAccountNode.top.name
      if (fromOwner != toOwner) {
        logger.warn {
          "WARNING: Transaction in $month has " +
            "to account ${toAccountNode.name} from ${fromAccountNode.name} with different owners " +
            "$fromOwner vs $toOwner"
        }
      }

      (statementBuilders[fromAccountNode] ?: throw IllegalStateException()).addTransfer(
        fromAccountNode,
        toAccountNode,
        month,
        -balance,
        description
      )
      (statementBuilders[toAccountNode] ?: throw IllegalStateException()).addTransfer(
        toAccountNode,
        fromAccountNode,
        month,
        balance,
        description
      )
    }
    return statementBuilders.mapValues {
      try {
        it.value.build()
      } catch (e: Exception) {
        e.addSuppressed(Exception("while processing ${it.key.pathString}"))
        throw e
      }
    }
  }

  fun validate(treeRoot: TreeNode<Profile> = treeNodeBuilder.build()): BudgetBuilder {
    validateBalances()
    resolveTransfers(treeRoot)
    return this
  }

  private fun validateBalances() {
    for ((accountPath, accountBalances) in balances) {
      val sourceFile = accountSourceFiles[accountPath] ?: continue
      validateAccountBalances(accountPath, accountBalances, sourceFile)
    }
  }

  private fun buildAccountNameToPaths(): Map<String, List<List<String>>> = pathToAccount.keys.groupBy { it.last() }

  private fun resolveTransfers(
    treeRoot: TreeNode<Profile>,
    accountNameToPaths: Map<String, List<List<String>>> = buildAccountNameToPaths()
  ): List<ResolvedTransfer> {
    val cachedNameToTreenodeMap = mutableMapOf<String, TreeNode.Leaf<Profile>>()
    val resolvedTransfers = ArrayList<ResolvedTransfer>(transferRecordList.size)

    for (record in transferRecordList) {
      val sourceFile = record.sourceFile ?: accountSourceFiles[record.fromAccountPath]
      resolvedTransfers.add(
        resolveTransferRecord(
          record = record,
          treeRoot = treeRoot,
          accountNameToPaths = accountNameToPaths,
          cachedNameToTreenodeMap = cachedNameToTreenodeMap,
          fallbackSourceFile = sourceFile
        )
      )
    }

    return resolvedTransfers
  }

  data class RecurringBuildContext(
    val previousFingerprint: TransfersFingerprint? = null,
    val previousDeferred: Deferred<RecurringTransactionIndex>? = null,
    val asyncScope: CoroutineScope? = null
  )

  fun build(recurringContext: RecurringBuildContext? = null): Budget = buildWithSnapshot(recurringContext).budget

  internal fun buildWithSnapshot(recurringContext: RecurringBuildContext? = null): BuildSnapshot {
    if (monthRange?.isEmpty() ?: true) {
      throw IllegalArgumentException("Budget must have at least one month.")
    }

    val treeRoot = treeNodeBuilder.build()
    val pathToLeaf = pathToAccount.keys.associateWith { path ->
      treeRoot[path] as? TreeNode.Leaf ?: throw IllegalStateException("Could not find path $path")
    }
    val accountNameToPaths = buildAccountNameToPaths()

    validateBalances()
    val resolvedTransfers = resolveTransfers(treeRoot, accountNameToPaths)

    val nodeToStatement = mutableMapOf<TreeNode<Profile>, Map<Month, Statement>>()
    val (numTransactions, elapsedTransactionTime) = timeSource.measureTimedValue {
      val transactionStatements = buildTransactionStatements(treeRoot, resolvedTransfers)
      nodeToStatement.putAll(transactionStatements)
      transactionStatements.values.sumOf { it.values.sumOf { stmt -> stmt.transactions.size } }
    }

    val (summaryNameMonthMap, elapsedBuildSummaryStatements) = timeSource.measureTimedValue {
      val summaryMapBuilder = SummaryMapBuilder()
      summaryMapBuilder.addAll(nodeToStatement)
      summaryMapBuilder.build(treeRoot)
    }
    nodeToStatement.putAll(summaryNameMonthMap)

    val fingerprint = computeResolvedTransfersFingerprint(resolvedTransfers)
    val (recurringDeferred, recurringLogDetail, elapsedRecurring) = scheduleRecurringIndex(
      candidatesProvider = { buildTransferCandidates(resolvedTransfers) },
      fingerprint = fingerprint,
      recurringContext = recurringContext,
      timeSource = timeSource
    )

    logger.info {
      "Build ${pathToLeaf.size} accounts, " +
        "$numTransactions transactions in $elapsedTransactionTime, " +
        "${summaryNameMonthMap.size} summaries in $elapsedBuildSummaryStatements, " +
        "$recurringLogDetail, " +
        "total in ${elapsedTransactionTime + elapsedBuildSummaryStatements + elapsedRecurring}"
    }

    val builtBudget = Budget(
      months = monthRange!!,
      tree = treeRoot,
      nodeToStatement = nodeToStatement,
      recurringIndexDeferred = recurringDeferred,
      transfersFingerprint = fingerprint
    )

    val leafBalances = pathToLeaf.entries.associate { (path, leaf) ->
      leaf to (balances[path]?.toMap() ?: emptyMap())
    }

    return BuildSnapshot(
      budget = builtBudget,
      pathToLeaf = pathToLeaf,
      accountNameToPaths = accountNameToPaths,
      leafBalances = leafBalances,
      resolvedTransfers = resolvedTransfers,
      transfersFingerprint = fingerprint
    )
  }

  companion object {
    private val logger = KotlinLogging.logger {}

    internal fun validateAccountBalances(
      accountPath: List<String>,
      accountBalances: Map<Month, Balance>,
      sourceFile: Path
    ) {
      for ((month, balance) in accountBalances) {
        val balanceMonth = Month.fromDate(balance.date)
        val balanceMonthDiff = abs(balanceMonth - month)
        if (balanceMonthDiff > 2) {
          val accountName = accountPath.last()
          val fileContext = ", while processing $sourceFile"
          throw BudgetLoadingException(
            "For $accountName account $balance and month $month are $balanceMonthDiff months apart (2 max)$fileContext",
            fileName = sourceFile.toString()
          )
        }
      }
    }

    internal fun resolveTransferRecord(
      record: TransferRecord,
      treeRoot: TreeNode<Profile>,
      accountNameToPaths: Map<String, List<List<String>>>,
      cachedNameToTreenodeMap: MutableMap<String, TreeNode.Leaf<Profile>>,
      fallbackSourceFile: Path?
    ): ResolvedTransfer {
      val (toAccountName, fromAccountPath, month, balance, description, _, sourceFile1, lineNumber) = record
      val balanceMonth = Month.fromDate(balance.date)
      val balanceMonthDiff = abs(balanceMonth - month)
      val sourceFile = sourceFile1 ?: fallbackSourceFile
      val fileContext = sourceFile?.let {
        val lineInfo = lineNumber?.let { line -> ":$line" } ?: ""
        ", while processing $it$lineInfo"
      } ?: ""

      if (balanceMonthDiff > 2 && sourceFile != null) {
        val fromAccountName = fromAccountPath.last()
        throw BudgetLoadingException(
          "For account '$fromAccountName' transfer to '$toAccountName' " +
            "for $month date ${balance.date} ($balanceMonth) are too far apart$fileContext",
          fileName = sourceFile.toString(),
          lineNumber = lineNumber
        )
      }

      val toAccountNode = cachedNameToTreenodeMap.getOrPut(toAccountName) {
        val toAccounts = accountNameToPaths[toAccountName].orEmpty()
        if (toAccounts.isEmpty()) {
          throw BudgetLoadingException(
            "Unknown to account $toAccountName in " +
              "${fromAccountPath.joinToString("/")}, " +
              "known accounts\n${treeRoot.toPrettyString()}$fileContext",
            fileName = sourceFile?.toString(),
            lineNumber = lineNumber
          )
        } else if (toAccounts.size > 1) {
          throw BudgetLoadingException(
            "Ambiguous transfer from ${fromAccountPath.joinToString("/")} to " +
              "$toAccountName, found multiple candidate accounts " +
              toAccounts.joinToString { it.joinToString("/") } + fileContext,
            fileName = sourceFile?.toString(),
            lineNumber = lineNumber
          )
        }

        treeRoot[toAccounts.first()] as? TreeNode.Leaf
          ?: throw IllegalStateException("Unknown account path ${toAccounts.first().joinToString("/")}")
      }

      val fromAccountNode = treeRoot[fromAccountPath] as? TreeNode.Leaf
        ?: throw BudgetLoadingException(
          "Unknown account ${fromAccountPath.joinToString("/")}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )

      val fromAccount = fromAccountNode.data.account
        ?: throw IllegalStateException("Node ${fromAccountNode.pathString} has no matching account entry")
      if (fromAccount.isClosed(month)) {
        throw BudgetLoadingException(
          "Account ${fromAccountNode.pathString} " +
            "has transfer during closed month $month to ${toAccountNode.pathString}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )
      }

      val toAccount = toAccountNode.data.account
        ?: throw IllegalStateException("Node ${toAccountNode.pathString} has no matching account entry")
      if (toAccount.isClosed(month)) {
        throw BudgetLoadingException(
          "Account ${toAccountNode.pathString} " +
            "has transfer during closed month $month from ${fromAccountNode.pathString}$fileContext",
          fileName = sourceFile?.toString(),
          lineNumber = lineNumber
        )
      }

      return ResolvedTransfer(
        fromAccountNode = fromAccountNode,
        toAccountNode = toAccountNode,
        month = month,
        balance = balance,
        description = description,
        sourceFileKey = sourceFile1?.toString()
      )
    }

    internal fun canonicalTransferType(
      fromNode: TreeNode.Leaf<Profile>,
      toNode: TreeNode.Leaf<Profile>,
      amount: Long
    ): Transaction.Type {
      val fromIsExternal = fromNode.data.isExternal
      val toIsExternal = toNode.data.isExternal
      return when {
        !fromIsExternal && !toIsExternal && fromNode.top.name == toNode.top.name ->
          Transaction.Type.TRANSFER
        fromIsExternal && !toIsExternal ->
          if (amount >= 0) Transaction.Type.INCOME else Transaction.Type.EXPENSE
        else ->
          if (amount >= 0) Transaction.Type.EXPENSE else Transaction.Type.INCOME
      }
    }

    internal fun buildTransferCandidates(resolvedTransfers: List<ResolvedTransfer>): List<TransferCandidate> =
      resolvedTransfers.mapIndexed { idx, transfer ->
        val fromNode = transfer.fromAccountNode
        val toNode = transfer.toAccountNode
        TransferCandidate(
          transferIndex = idx,
          owner = fromNode.top.name,
          fromAccountPath = fromNode.pathString,
          fromAccountName = fromNode.name,
          toAccountPath = toNode.pathString,
          toAccountName = toNode.name,
          month = transfer.month,
          balance = transfer.balance,
          description = transfer.description,
          type = canonicalTransferType(fromNode, toNode, transfer.balance.amount)
        )
      }

    internal fun computeResolvedTransfersFingerprint(transfers: List<ResolvedTransfer>): TransfersFingerprint {
      var sumHash = 0L
      var xorHash = 0L
      for (transfer in transfers) {
        val fromNode = transfer.fromAccountNode
        val toNode = transfer.toAccountNode
        val canonicalType = canonicalTransferType(fromNode, toNode, transfer.balance.amount)
        var h = 1125899906842597L
        h = h * 31L + fromNode.top.name.hashCode()
        h = h * 31L + fromNode.pathString.hashCode()
        h = h * 31L + toNode.pathString.hashCode()
        h = h * 31L + transfer.month.hashCode()
        h = h * 31L + transfer.balance.amount
        h = h * 31L + transfer.balance.date.toEpochDays()
        h = h * 31L + transfer.balance.type.ordinal
        h = h * 31L + (transfer.description?.hashCode() ?: 0)
        h = h * 31L + canonicalType.ordinal
        val mixed = splitMix64(h)
        sumHash += mixed
        xorHash = xorHash xor mixed
      }
      return TransfersFingerprint(transfers.size, sumHash, xorHash)
    }

    internal fun scheduleRecurringIndex(
      candidatesProvider: () -> List<TransferCandidate>,
      fingerprint: TransfersFingerprint,
      recurringContext: RecurringBuildContext?,
      timeSource: TimeSource
    ): Triple<Deferred<RecurringTransactionIndex>, String, kotlin.time.Duration> {
      val prevDeferred = recurringContext?.previousDeferred
      return if (recurringContext?.previousFingerprint == fingerprint &&
        prevDeferred != null &&
        !prevDeferred.isCancelled
      ) {
        Triple(prevDeferred, "reused recurring index (transfers unchanged)", kotlin.time.Duration.ZERO)
      } else if (recurringContext?.asyncScope != null) {
        if (prevDeferred != null && prevDeferred.isActive) {
          prevDeferred.cancel()
        }
        val deferred = recurringContext.asyncScope.async(Dispatchers.Default) {
          val (index, bgElapsed) = timeSource.measureTimedValue {
            RecurringTransactionDetector().detect(candidatesProvider())
          }
          logger.info { "Computed ${index.groups.size} recurring groups asynchronously in $bgElapsed" }
          index
        }
        Triple(deferred, "scheduled recurring detection asynchronously", kotlin.time.Duration.ZERO)
      } else {
        val (index, syncElapsed) = timeSource.measureTimedValue {
          RecurringTransactionDetector().detect(candidatesProvider())
        }
        Triple(CompletableDeferred(index), "${index.groups.size} recurring groups in $syncElapsed", syncElapsed)
      }
    }

    private fun splitMix64(seed: Long): Long {
      var z = seed + -7046029254386353131L
      z = (z xor (z ushr 30)) * -4658895280553007687L
      z = (z xor (z ushr 27)) * -7723592293110705685L
      return z xor (z ushr 31)
    }
  }
}

fun budget(
  recurringContext: BudgetBuilder.RecurringBuildContext? = null,
  block: BudgetBuilder.() -> Unit
): Budget {
  val builder = BudgetBuilder()
  block(builder)
  return builder.build(recurringContext)
}
