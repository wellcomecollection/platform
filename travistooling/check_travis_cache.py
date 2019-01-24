#!/usr/bin/env python
# -*- encoding: utf-8
"""
A standalone script that inspects the cached directories in .travis.yml,
and compares them to the sbt directories, to see if:

*   We're caching something we shouldn't
*   We aren't caching something we should

"""

import os
import sys
import yaml

from sbt_dependency_tree import Repository


repo = Repository("builds/sbt_metadata")

with open(".travis.yml") as stream:
    travis_data = yaml.safe_load(stream)

directories = set(travis_data["cache"]["directories"])

directories -= {"$HOME/.sbt", "$HOME/.ivy2/cache", "project/target", "target"}

not_cached = set()
for project in repo.projects.values():
    target_dir = os.path.join(project.folder, "target")
    if target_dir in directories:
        directories.remove(target_dir)
    else:
        not_cached.add(target_dir)

if directories or not_cached:
    if directories:
        print("Don't cache these directories:")
        for d in sorted(directories):
            print(" - %r" % d)
        print("")

    if not_cached:
        print("Do cache these directories:")
        for d in sorted(not_cached):
            print(" - %r" % d)
        print("")

    sys.exit(1)
