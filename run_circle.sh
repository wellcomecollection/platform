#!/usr/bin/env bash
# This script runs our build process for Circle.  This is made somewhat
# complicated because we have a multi-project setup.
#
# The script is invoked with a single command-line argument: one of
# 'compile', 'test', or 'deploy', which runs the corresponding task for
# each of the projects in the repo.


# List of projects that sbt knows how to build
PROJECTS="common api transformer calm_adapter ingestor"


# Some bash debugging options: tracing, and exit as soon as a build step fails
set -x
set -o errexit
set -o nounset


# Read command-line arguments
if (( $# == 1 ))
then
    TASK="$1"
else
    echo "Usage: run_circle.sh <TASK>"
    exit 1
fi


# Check if we want to build this project, based on the files that have
# changed between this branch and the current master

# Get the name of the currently checked-out branch
current_branch=$(git rev-parse --abbrev-ref HEAD)

if [[ $current_branch != "master" ]]
then
    git fetch origin

    # Get a list of files that changed between master and the current branch
    # TODO: Err, does this handle added files?
    changed_files=$(git diff --name-only master "$current_branch" --)
fi


for project in $PROJECTS
do
    # Always build everything on master
    if [[ $current_branch == "master" ]]
    then
        run_task=true
    else
        # Build everything if common-lib has changed, otherwise only build
        # projects with changed files.
        run_task=false
        for file in $changed_files
        do
            # TODO: What's the name for the common-lib directory?
            if [[ "$file" == "common-lib/"* ]] || [[ "$file" == "$project"/* ]]
            then
                run_task=true
                break
            fi
        done
    fi

    if [[ "$run_task" != "true" ]]
    then
        continue
    fi

    # At this point we know we want to run this project: run the task!
    if [[ "$TASK" == "compile" || "$TASK" == "test" ]]
    then
        sbt "project $project" "$TASK"
    elif [[ "$TASK" == "deploy" ]]
    then
        # There isn't a deploy step for the common lib
        if [[ "$project" == "common" ]]
        then
            continue
        fi
        sbt "project $project" -DconfigBucket=$CONFIG_BUCKET -Denv=$BUILD_ENV ecr:push
    else
        echo "Unrecognised task '$TASK'"
        exit 1
    fi

done
