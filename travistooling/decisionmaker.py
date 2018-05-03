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
    AppDoesNotUseThisScalaCommonLib,
    AppUsesThisScalaCommonLib,
    ChangesToTestsDontGetPublished,
    CheckedByTravisFormat,
    ExclusivelyAffectsAnotherTask,
    ExclusivelyAffectsThisTask,
    IgnoredFileFormat,
    IgnoredPath,
    InsignificantFile,
    ScalaChangeAndIsScalaApp,
    ScalaChangeAndNotScalaApp,
    SignificantFile,
    UnrecognisedFile
)
from travistooling.git_utils import ROOT
from travistooling.parse_makefiles import get_projects
from travistooling.sbt_dependency_checker import does_path_depend_on_library


# Cache the Makefile information in a global variable, so we only have to
# load it once.
PROJECTS = list(get_projects(ROOT))


def does_file_affect_build_task(path, task):
    # Catch all the common file types that we care about for travis-format.
    if (
        task == 'travis-format' and
        path.endswith(('.scala', '.tf', '.py', '.json', '.ttl'))
    ):
        raise CheckedByTravisFormat()

    # These extensions and paths never have an effect on tests.
    if path.endswith(('.md', '.png', '.graffle', '.tf', 'Makefile')):
        raise IgnoredFileFormat()

    # These paths never have an effect on tests.
    if path in [
        'LICENSE',
        '.travis.yml',
        'run_travis_task.py',
    ] or path.startswith(('misc/', 'ontologies/', 'data_science/scripts/')):
        raise IgnoredPath()

    # Some directories only affect one task.
    #
    # For example, the ``catalogue_api/api`` directory only contains code
    # for the api Scala app, so changes in this directory cannot affect
    # any other task.
    exclusive_directories = {
        os.path.relpath(p.exclusive_path, start=ROOT): p.name
        for p in PROJECTS
        if p.exclusive_path is not None
    }

    for dir_name, task_prefix in exclusive_directories.items():
        if path.startswith(dir_name):
            if task.startswith(task_prefix):
                raise ExclusivelyAffectsThisTask()
            else:
                raise ExclusivelyAffectsAnotherTask(task_prefix)

    # If this is a test file and we're in a publish task, we can skip
    # running the task.
    if task.endswith('-publish') and (

        # Scala test files
        'src/test/scala/uk/ac/wellcome' in path or

        # Python test files
        path.endswith(('conftest.py', '.coveragerc'))
    ):
        raise ChangesToTestsDontGetPublished()

    # Now we need to start looking at projects on an individual basis.
    project = None
    for p in PROJECTS:
        if task.startswith(p.name):
            project = p
            break

    else:  # pragma: no cover
        if task == 'travis-format':
            project = Project(
                name='travis-format',
                type='python_lib',
                exclusive_path=None
            )
        else:
            raise RuntimeError("Unrecognised task: %s" % task)

    # Changes to the sbt_common libraries should only be tested if the
    # application in question uses this library.
    if path.startswith('sbt_common/') and project.type == 'sbt_app':

        # This directory has libraries of the form
        #
        #   sbt_common/display/...
        #   sbt_common/elasticsearch/...
        #
        library_name = path.split('/')[1]

        # This tells us where the exclusive code for this app exists.
        project_dir = os.path.relpath(project.exclusive_path, start=ROOT)

        if does_path_depend_on_library(
            path=project_dir,
            library_name=library_name
        ):
            raise AppUsesThisScalaCommonLib(library_name)
        else:
            raise AppDoesNotUseThisScalaCommonLib(library_name)

    # We have a couple of sbt common libs and files scattered around the
    # repository; changes to any of these don't affect non-sbt applications.
    if path.startswith((
        'sierra_adapter/common',
        'common/',
        'project/',
        'build.sbt',
        'sbt_common/',
    )):
        if project.type == 'sbt_app':
            raise ScalaChangeAndIsScalaApp()
        else:
            raise ScalaChangeAndNotScalaApp()

    # Changes made in the travistooling directory only ever affect the
    # travistooling tests (but they're not defined in a Makefile).
    # Check and skip here if possible.
    if path.startswith('travistooling/'):
        if task == 'travistooling-test':
            raise ExclusivelyAffectsThisTask()
        else:
            raise ExclusivelyAffectsAnotherTask('travistooling-test')

    # This script is only used in the travis-format task.  It's already
    # picked up as significant by travis-format because it's a *.py file.
    if path == 'run_autoformat.py' and task != 'travis-format':
        raise ExclusivelyAffectsAnotherTask('travis-format')

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
    report = {
        True: collections.defaultdict(set),
        False: collections.defaultdict(set),
    }
    for path in sorted(changed_paths):
        try:
            does_file_affect_build_task(path=path, task=task)
        except InsignificantFile as err:
            report[False][err.message].add(path)
        except SignificantFile as err:
            report[True][err.message].add(path)

    return (
        bool(report[True]),
        {True: dict(report[True]), False: dict(report[False])}
    )
