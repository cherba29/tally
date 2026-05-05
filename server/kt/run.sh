#!/usr/bin/env bash

TALLY_FILES=../../data TALLY_CLIENT_BUNDLE=../../bazel-bin/client/hbs ./gradlew run -t --info
