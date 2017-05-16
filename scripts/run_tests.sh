#!/usr/bin/env bash
# Run all the tests.

set -o nounset

ALL_PROJECTS="api common id_minter ingestor miro_adapter reindexer transformer"


function run_tests {
  # Run the commands for the test.  Chaining them together means we don't
  # have to pay the cost of starting the JVM three times.
  # http://www.scala-sbt.org/0.12.2/docs/Howto/runningcommands.html
  sbt "project $1" ";dockerComposeUp;test;dockerComposeStop"
}


PROJECT=${PROJECT:-""}
if [[ -z "$PROJECT" ]]
then
  echo "=== No project specified; running tests for all projects ==="
  for project in $ALL_PROJECTS
  do
    echo "=== Starting tests for $project ==="
    run_tests "$project"
    echo "=== Finished tests for $project ==="
  done
else
  run_tests "$PROJECT"
fi
