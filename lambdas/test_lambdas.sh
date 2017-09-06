#!/usr/bin/env bash

# Run lambda tests
find ./*/target -maxdepth 1 -name "test_*.py" | xargs py.test

# Run common code tests
find ./common -maxdepth 2 -name "test_*.py"