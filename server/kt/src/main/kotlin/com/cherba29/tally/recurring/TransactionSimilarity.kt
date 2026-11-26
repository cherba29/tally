package com.cherba29.tally.recurring

import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-signal pairwise similarity scorer for recurring transaction detection.
 */
object TransactionSimilarity {
  private const val MAX_RELATIVE_AMOUNT_DIFF = 0.35

  /**
   * Computes a similarity score in `[0.0, 1.0]` between two transfer candidates.
   * Returns `0.0` immediately when hard disqualifiers apply (different owner, type,
   * direction, unrelated accounts, or incompatible descriptions).
   */
  fun score(
    a: TransferCandidate,
    aNorm: NormalizedDescription,
    b: TransferCandidate,
    bNorm: NormalizedDescription,
    config: RecurringDetectionConfig = RecurringDetectionConfig()
  ): Double {
    if (a.owner != b.owner) return 0.0
    if (a.type != b.type) return 0.0

    // Reversed flow sign (e.g. payment vs refund) should not merge.
    val aSign = if (a.balance.amount >= 0) 1 else -1
    val bSign = if (b.balance.amount >= 0) 1 else -1
    if (aSign != bSign) return 0.0

    // Opposite direction between the same pair of accounts should not merge.
    if (a.fromAccountPath == b.toAccountPath && a.toAccountPath == b.fromAccountPath &&
      a.fromAccountPath != a.toAccountPath
    ) {
      return 0.0
    }

    val accountScore = accountSimilarity(a, b)
    if (accountScore == 0.0) return 0.0

    val amountScore = amountSimilarity(a.balance.amount, b.balance.amount)
    val timingScore = timingSimilarity(a.balance.date, b.balance.date)

    // Both descriptions empty: allow matching only when exact accounts match and
    // amount or timing regularity is strong.
    if (aNorm.isEmpty && bNorm.isEmpty) {
      if (accountScore < 1.0) return 0.0
      if (amountScore < 0.9 && timingScore < 0.8) return 0.0
      return 0.45 * amountScore + 0.35 * timingScore + 0.20 * accountScore
    }

    // One empty and one non-empty description: treat as non-matching.
    if (aNorm.isEmpty || bNorm.isEmpty) return 0.0

    val descScore = descriptionSimilarity(aNorm, bNorm, config.minDescriptionSimilarity)
    if (descScore < config.minDescriptionSimilarity) return 0.0

    val totalWeight = config.descWeight + config.accountWeight + config.amountWeight + config.timingWeight
    if (totalWeight <= 0.0) return 0.0

    val weighted =
      config.descWeight * descScore +
        config.accountWeight * accountScore +
        config.amountWeight * amountScore +
        config.timingWeight * timingScore
    return (weighted / totalWeight).coerceIn(0.0, 1.0)
  }

  /**
   * Computes description similarity in `[0.0, 1.0]` combining token Dice similarity,
   * subset containment (for descriptions with extra notes), and normalized Levenshtein similarity.
   *
   * When [minRequired] > 0 and an exact upper bound proves the result cannot reach [minRequired],
   * skips the O(L^2) Levenshtein computation and returns `0.0`.
   */
  fun descriptionSimilarity(
    a: NormalizedDescription,
    b: NormalizedDescription,
    minRequired: Double = 0.0
  ): Double {
    if (a.isEmpty || b.isEmpty) return 0.0
    if (a.normalizedText == b.normalizedText) return 1.0

    val (smallerTokens, largerTokens) = if (a.tokenSet.size <= b.tokenSet.size) {
      a.tokenSet to b.tokenSet
    } else {
      b.tokenSet to a.tokenSet
    }
    var intersectionSize = 0
    for (tok in smallerTokens) {
      if (tok in largerTokens) intersectionSize++
    }
    val dice = (2.0 * intersectionSize) / (a.tokenSet.size + b.tokenSet.size).toDouble()

    val smallerSize = smallerTokens.size
    val largerSize = largerTokens.size
    val containment = if (smallerSize >= 2 && intersectionSize == smallerSize) {
      (0.85 + 0.15 * (smallerSize.toDouble() / largerSize.toDouble())) * 0.90
    } else {
      0.0
    }

    val lenA = a.normalizedText.length
    val lenB = b.normalizedText.length
    val maxLen = max(lenA, lenB)
    if (maxLen == 0) return 1.0

    // Upper bound on normalized Levenshtein similarity is minLen / maxLen.
    if (minRequired > 0.0 && containment < minRequired) {
      val maxPossibleLev = min(lenA, lenB).toDouble() / maxLen.toDouble()
      if (0.65 * dice + 0.35 * maxPossibleLev < minRequired) {
        return 0.0
      }
    }

    val lev = 1.0 - levenshteinDistance(a.normalizedText, b.normalizedText).toDouble() / maxLen.toDouble()
    val blended = 0.65 * dice + 0.35 * lev
    return max(blended, containment).coerceIn(0.0, 1.0)
  }

  /**
   * Scores account overlap:
   * - `1.0` if both source and destination match
   * - `0.5` if either source or destination matches (e.g. a subscription moved across cards)
   * - `0.0` otherwise
   */
  fun accountSimilarity(
    a: TransferCandidate,
    b: TransferCandidate
  ): Double =
    when {
      a.fromAccountPath == b.fromAccountPath && a.toAccountPath == b.toAccountPath -> 1.0
      a.fromAccountPath == b.fromAccountPath || a.toAccountPath == b.toAccountPath -> 0.5
      else -> 0.0
    }

  /**
   * Scores closeness of two amounts in cents using smooth linear decay up to [MAX_RELATIVE_AMOUNT_DIFF].
   */
  fun amountSimilarity(
    amountA: Long,
    amountB: Long
  ): Double {
    val absA = abs(amountA)
    val absB = abs(amountB)
    if (absA == absB) return 1.0
    val maxAmt = max(absA, absB)
    if (maxAmt == 0L) return 1.0
    val relDiff = abs(absA - absB).toDouble() / maxAmt.toDouble()
    return max(0.0, 1.0 - relDiff / MAX_RELATIVE_AMOUNT_DIFF)
  }

  /**
   * Scores temporal alignment using circular day-of-month proximity (handling month-end drift
   * and weekend shifts) with a fallback check for weekly/bi-weekly intervals.
   */
  fun timingSimilarity(
    dateA: LocalDate,
    dateB: LocalDate
  ): Double {
    val rawDayDiff = abs(dateA.day - dateB.day)
    val circularDayDiff = min(rawDayDiff, 30 - rawDayDiff)
    if (circularDayDiff <= 3) return 1.0
    if (circularDayDiff <= 6) return 0.5

    val daysBetween = abs(dateA.daysUntil(dateB))
    if (daysBetween >= 6) {
      val mod7 = daysBetween % 7
      if (mod7 <= 1 || mod7 == 6) return 0.7
      val mod14 = daysBetween % 14
      if (mod14 <= 2 || mod14 >= 12) return 0.7
    }
    return 0.0
  }

  /**
   * Computes standard Levenshtein edit distance using O(min(m, n)) memory.
   */
  internal fun levenshteinDistance(
    s: String,
    t: String
  ): Int {
    if (s == t) return 0
    if (s.isEmpty()) return t.length
    if (t.isEmpty()) return s.length

    val (shorter, longer) = if (s.length <= t.length) s to t else t to s
    var prev = IntArray(shorter.length + 1) { it }
    var curr = IntArray(shorter.length + 1)

    for (i in 1..longer.length) {
      curr[0] = i
      val chLong = longer[i - 1]
      for (j in 1..shorter.length) {
        val cost = if (chLong == shorter[j - 1]) 0 else 1
        curr[j] = min(
          min(curr[j - 1] + 1, prev[j] + 1),
          prev[j - 1] + cost
        )
      }
      val tmp = prev
      prev = curr
      curr = tmp
    }
    return prev[shorter.length]
  }
}
