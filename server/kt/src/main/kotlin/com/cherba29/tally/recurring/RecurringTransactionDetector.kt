package com.cherba29.tally.recurring

import com.cherba29.tally.core.Transaction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Detects groups of recurring transactions from canonical transfer candidates using
 * token-blocked, anti-chaining average-linkage clustering and cadence inference.
 */
class RecurringTransactionDetector(
  private val config: RecurringDetectionConfig = RecurringDetectionConfig()
) {
  private data class NormalizedCandidate(
    val candidate: TransferCandidate,
    val normalized: NormalizedDescription
  )

  private data class PartitionKey(
    val owner: String,
    val type: Transaction.Type,
    val flowSign: Int
  )

  private class MutableCluster(first: NormalizedCandidate) {
    val anchor: NormalizedCandidate = first
    val members: MutableList<NormalizedCandidate> = mutableListOf(first)
  }

  private data class RawGroup(
    val baseIdKey: String,
    val owner: String,
    val label: String,
    val cadence: RecurringCadence,
    val typicalDayOfMonth: Int?,
    val averageAmount: Long,
    val minAmount: Long,
    val maxAmount: Long,
    val totalAmount: Long,
    val isFixedAmount: Boolean,
    val occurrences: List<RecurringOccurrence>
  )

  fun detect(candidates: List<TransferCandidate>): RecurringTransactionIndex {
    if (candidates.size < config.minOccurrences) return RecurringTransactionIndex.EMPTY

    val normalizedCandidates = candidates.map {
      NormalizedCandidate(it, DescriptionNormalizer.normalize(it.description))
    }

    val partitions = normalizedCandidates.groupBy { nc ->
      PartitionKey(
        owner = nc.candidate.owner,
        type = nc.candidate.type,
        flowSign = if (nc.candidate.balance.amount >= 0) 1 else -1
      )
    }

    val rawGroups = mutableListOf<RawGroup>()
    val sortedPartitionKeys = partitions.keys.sortedWith(
      compareBy<PartitionKey>({ it.owner }, { it.type.name }, { it.flowSign })
    )

    for (key in sortedPartitionKeys) {
      val partitionItems = partitions[key].orEmpty()
      if (partitionItems.size < config.minOccurrences) continue
      val clusters = clusterPartition(partitionItems)
      for (cluster in clusters) {
        buildRawGroupIfRecurring(cluster)?.let { rawGroups.add(it) }
      }
    }

    if (rawGroups.isEmpty()) return RecurringTransactionIndex.EMPTY

    val groups = assignStableIds(rawGroups)
    return RecurringTransactionIndex.fromGroups(groups)
  }

  private fun clusterPartition(items: List<NormalizedCandidate>): List<MutableCluster> {
    val sorted = items.sortedWith(
      compareBy<NormalizedCandidate>(
        { it.normalized.normalizedText },
        { it.candidate.fromAccountPath },
        { it.candidate.toAccountPath },
        { it.candidate.balance.date },
        { it.candidate.balance.amount },
        { it.candidate.transferIndex }
      )
    )

    val clusters = mutableListOf<MutableCluster>()
    val tokenToClusterIndices = HashMap<String, MutableList<Int>>()
    val blankPairToClusterIndices = HashMap<Pair<String, String>, MutableList<Int>>()
    val anchorMinThreshold = config.similarityThreshold * ANCHOR_SIMILARITY_FACTOR

    for (item in sorted) {
      val candidateClusterIndices = LinkedHashSet<Int>()
      if (item.normalized.isEmpty) {
        val pair = item.candidate.fromAccountPath to item.candidate.toAccountPath
        blankPairToClusterIndices[pair]?.let { candidateClusterIndices.addAll(it) }
      } else {
        for (token in item.normalized.tokenSet) {
          if (token.length >= MIN_INDEX_TOKEN_LENGTH) {
            tokenToClusterIndices[token]?.let { candidateClusterIndices.addAll(it) }
          }
        }
        // Fallback for very short single-char tokens when no longer token exists.
        if (candidateClusterIndices.isEmpty()) {
          for (token in item.normalized.tokenSet) {
            tokenToClusterIndices[token]?.let { candidateClusterIndices.addAll(it) }
          }
        }
      }

      var bestClusterIdx = -1
      var bestScore = 0.0

      for (clusterIdx in candidateClusterIndices) {
        val cluster = clusters[clusterIdx]
        val anchorScore = TransactionSimilarity.score(
          item.candidate,
          item.normalized,
          cluster.anchor.candidate,
          cluster.anchor.normalized,
          config
        )
        if (anchorScore < anchorMinThreshold) continue

        val avgScore = averageClusterSimilarity(item, cluster)
        if (avgScore >= config.similarityThreshold && avgScore > bestScore) {
          bestScore = avgScore
          bestClusterIdx = clusterIdx
        }
      }

      if (bestClusterIdx >= 0) {
        clusters[bestClusterIdx].members.add(item)
        indexClusterItem(bestClusterIdx, item, tokenToClusterIndices, blankPairToClusterIndices)
      } else {
        val newIdx = clusters.size
        clusters.add(MutableCluster(item))
        indexClusterItem(newIdx, item, tokenToClusterIndices, blankPairToClusterIndices)
      }
    }

    return clusters
  }

  private fun indexClusterItem(
    clusterIdx: Int,
    item: NormalizedCandidate,
    tokenToClusterIndices: MutableMap<String, MutableList<Int>>,
    blankPairToClusterIndices: MutableMap<Pair<String, String>, MutableList<Int>>
  ) {
    if (item.normalized.isEmpty) {
      val pair = item.candidate.fromAccountPath to item.candidate.toAccountPath
      val list = blankPairToClusterIndices.getOrPut(pair) { mutableListOf() }
      if (list.lastOrNull() != clusterIdx) list.add(clusterIdx)
    } else {
      for (token in item.normalized.tokenSet) {
        val list = tokenToClusterIndices.getOrPut(token) { mutableListOf() }
        if (list.lastOrNull() != clusterIdx) list.add(clusterIdx)
      }
    }
  }

  private fun averageClusterSimilarity(
    item: NormalizedCandidate,
    cluster: MutableCluster
  ): Double {
    val members = cluster.members
    val sample = if (members.size <= MAX_CLUSTER_SAMPLE_SIZE) {
      members
    } else {
      val step = members.size.toDouble() / MAX_CLUSTER_SAMPLE_SIZE.toDouble()
      List(MAX_CLUSTER_SAMPLE_SIZE) { i -> members[(i * step).toInt()] }
    }
    var sum = 0.0
    for (member in sample) {
      sum += TransactionSimilarity.score(
        item.candidate,
        item.normalized,
        member.candidate,
        member.normalized,
        config
      )
    }
    return sum / sample.size.toDouble()
  }

  private fun buildRawGroupIfRecurring(cluster: MutableCluster): RawGroup? {
    val members = cluster.members
    if (members.size < config.minOccurrences) return null

    val distinctDates = members.map { it.candidate.balance.date }.distinct().sorted()
    if (distinctDates.size < 2) return null

    val spanDays = distinctDates.first().daysUntil(distinctDates.last())
    if (spanDays < config.minSpanDays) return null

    val cadence = inferCadence(distinctDates)
    val typicalDayOfMonth = inferTypicalDayOfMonth(distinctDates, cadence)

    val magnitudes = members.map { abs(it.candidate.balance.amount) }
    val minAmount = magnitudes.min()
    val maxAmount = magnitudes.max()
    val totalAmount = magnitudes.sum()
    val averageAmount = (totalAmount.toDouble() / magnitudes.size.toDouble()).roundToLong()
    val isFixedAmount = minAmount == maxAmount

    val sortedOccurrences = members
      .map { nc ->
        val c = nc.candidate
        RecurringOccurrence(
          fromAccountPath = c.fromAccountPath,
          fromAccountName = c.fromAccountName,
          toAccountPath = c.toAccountPath,
          toAccountName = c.toAccountName,
          month = c.month,
          balance = c.balance,
          description = c.description.orEmpty(),
          type = c.type
        )
      }.sortedWith(
        compareByDescending<RecurringOccurrence> { it.balance.date }
          .thenByDescending { it.month }
          .thenBy { it.fromAccountPath }
          .thenBy { it.toAccountPath }
          .thenBy { it.description }
          .thenByDescending { abs(it.balance.amount) }
      )

    val primaryPair = members
      .groupingBy { it.candidate.fromAccountPath to it.candidate.toAccountPath }
      .eachCount()
      .entries
      .sortedWith(
        compareByDescending<Map.Entry<Pair<String, String>, Int>> { it.value }
          .thenBy { it.key.first }
          .thenBy { it.key.second }
      ).first()
      .key

    val primaryFromName = members
      .first { it.candidate.fromAccountPath == primaryPair.first }
      .candidate.fromAccountName
    val primaryToName = members
      .first { it.candidate.toAccountPath == primaryPair.second }
      .candidate.toAccountName

    val canonicalNormalized = members
      .map { it.normalized.normalizedText }
      .groupingBy { it }
      .eachCount()
      .entries
      .sortedWith(
        compareByDescending<Map.Entry<String, Int>> { it.value }
          .thenBy { it.key }
      ).first()
      .key

    val label = pickRepresentativeLabel(members, primaryFromName, primaryToName)
    val owner = members.first().candidate.owner
    val type = members.first().candidate.type
    val baseIdKey = "$owner|${type.name}|${primaryPair.first}|${primaryPair.second}|$canonicalNormalized"

    return RawGroup(
      baseIdKey = baseIdKey,
      owner = owner,
      label = label,
      cadence = cadence,
      typicalDayOfMonth = typicalDayOfMonth,
      averageAmount = averageAmount,
      minAmount = minAmount,
      maxAmount = maxAmount,
      totalAmount = totalAmount,
      isFixedAmount = isFixedAmount,
      occurrences = sortedOccurrences
    )
  }

  private fun pickRepresentativeLabel(
    members: List<NormalizedCandidate>,
    primaryFromName: String,
    primaryToName: String
  ): String {
    val byCleaned = members
      .mapNotNull { nc ->
        val cleaned = DescriptionNormalizer.stripStatusSuffix(nc.candidate.description)
        if (cleaned.isEmpty()) null else cleaned to nc.candidate.balance.date
      }.groupBy({ it.first }, { it.second })

    val bestCleaned = byCleaned.entries
      .sortedWith(
        compareByDescending<Map.Entry<String, List<LocalDate>>> { it.value.size }
          .thenByDescending { it.value.max() }
          .thenBy { it.key }
      ).firstOrNull()
      ?.key

    return bestCleaned ?: "$primaryFromName -> $primaryToName"
  }

  private fun assignStableIds(rawGroups: List<RawGroup>): List<RecurringGroup> {
    val sorted = rawGroups.sortedWith(
      compareBy<RawGroup>(
        { it.baseIdKey },
        { it.averageAmount },
        { it.typicalDayOfMonth ?: 0 },
        { it.label }
      )
    )

    val usedIds = HashSet<String>()
    return sorted.map { raw ->
      val baseId = shortSha256Hex(raw.baseIdKey)
      var candidateId = baseId
      var suffix = 2
      while (!usedIds.add(candidateId)) {
        candidateId = "$baseId-$suffix"
        suffix++
      }
      RecurringGroup(
        id = candidateId,
        label = raw.label,
        cadence = raw.cadence,
        typicalDayOfMonth = raw.typicalDayOfMonth,
        averageAmount = raw.averageAmount,
        minAmount = raw.minAmount,
        maxAmount = raw.maxAmount,
        totalAmount = raw.totalAmount,
        isFixedAmount = raw.isFixedAmount,
        occurrences = raw.occurrences
      )
    }
  }

  companion object {
    private const val MIN_INDEX_TOKEN_LENGTH = 2
    private const val MAX_CLUSTER_SAMPLE_SIZE = 12
    private const val ANCHOR_SIMILARITY_FACTOR = 0.85
    private const val HASH_HEX_LENGTH = 12
    private const val MAX_TYPICAL_DAY_SPREAD = 5

    internal fun inferCadence(sortedDistinctDates: List<LocalDate>): RecurringCadence {
      if (sortedDistinctDates.size < 2) return RecurringCadence.IRREGULAR
      val gaps = (0 until sortedDistinctDates.size - 1)
        .map { i -> sortedDistinctDates[i].daysUntil(sortedDistinctDates[i + 1]) }
        .sorted()
      val medianGap = gaps[gaps.size / 2]
      return when (medianGap) {
        in 5..9 -> RecurringCadence.WEEKLY
        in 12..17 -> RecurringCadence.BIWEEKLY
        in 25..36 -> RecurringCadence.MONTHLY
        in 80..100 -> RecurringCadence.QUARTERLY
        in 345..385 -> RecurringCadence.YEARLY
        else -> RecurringCadence.IRREGULAR
      }
    }

    internal fun inferTypicalDayOfMonth(
      sortedDistinctDates: List<LocalDate>,
      cadence: RecurringCadence
    ): Int? {
      if (cadence != RecurringCadence.MONTHLY &&
        cadence != RecurringCadence.QUARTERLY &&
        cadence != RecurringCadence.YEARLY
      ) {
        return null
      }
      val days = sortedDistinctDates.map { it.day }.sorted()
      val medianDay = days[days.size / 2]
      val maxCircularDeviation = days.maxOf { day ->
        val raw = abs(day - medianDay)
        min(raw, 30 - raw)
      }
      return if (maxCircularDeviation <= MAX_TYPICAL_DAY_SPREAD) medianDay else null
    }

    private fun shortSha256Hex(input: String): String {
      val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
      val hex = StringBuilder(HASH_HEX_LENGTH)
      for (i in 0 until HASH_HEX_LENGTH / 2) {
        val b = digest[i].toInt() and 0xFF
        hex.append(HEX_CHARS[b ushr 4])
        hex.append(HEX_CHARS[b and 0x0F])
      }
      return hex.toString()
    }

    private val HEX_CHARS = "0123456789abcdef".toCharArray()
  }
}
