package com.cherba29.tally.cli.cmds

import com.cherba29.tally.core.Month
import com.jsoizo.kotlincsv.csvReader
import com.jsoizo.kotlincsv.reader.readFromFile
import com.jsoizo.kotlincsv.reader.withHeader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.number
import java.nio.file.Path

class CsvToTransfers : CliktCommand("csv-to-transfers") {
  override fun help(context: Context) = "Convert give csv file to list of transfers"

  val csvPath: Path by option("--csv-file", help = "Path to the csv file with transactions")
    .path(mustExist = true, canBeDir = false, mustBeReadable = true).required()

  override fun run() {
    echo("Converting csv '$csvPath' to transfers")
    val data = csvReader {
      skipEmptyLine = true
    }.readFromFile(csvPath.toString()) { rows ->
      val records = rows.withHeader()
      val data = mutableMapOf<String, MutableList<String>>()
      for (record in records) {
        for ((header, value) in record) {
          data.getOrPut(header) { mutableListOf() }.add(value)
        }
        echo(record)
      }
      data
    }
    val csvMetadata = getCsvSource(data.keys)
      ?: throw CliktError("Unknown type of csv with fields ${data.keys}")

    val dates = data[csvMetadata.dateField]?.map { LocalDate.parse(it, dateFormat) }
      ?: throw CliktError("Cant find Date field '${csvMetadata.dateField}'")
    val month = dates.max().run { Month(year, month.number - 1) }

    echo("Detected source: ${csvMetadata.name}")
    echo("Detected month: $month")

    val descriptions = data[csvMetadata.descriptionField]
      ?: throw IllegalArgumentException("Cant find description field ${csvMetadata.descriptionField}")
    require(descriptions.size == dates.size)
    val debits = data[csvMetadata.debitField]
      ?: throw IllegalArgumentException("Cant find 'Debit' field")
    require(debits.size == dates.size)

    val credits = data[csvMetadata.creditField]
      ?: throw IllegalArgumentException("Cant find 'Credit' field")
    require(credits.size == dates.size)

    val amountFactor = if (csvMetadata.negated) -1 else 1
    val amountFormat = csvMetadata.amountFormat

    for ((i, date) in dates.withIndex()) {
      val description = descriptions[i]
      val amount = amountFactor * (debits[i].toFloatOrNull() ?: (credits[i].toFloatOrNull()) ?:
        throw IllegalArgumentException("'Credit' or 'Debit' field is not set on row $i for date $date"))

      if (amount < 0 && description.contains("payment", ignoreCase = true)) {
        continue
      }
      echo("    - { grp: $month, date: $date, camt: ${amountFormat.format(amount)}, desc: \"$description\" }")
    }
  }

  companion object {
    data class CsvMetadata(
      val name: String,
      val fields: Set<String>,
      val dateField: String,
      val descriptionField: String,
      val debitField: String,
      val creditField: String,
      val negated: Boolean,
      val amountFormat: String,
    )
    private val creditCardFields = listOf(
      CsvMetadata(
        name = "costco",
        setOf("Status", "Date", "Description", "Debit", "Credit", "Member Name"),
        dateField = "Date",
        descriptionField = "Description",
        debitField = "Debit",
        creditField = "Credit",
        negated = false,
        amountFormat = "%7.2f",
      ),
      CsvMetadata(
        name = "citi-double",
        setOf("Status", "Date", "Description", "Debit", "Credit"),
        dateField = "Date",
        descriptionField = "Description",
        debitField = "Debit",
        creditField = "Credit",
        negated = false,
        amountFormat = "%7.2f",
      ),
      CsvMetadata(
        name = "chase_amazon",
          setOf("Transaction Date","Post Date","Description","Category","Type","Amount","Memo"),
        dateField = "Transaction Date",
        descriptionField = "Description",
        debitField = "Amount",
        creditField = "Amount",
        negated = true,
        amountFormat = "%7.2f",
      )
    )
    fun getCsvSource(header: Set<String>): CsvMetadata? {
      for (metadata in creditCardFields) {
        if (metadata.fields == header) return metadata
      }
      return null
    }

    val dateFormat = LocalDate.Format { byUnicodePattern("MM/dd/yyyy") }
   }

}
