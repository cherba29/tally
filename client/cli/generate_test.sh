#!/bin/bash
set -eu

# Find the path to the greeter binary using environment variables
# Bazel sets TEST_SRCDIR and TEST_WORKSPACE to help locate data dependencies at runtime.
# This approach is robust and avoids hardcoding paths.
TALLY_CLI_BIN="${TEST_SRCDIR}/${TEST_WORKSPACE}/client/cli/tally_cli.sh"

# Ensure the binary is found and executable
if [ ! -f "$TALLY_CLI_BIN" ]; then
    echo "Error: tally_cli binary not found at $TALLY_CLI_BIN"
    exit 1
fi

# Run the binary and capture its output
OUTPUT=$(BUILD_WORKSPACE_DIRECTORY=${TEST_SRCDIR}/${TEST_WORKSPACE}/client/cli "$TALLY_CLI_BIN" generate External --start-month Dec2022)
EXPECTED="Generating balances for External starting from Dec2022"

if echo "$OUTPUT" | grep -q "$EXPECTED"; then
    echo "Test Passed: Output is correct."
    exit 0
else
    echo "Test Failed: Expected '$EXPECTED' was not found in '$OUTPUT'"
    exit 1
fi
