package com.cherba29.tally.cli.cmds

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.transformAll
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Files
import java.nio.file.Paths

const val TALLY_FILES_ENV_VAR = "TALLY_FILES"
const val LEGACY_TALLY_PATH_ENV_VAR = "TALLY_PATH"

/**
 * Shared `--tally-path` option for CLI commands.
 *
 * Precedence:
 * 1. Explicit `--tally-path` CLI option
 * 2. `TALLY_FILES` environment variable (unified across server and CLI)
 * 3. `TALLY_PATH` environment variable (legacy fallback)
 */
fun CliktCommand.tallyPathOption() =
  option(
    "--tally-path",
    envvar = TALLY_FILES_ENV_VAR,
    help = "Path to YAML budget data directory (env: $TALLY_FILES_ENV_VAR, fallback: $LEGACY_TALLY_PATH_ENV_VAR)"
  ).path(mustExist = true).transformAll(showAsRequired = true) { values ->
    values.lastOrNull()
      ?: context.readEnvvar(LEGACY_TALLY_PATH_ENV_VAR)?.takeIf { it.isNotBlank() }?.let { raw ->
        val legacyPath = Paths.get(raw)
        if (!Files.exists(legacyPath)) {
          fail("Path \"$raw\" does not exist.")
        }
        legacyPath
      }
      ?: throw MissingOption(option)
  }
