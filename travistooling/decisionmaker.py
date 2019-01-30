# -*- encoding: utf-8
"""
An important part of our Travis setup is that we skip build jobs where
possible, as this means we can merge pull requests more quickly.
A full build can take nearly two hours across 25 tasks, and that keeps
going up.  This file contains the logic for answering the question:

    Does file X affect build job Y?

"""

import collections
import os

from travistooling.decisions import (
    ChangesToTestsDontGetPublished,
    CheckedByTravisFormat,
    CheckedByTravisLambda,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    InsignificantFile,
    PythonChangeAndIsScalaApp,
    SignificantFile,
    UnrecognisedFile,
)
from travistooling.git_utils import ROOT
from travistooling.sbt_dependency_tree import Repository


# Cache the Makefile information in a global variable, so we only have to
# load it once.
SBT_REPO = Repository(metadata_dir=os.path.join(ROOT, "builds", "sbt_metadata"))


def does_file_affect_build_task(path, task):
    # Catch all the common file types that we care about for travis-format.
    if task == "travis-format" and path.endswith(
        (".scala", ".tf", ".py", ".json", ".ttl")
    ):
        raise CheckedByTravisFormat()

    # These extensions and paths never have an effect on tests.
    if path.endswith(
        (".in", ".ipynb", ".md", ".png", ".graffle", ".tf", "Makefile")
    ) or os.path.basename(path).startswith(".git"):
        raise IgnoredFileFormat()

    # These paths never have an effect on tests.
    if path in [
        "LICENSE",
        ".travis.yml",
        "run_travis_task.py",
        "run_travis_lambdas.py",
    ] or path.startswith(
        ("misc/", "ontologies/", "data_science/scripts/", "builds/sbt_metadata/")
    ):
        raise IgnoredPath()

    # If this is a test file and we're in a publish task, we can skip
    # running the task.
    if task.endswith("-publish") and (
        # Scala test files
        "src/test/scala/uk/ac/wellcome" in path
        # Python test files
        or path.endswith(("conftest.py", ".coveragerc"))
    ):
        raise ChangesToTestsDontGetPublished()

    # And a quick catch-all of file types that might signify a change for
    # travis-lambda-{test, publish}
    if task.startswith("travis-lambda-") and path.endswith(("requirements.txt", ".py")):
        raise CheckedByTravisLambda()

    # Okay, so now we need to see if this is a Scala task, and if so, whether the
    # path is one that affects this task.
    project_name = task.split("-")[0]
    if project_name in SBT_REPO.projects:

        if path.endswith(".py"):
            raise PythonChangeAndIsScalaApp()

        if path.endswith(".sbt"):
            raise SignificantFile("build.sbt affects all Scala apps")
        if path.startswith("project/"):
            raise SignificantFile("Changes in project/ affect all Scala apps")

        if path.endswith(".scala"):
            project = SBT_REPO.get_project(project_name)
            for f in project.all_folders():
                if path.startswith(f):
                    raise SignificantFile("%s depends on %s" % (project_name, f))
            else:
                raise InsignificantFile()

    # Changes made in the travistooling directory only ever affect the
    # travistooling tests (but they're not defined in a Makefile).
    # Check and skip here if possible.
    if path.startswith("travistooling/"):
        if task == "travistooling-test":
            raise ExclusivelyAffectsThisTask()
        else:
            raise ExclusivelyAffectsAnotherTask("travistooling-test")

    # This script is only used in the travis-format task.  It's already
    # picked up as significant by travis-format because it's a *.py file.
    if path == "run_autoformat.py" and task != "travis-format":
        raise ExclusivelyAffectsAnotherTask("travis-format")

    # The bagger code is a bit of an oddity; for now I've hard-coded an
    # exception but we should really handle it properly.
    if path.startswith("storage/bagger"):
        if task == "bagger-publish":
            raise SignificantFile()
        else:
            raise ExclusivelyAffectsAnotherTask("bagger-publish")

    # We have a number of scripts that live in the top of a stack, which
    # contain useful code for that stack, but which aren't part of another
    # task.  They should be checked by travis-format, but that's it.
    if (
        path.endswith(".py")
        and path.count(os.path.sep) == 1
        and task != "travis-format"
    ):
        raise ExclusivelyAffectsAnotherTask("travis-format")

    # Changes to Python files only affect Scala apps.

    # If we can't decide if a file affects a build job, we assume it's
    # significant and run the job just-in-case.
    raise UnrecognisedFile()


def should_run_build_task(changed_paths, task):
    """
    Should we run this build job?  Returns a tuple (result, report).
    """
    # True/False is whether this path is significant to the current test job.
    # Within each map, we're recording the type of the exception and the
    # files associated with it.
    report = {True: collections.defaultdict(set), False: collections.defaultdict(set)}
    for path in sorted(changed_paths):
        try:
            does_file_affect_build_task(path=path, task=task)
        except InsignificantFile as err:
            report[False][err.message].add(path)
        except SignificantFile as err:
            report[True][err.message].add(path)

    return (bool(report[True]), {True: dict(report[True]), False: dict(report[False])})
