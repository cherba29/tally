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
 * exact-template pre-grouping, rare-token (IDF) blocking, anti-chaining average-linkage
 * clustering, and cadence inference.
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

  private data class ExactTemplateKey(
    val fromAccountPath: String,
    val toAccountPath: String,
    val normalizedText: String
  )

  private class TemplateBucket(
    val normalized: NormalizedDescription,
    val members: MutableList<NormalizedCandidate>,
    val representative: NormalizedCandidate
  )

  private class TemplateCluster(first: TemplateBucket) {
    val anchor: TemplateBucket = first
    val buckets: MutableList<TemplateBucket> = mutableListOf(first)
  }

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

    val totalWeight = config.descWeight + config.accountWeight + config.amountWeight + config.timingWeight
    val exactSameAccountAlwaysMerges =
      totalWeight > 0.0 && (config.descWeight + config.accountWeight) / totalWeight >= config.similarityThreshold

    for (key in sortedPartitionKeys) {
      val partitionItems = partitions[key].orEmpty()
      if (partitionItems.size < config.minOccurrences) continue

      val blankItems = mutableListOf<NormalizedCandidate>()
      val nonBlankItems = mutableListOf<NormalizedCandidate>()
      for (item in partitionItems) {
        if (item.normalized.isEmpty) blankItems.add(item) else nonBlankItems.add(item)
      }

      if (nonBlankItems.size >= config.minOccurrences) {
        val memberGroups = if (exactSameAccountAlwaysMerges) {
          clusterNonBlankByTemplate(nonBlankItems)
        } else {
          clusterItemsDirectly(nonBlankItems)
        }
        for (members in memberGroups) {
          buildRawGroupIfRecurring(members)?.let { rawGroups.add(it) }
        }
      }

      if (blankItems.size >= config.minOccurrences) {
        val byPair = blankItems.groupBy { it.candidate.fromAccountPath to it.candidate.toAccountPath }
        for (pairItems in byPair.values) {
          if (pairItems.size < config.minOccurrences) continue
          for (members in clusterItemsDirectly(pairItems)) {
            buildRawGroupIfRecurring(members)?.let { rawGroups.add(it) }
          }
        }
      }
    }

    if (rawGroups.isEmpty()) return RecurringTransactionIndex.EMPTY

    val groups = assignStableIds(rawGroups)
    return RecurringTransactionIndex.fromGroups(groups)
  }

  /**
   * Fast two-stage clustering for non-blank descriptions:
   * 1. Pre-groups identical `(fromAccountPath, toAccountPath, normalizedText)` into compact [TemplateBucket]s
   *    in O(N) time (guaranteed to belong to the same cluster when `descWeight + accountWeight >= threshold`).
   * 2. Runs fuzzy anti-chaining clustering across the much smaller set of unique templates using
   *    exact-text indexing + rare-token (IDF) blocking so common bank words (`com`, `us`, `payment`, etc.)
   *    never cause quadratic blowup.
   */
  private fun clusterNonBlankByTemplate(items: List<NormalizedCandidate>): List<List<NormalizedCandidate>> {
    val grouped = LinkedHashMap<ExactTemplateKey, MutableList<NormalizedCandidate>>()
    for (item in items) {
      val key = ExactTemplateKey(
        fromAccountPath = item.candidate.fromAccountPath,
        toAccountPath = item.candidate.toAccountPath,
        normalizedText = item.normalized.normalizedText
      )
      grouped.getOrPut(key) { mutableListOf() }.add(item)
    }

    val buckets = grouped.values
      .map { members -> buildTemplateBucket(members) }
      .sortedWith(
        compareBy<TemplateBucket>(
          { it.normalized.normalizedText },
          { it.representative.candidate.fromAccountPath },
          { it.representative.candidate.toAccountPath },
          { it.representative.candidate.balance.date },
          { it.representative.candidate.balance.amount },
          { it.representative.candidate.transferIndex }
        )
      )

    // Compute document frequency of each token across unique templates to prune noisy high-frequency tokens.
    val tokenDocFreq = HashMap<String, Int>()
    for (bucket in buckets) {
      for (token in bucket.normalized.tokenSet) {
        tokenDocFreq[token] = (tokenDocFreq[token] ?: 0) + 1
      }
    }

    val clusters = mutableListOf<TemplateCluster>()
    val exactTextToClusterIndices = HashMap<String, MutableList<Int>>()
    val tokenToClusterIndices = HashMap<String, MutableList<Int>>()
    val anchorMinThreshold = config.similarityThreshold * ANCHOR_SIMILARITY_FACTOR

    for (bucket in buckets) {
      val candidateClusterIndices = LinkedHashSet<Int>()

      // 1. Always consider exact normalized text matches across different accounts (O(1) lookup).
      exactTextToClusterIndices[bucket.normalized.normalizedText]?.let { candidateClusterIndices.addAll(it) }

      // 2. Collect candidate clusters sharing rare/informative tokens.
      val blockingTokens = selectBlockingTokens(bucket.normalized.tokenSet, tokenDocFreq)
      for (token in blockingTokens) {
        val posting = tokenToClusterIndices[token] ?: continue
        if (posting.size <= MAX_POSTING_LIST_SIZE) {
          candidateClusterIndices.addAll(posting)
        } else {
          // Bound worst-case fan-out even when falling back to a common token.
          val start = posting.size - MAX_POSTING_LIST_SIZE
          for (i in start until posting.size) {
            candidateClusterIndices.add(posting[i])
          }
        }
      }

      var bestClusterIdx = -1
      var bestScore = 0.0

      for (clusterIdx in candidateClusterIndices) {
        val cluster = clusters[clusterIdx]
        // Fast O(1) account overlap pre-check before touching string similarity.
        if (TransactionSimilarity.accountSimilarity(
            bucket.representative.candidate,
            cluster.anchor.representative.candidate
          ) == 0.0
        ) {
          continue
        }

        val anchorScore = TransactionSimilarity.score(
          bucket.representative.candidate,
          bucket.normalized,
          cluster.anchor.representative.candidate,
          cluster.anchor.normalized,
          config
        )
        if (anchorScore < anchorMinThreshold) continue

        val avgScore = averageTemplateClusterSimilarity(bucket, cluster)
        if (avgScore >= config.similarityThreshold && avgScore > bestScore) {
          bestScore = avgScore
          bestClusterIdx = clusterIdx
        }
      }

      if (bestClusterIdx >= 0) {
        clusters[bestClusterIdx].buckets.add(bucket)
        indexTemplateBucket(bestClusterIdx, bucket, blockingTokens, exactTextToClusterIndices, tokenToClusterIndices)
      } else {
        val newIdx = clusters.size
        clusters.add(TemplateCluster(bucket))
        indexTemplateBucket(newIdx, bucket, blockingTokens, exactTextToClusterIndices, tokenToClusterIndices)
      }
    }

    return clusters.map { cluster ->
      if (cluster.buckets.size == 1) {
        cluster.buckets[0].members
      } else {
        val combined = ArrayList<NormalizedCandidate>(cluster.buckets.sumOf { it.members.size })
        for (b in cluster.buckets) {
          combined.addAll(b.members)
        }
        combined
      }
    }
  }

  private fun buildTemplateBucket(members: MutableList<NormalizedCandidate>): TemplateBucket {
    if (members.size == 1) {
      return TemplateBucket(members[0].normalized, members, members[0])
    }
    members.sortWith(
      compareBy<NormalizedCandidate>(
        { it.candidate.balance.date },
        { it.candidate.balance.amount },
        { it.candidate.transferIndex }
      )
    )
    // Pick median-amount member with median day-of-month as representative for fuzzy template comparison.
    val byAmount = members.sortedBy { it.candidate.balance.amount }
    val medianAmount = byAmount[byAmount.size / 2].candidate.balance.amount
    val byDay = members.sortedBy { it.candidate.balance.date.day }
    val medianDateMember = byDay[byDay.size / 2]
    val repCandidate = medianDateMember.candidate.copy(
      balance = medianDateMember.candidate.balance.copy(amount = medianAmount)
    )
    val representative = NormalizedCandidate(repCandidate, medianDateMember.normalized)
    return TemplateBucket(medianDateMember.normalized, members, representative)
  }

  private fun selectBlockingTokens(
    tokens: Set<String>,
    tokenDocFreq: Map<String, Int>
  ): List<String> {
    if (tokens.isEmpty()) return emptyList()
    val rare = mutableListOf<String>()
    var rarestToken: String? = null
    var rarestFreq = Int.MAX_VALUE

    for (token in tokens) {
      val freq = tokenDocFreq[token] ?: 1
      if (token.length >= MIN_INDEX_TOKEN_LENGTH && freq < rarestFreq) {
        rarestFreq = freq
        rarestToken = token
      }
      if (token.length >= MIN_BLOCKING_TOKEN_LENGTH && freq <= MAX_POSTING_LIST_SIZE) {
        rare.add(token)
      }
    }

    if (rare.isNotEmpty()) return rare
    if (rarestToken != null) return listOf(rarestToken)
    return tokens.toList()
  }

  private fun indexTemplateBucket(
    clusterIdx: Int,
    bucket: TemplateBucket,
    blockingTokens: List<String>,
    exactTextToClusterIndices: MutableMap<String, MutableList<Int>>,
    tokenToClusterIndices: MutableMap<String, MutableList<Int>>
  ) {
    val exactList = exactTextToClusterIndices.getOrPut(bucket.normalized.normalizedText) { mutableListOf() }
    if (exactList.lastOrNull() != clusterIdx) exactList.add(clusterIdx)

    for (token in blockingTokens) {
      val list = tokenToClusterIndices.getOrPut(token) { mutableListOf() }
      if (list.lastOrNull() != clusterIdx) list.add(clusterIdx)
    }
  }

  private fun averageTemplateClusterSimilarity(
    bucket: TemplateBucket,
    cluster: TemplateCluster
  ): Double {
    val buckets = cluster.buckets
    val sample = if (buckets.size <= MAX_TEMPLATE_SAMPLE_SIZE) {
      buckets
    } else {
      val step = buckets.size.toDouble() / MAX_TEMPLATE_SAMPLE_SIZE.toDouble()
      List(MAX_TEMPLATE_SAMPLE_SIZE) { i -> buckets[(i * step).toInt()] }
    }
    var sum = 0.0
    for (member in sample) {
      sum += TransactionSimilarity.score(
        bucket.representative.candidate,
        bucket.normalized,
        member.representative.candidate,
        member.normalized,
        config
      )
    }
    return sum / sample.size.toDouble()
  }

  private fun clusterItemsDirectly(items: List<NormalizedCandidate>): List<List<NormalizedCandidate>> {
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

    return clusters.map { it.members }
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

  private fun buildRawGroupIfRecurring(members: List<NormalizedCandidate>): RawGroup? {
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
    private const val MIN_BLOCKING_TOKEN_LENGTH = 3
    private const val MAX_POSTING_LIST_SIZE = 60
    private const val MAX_TEMPLATE_SAMPLE_SIZE = 6
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
