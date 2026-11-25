package com.cherba29.tally.recurring

import java.text.Normalizer
import java.util.Locale

/**
 * Normalized representation of a transaction description with volatile tokens removed.
 */
data class NormalizedDescription(
  val normalizedText: String,
  val tokens: List<String>,
  val tokenSet: Set<String>
) {
  val isEmpty: Boolean get() = normalizedText.isEmpty()

  companion object {
    val EMPTY = NormalizedDescription("", emptyList(), emptySet())
  }
}

/**
 * Strips temporary/ephemeral markers (dates, timestamps, month tokens, status annotations,
 * confirmation/reference/check IDs, and numeric codes) from transaction descriptions while
 * preserving merchant and semantic tokens.
 */
object DescriptionNormalizer {
  private val STATUS_BRACKETED_REGEX =
    Regex("""[\(\[]\s*(?:projected|pending|est(?:imated)?)\s*[\)\]]""", RegexOption.IGNORE_CASE)
  private val STATUS_WORD_REGEX =
    Regex("""\b(?:projected|pending)\b""", RegexOption.IGNORE_CASE)

  // Dates & timestamps
  private val ISO_DATE_REGEX =
    Regex("""\b\d{4}[-/]\d{1,2}[-/]\d{1,2}\b""")
  private val SLASH_DASH_DATE_REGEX =
    Regex("""\b\d{1,2}[-/]\d{1,2}[-/]\d{2,4}\b""")
  private val SHORT_DATE_REGEX =
    Regex("""\b(?:0?[1-9]|1[0-2])[/-](?:0?[1-9]|[12]\d|3[01])\b""")
  private val TIMESTAMP_REGEX =
    Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")

  // Month names, Tally month tokens (e.g. Oct2026), quarters, years
  private val MONTH_YEAR_TOKEN_REGEX =
    Regex(
      """\b(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)\d{2,4}\b""",
      RegexOption.IGNORE_CASE
    )
  private val MONTH_NAME_REGEX =
    Regex(
      """\b(?:january|february|march|april|may|june|july|august|september|october|november|december|""" +
        """jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\b""",
      RegexOption.IGNORE_CASE
    )
  private val QUARTER_REGEX =
    Regex("""\bq[1-4]\b""", RegexOption.IGNORE_CASE)
  private val YEAR_REGEX =
    Regex("""\b(?:19|20)\d{2}\b""")

  // Labeled IDs / confirmation / check / reference numbers
  private val LABELED_ID_WITH_SEP_REGEX =
    Regex(
      """\b(?:ref|conf|confirmation|txn|trans|transaction|id|inv|invoice|chk|check|order|auth|trace|seq)""" +
        """\s*[#:]\s*[a-z0-9\-]+\b""",
      RegexOption.IGNORE_CASE
    )
  private val LABELED_ID_WITH_DIGIT_REGEX =
    Regex(
      """\b(?:ref|conf|confirmation|txn|trans|transaction|id|inv|invoice|chk|check|order|auth|trace|seq)""" +
        """(?:\s*-\s*|\s+)[a-z0-9]*\d[a-z0-9\-]*\b""",
      RegexOption.IGNORE_CASE
    )

  // Hash/asterisk prefixed IDs containing at least one digit (preserves "PAYPAL *SPOTIFY")
  private val SYMBOL_ID_WITH_DIGIT_REGEX =
    Regex("""[#*]\s*[a-z0-9]*\d[a-z0-9\-]*""", RegexOption.IGNORE_CASE)

  // Phone numbers, standalone numbers, and mixed alphanumeric/hex codes (>= 5 chars with digits)
  private val NUMERIC_SEQUENCE_REGEX =
    Regex("""\b\d+(?:[-./]\d+)*\b""")
  private val ALPHANUMERIC_CODE_REGEX =
    Regex("""\b(?=[a-z0-9]*\d)(?=[a-z0-9]*[a-z])[a-z0-9]{5,}\b""", RegexOption.IGNORE_CASE)

  private val NON_ALNUM_REGEX = Regex("""[^a-z0-9]+""")
  private val WHITESPACE_REGEX = Regex("""\s+""")

  fun normalize(rawDescription: String?): NormalizedDescription {
    if (rawDescription.isNullOrBlank()) return NormalizedDescription.EMPTY

    var text = Normalizer.normalize(rawDescription, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    text = STATUS_BRACKETED_REGEX.replace(text, " ")
    text = STATUS_WORD_REGEX.replace(text, " ")
    text = ISO_DATE_REGEX.replace(text, " ")
    text = SLASH_DASH_DATE_REGEX.replace(text, " ")
    text = SHORT_DATE_REGEX.replace(text, " ")
    text = TIMESTAMP_REGEX.replace(text, " ")
    text = MONTH_YEAR_TOKEN_REGEX.replace(text, " ")
    text = MONTH_NAME_REGEX.replace(text, " ")
    text = QUARTER_REGEX.replace(text, " ")
    text = YEAR_REGEX.replace(text, " ")
    text = LABELED_ID_WITH_SEP_REGEX.replace(text, " ")
    text = LABELED_ID_WITH_DIGIT_REGEX.replace(text, " ")
    text = SYMBOL_ID_WITH_DIGIT_REGEX.replace(text, " ")
    text = NUMERIC_SEQUENCE_REGEX.replace(text, " ")
    text = ALPHANUMERIC_CODE_REGEX.replace(text, " ")
    text = NON_ALNUM_REGEX.replace(text, " ").trim()

    if (text.isEmpty()) return NormalizedDescription.EMPTY

    val tokens = text.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
    val normalizedText = tokens.joinToString(" ")
    return NormalizedDescription(
      normalizedText = normalizedText,
      tokens = tokens,
      tokenSet = tokens.toSet()
    )
  }

  /**
   * Removes trailing/bracketed status annotations such as `(projected)` from a raw description
   * so recurring groups get a clean human-readable label.
   */
  fun stripStatusSuffix(rawDescription: String?): String {
    if (rawDescription.isNullOrBlank()) return ""
    val cleaned = STATUS_BRACKETED_REGEX
      .replace(rawDescription, " ")
      .replace(WHITESPACE_REGEX, " ")
      .trim()
    return cleaned
  }
}
