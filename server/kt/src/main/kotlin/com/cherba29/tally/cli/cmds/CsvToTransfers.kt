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
    val dates = data["Date"]?.map { LocalDate.parse(it, dateFormat) }
      ?: throw CliktError("Cant find 'Date' field")
    val month = dates.max().run { Month(year, month.number - 1) }

    echo("Detected source: ${getCsvSource(data.keys)}")
    echo("Detected month: $month")

    val descriptions = data["Description"]
      ?: throw IllegalArgumentException("Cant find 'Description' field")
    require(descriptions.size == dates.size)
    val debits = data["Debit"]
      ?: throw IllegalArgumentException("Cant find 'Debit' field")
    require(debits.size == dates.size)

    val credits = data["Credit"]
      ?: throw IllegalArgumentException("Cant find 'Credit' field")
    require(credits.size == dates.size)

    for ((i, date) in dates.withIndex()) {
      val description = descriptions[i]
      val amount = debits[i].toFloatOrNull() ?: (credits[i].toFloatOrNull()) ?:
        throw IllegalArgumentException("'Credit' or 'Debit' field is not set on row $i for date $date")

      if (amount < 0 && description.contains("payment", ignoreCase = true)) {
        continue
      }
      echo("    - { grp: $month, date: $date, camt: ${"%8.2f".format(amount)}, desc: \"$description\" }")
    }
  }

  companion object {
    private val creditCardFields = mapOf(
      "costco" to setOf("Status", "Date", "Description", "Debit", "Credit", "Member Name")
    )
    fun getCsvSource(header: Set<String>): String? {
      for ((source, fields) in creditCardFields) {
        if (fields == header) return source
      }
      return null
    }

    val dateFormat = LocalDate.Format { byUnicodePattern("MM/dd/yyyy") }
   }

}
