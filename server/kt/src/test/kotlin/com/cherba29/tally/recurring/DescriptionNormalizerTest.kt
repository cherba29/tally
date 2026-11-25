package com.cherba29.tally.recurring

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class DescriptionNormalizerTest : DescribeSpec({
  describe("DescriptionNormalizer.normalize") {
    it("returns EMPTY for null, blank, or punctuation-only descriptions") {
      DescriptionNormalizer.normalize(null) shouldBe NormalizedDescription.EMPTY
      DescriptionNormalizer.normalize("") shouldBe NormalizedDescription.EMPTY
      DescriptionNormalizer.normalize("   --- ### 12345  ") shouldBe NormalizedDescription.EMPTY
    }

    it("strips status annotations and Tally month tokens") {
      DescriptionNormalizer.normalize("Pay off Nov2026 statement (projected)").normalizedText shouldBe
        "pay off statement"
      DescriptionNormalizer.normalize("Pay off Oct2026 statement").normalizedText shouldBe
        "pay off statement"
      DescriptionNormalizer.normalize("Acme Corp bi-monthly paycheck [pending]").normalizedText shouldBe
        "acme corp bi monthly paycheck"
    }

    it("strips ISO dates, slash dates, short dates, timestamps, years, and quarters") {
      DescriptionNormalizer.normalize("Payroll DD 10/15/2026 14:32:05").normalizedText shouldBe
        "payroll dd"
      DescriptionNormalizer.normalize("ACH DEBIT CON EDISON 2026-10-15 REF#991823").normalizedText shouldBe
        "ach debit con edison"
      DescriptionNormalizer.normalize("ACH DEBIT CON EDISON 2026-11-15 REF#442019").normalizedText shouldBe
        "ach debit con edison"
      DescriptionNormalizer.normalize("Paycheck + Q3 spot bonus 2026").normalizedText shouldBe
        "paycheck spot bonus"
    }

    it("strips reference, confirmation, transaction, and check IDs while keeping merchant text") {
      DescriptionNormalizer.normalize("ZELLE PAYMENT TO JANE DOE CONF# ABC123456").normalizedText shouldBe
        "zelle payment to jane doe"
      DescriptionNormalizer.normalize("ZELLE PAYMENT TO JANE DOE CONF# XYZ789012").normalizedText shouldBe
        "zelle payment to jane doe"
      DescriptionNormalizer.normalize("COMCAST / XFINITY 800-934-6489 TXN 8A3F9C").normalizedText shouldBe
        "comcast xfinity"
      DescriptionNormalizer.normalize("CHK #1042 RENT").normalizedText shouldBe
        "rent"
      DescriptionNormalizer.normalize("Order groceries").normalizedText shouldBe
        "order groceries"
    }

    it("preserves alphabetic merchant names after asterisk while stripping digit-bearing codes") {
      DescriptionNormalizer.normalize("PAYPAL *SPOTIFY 4029357733").normalizedText shouldBe
        "paypal spotify"
      DescriptionNormalizer.normalize("PAYPAL *SPOTIFY 4029359981").normalizedText shouldBe
        "paypal spotify"
      DescriptionNormalizer.normalize("AMZN MKTP US*2K9X184").normalizedText shouldBe
        "amzn mktp us"
      DescriptionNormalizer.normalize("Netflix.com #12345").normalizedText shouldBe
        "netflix com"
      DescriptionNormalizer.normalize("401k contribution").normalizedText shouldBe
        "401k contribution"
    }
  }

  describe("DescriptionNormalizer.stripStatusSuffix") {
    it("removes bracketed projected or pending annotations while preserving original casing") {
      DescriptionNormalizer.stripStatusSuffix("Acme Corp bi-monthly paycheck (projected)") shouldBe
        "Acme Corp bi-monthly paycheck"
      DescriptionNormalizer.stripStatusSuffix("Electric & gas utility [pending]") shouldBe
        "Electric & gas utility"
      DescriptionNormalizer.stripStatusSuffix("Electric & gas utility (summer AC)") shouldBe
        "Electric & gas utility (summer AC)"
      DescriptionNormalizer.stripStatusSuffix(null) shouldBe ""
    }
  }
})
