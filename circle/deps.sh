#!/bin/bash

set -x
set -o errexit

for project in common api
do
    sbt "project $project" test:compile
done