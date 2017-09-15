#!/usr/bin/env bash

set -o errexit
set -o nounset

lambda_dir="src"
target_dir="target"

if [ -d $lambda_dir ]
then
  echo "Building ./target directory for lambda."


  rm -rf $target_dir
  cp -r $lambda_dir $target_dir

  if [ -e src/requirements.txt ]
  then
    echo "Found requirements.txt, installing."
    pip install -r src/requirements.txt --target=$target_dir
  else
    echo "No requirements.txt present. Skipping."
  fi
else
  echo "No ./src directory found! Exiting."
  exit 1
fi
