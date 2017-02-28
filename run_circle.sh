#!/usr/bin/env bash
# This script runs our build process for Circle.  This is made somewhat
# complicated because we have a multi-project setup.
#
# The script is invoked with a single command-line argument: one of
# 'compile', 'test', or 'deploy', which runs the corresponding task for
# each of the projects in the repo.


# List of projects that sbt knows how to build
PROJECTS="common api"


# Some bash debugging options: tracing, and exit as soon as a build step fails
set -x
set -o errexit
set -o nounset



# Read command-line arguments
if (( $# == 1 )); then
    TASK="$1"
else
    echo "Usage: run_circle.sh <TASK>"
    exit 1
fi


if [[ "$TASK" == "compile" || "$TASK" == "test" ]]; then
    for project in $PROJECTS; do
        sbt "project $project" "$TASK"
    done
elif [[ "$TASK" == "deploy" ]]; then
    for project in $PROJECTS; do
        # There isn't a deploy step for the common lib
        if [[ "$project" == "common" ]]; then continue; fi
        sbt "project $project" -DconfigBucket=$CONFIG_BUCKET -Denv=$BUILD_ENV ecr:push
    done
else
    echo "Unrecognised task '$TASK'"
    exit 1
fi
