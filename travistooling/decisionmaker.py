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
    ChangeToUnusedLibrary,
    CheckedByTravisFormat,
    CheckedByTravisLambda,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    InsignificantFile,
    PythonChangeAndIsScalaApp,
    ScalaChangeAndIsScalaApp,
    ScalaChangeAndNotScalaApp,
    SignificantFile,
    UnrecognisedFile,
)
from travistooling.git_utils import ROOT
from travistooling.parse_makefiles import get_projects


# Cache the Makefile information in a global variable, so we only have to
# load it once.
PROJECTS = list(get_projects(ROOT))


def does_file_affect_build_task(path, task):
    # Catch all the common file types that we care about for travis-format.
    if task == "travis-format" and path.endswith(
        (".scala", ".tf", ".py", ".json", ".ttl")
    ):
        raise CheckedByTravisFormat()

    # And a quick catch-all of file types that might signify a change for
    # travis-lambda-{test, publish}
    if task.startswith("travis-lambda-") and path.endswith(("requirements.txt", ".py")):
        raise CheckedByTravisLambda()

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
    ] or path.startswith(("misc/", "ontologies/", "data_science/scripts/")):
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

    # Some directories only affect one task.
    #
    # For example, the ``catalogue_api/api`` directory only contains code
    # for the api Scala app, so changes in this directory cannot affect
    # any other task.
    exclusive_directories = {proj.exclusive_path: proj.name for proj in PROJECTS}

    for dir_name, task_prefix in exclusive_directories.items():
        if path.startswith(dir_name):
            if task.startswith(task_prefix):
                raise ExclusivelyAffectsThisTask()
            else:
                raise ExclusivelyAffectsAnotherTask(task_prefix)

    # We have a library containing display models in sbt_common/display.
    #
    # Not every application uses these display models -- in particular,
    # all our pipeline applications.  So a change to the display models
    # can be safely ignored here.  Specifically, applications in the
    # following stacks:
    #
    #   - catalogue_pipeline
    #   - reindexer
    #   - goobi_adapter
    #   - sierra_adapter
    #
    if path.startswith("sbt_common/display"):
        for project in PROJECTS:
            if task.startswith(project.name) and (project.type == "sbt_app"):
                if project.exclusive_path.startswith(
                    (
                        "catalogue_pipeline/",
                        "reindexer/",
                        "goobi_adapter/",
                        "sierra_adapter/",
                    )
                ):
                    raise ChangeToUnusedLibrary("display")

    # We have a library for elasticsearch code.
    #
    # Not every application uses these display models -- in particular,
    # quite a bit of the pipeline, and some of the adapters.  So a change
    # to the elasticsearch code can safely be ignored.
    #
    if path.startswith(
        ("sbt_common/elasticsearch", "sbt_common/finatra_elasticsearch")
    ):
        for project in PROJECTS:
            if task.startswith(project.name) and (project.type == "sbt_app"):
                if project.exclusive_path.startswith(
                    (
                        "catalogue_pipeline/id_minter",
                        "catalogue_pipeline/matcher",
                        "catalogue_pipeline/merger",
                        "catalogue_pipeline/recorder",
                        "catalogue_pipeline/relater",
                        "reindexer/",
                        "goobi_adapter/",
                        "sierra_adapter/",
                    )
                ):
                    raise ChangeToUnusedLibrary("elasticsearch")

    # We have a library for messaging code.
    #
    # The catalogue API doesn't use this code, because it's not SQS-driven.
    #
    if path.startswith(("sbt_common/messaging", "sbt_common/finatra_messaging")):
        for project in PROJECTS:
            if task.startswith(project.name) and (project.type == "sbt_app"):
                if project.exclusive_path.startswith(("catalogue_api/",)):
                    raise ChangeToUnusedLibrary("messaging")

    # We have a library for common archive code.
    #
    # Only apps in the archive stack use this code.
    if path.startswith("archive/common"):
        if task.startswith(project.name) and (project.type == "sbt_app"):
            if not project.exclusive_path.startswith("archive/"):
                raise ChangeToUnusedLibrary("archive_common")

    # We have a couple of sbt common libs and files scattered around the
    # repository; changes to any of these don't affect non-sbt applications.
    if path.startswith(
        ("sierra_adapter/common", "project/", "build.sbt", "sbt_common/")
    ):
        if (task in "travistooling-test") or task.startswith("travis-lambda"):
            raise ScalaChangeAndNotScalaApp()

        for project in PROJECTS:
            if task.startswith(project.name):
                if project.type == "sbt_app":
                    raise ScalaChangeAndIsScalaApp()
                else:
                    raise ScalaChangeAndNotScalaApp()

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
    if path.endswith(".py"):
        for project in PROJECTS:
            if task.startswith(project.name) and (project.type == "sbt_app"):
                raise PythonChangeAndIsScalaApp()

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
