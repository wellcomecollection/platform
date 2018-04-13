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


# Cache the Makefile information in a global variable, so we only have to
# load it once.
PROJECTS = list(get_projects(ROOT))


def does_file_affect_build_job(path, task_name):
    # Catch all the common file types that we care about for travis-format.
    if (
        task_name == 'travis-format' and
        path.endswith(('.scala', '.tf', '.py', '.json', '.ttl'))
    ):
        raise CheckedByTravisFormat()

    # These extensions and paths never have an effect on tests.
    if path.endswith(('.md', '.png', '.graffle', '.tf')):
        raise IgnoredFileFormat()

    # These paths never have an effect on tests.
    if path in ['LICENSE', ] or path.startswith(('misc/', 'ontologies/')):
        raise IgnoredPath()

    # Some directories only affect one task.
    #
    # For example, the ``catalogue_api/api`` directory only contains code
    # for the api Scala app, so changes in this directory cannot affect
    # any other task.
    exclusive_directories = {
        os.path.relpath(t.exclusive_path, start=ROOT): t.name for t in PROJECTS
    }

    for dir_name, task_name_prefix in exclusive_directories.items():
        if path.startswith(dir_name):
            if task_name.startswith(task_name_prefix):
                raise ExclusivelyAffectsThisTask()
            else:
                raise ExclusivelyAffectsAnotherTask(task_name_prefix)

    # If this is a Scala test file and we're in a publish task, we can
    # skip running the task.
    if (
        'src/test/scala/uk/ac/wellcome' in path and
        task_name.endswith('-publish')
    ):
        raise ChangesToTestsDontGetPublished()

    # We have a couple of sbt common libs and files scattered around the
    # repository; changes to any of these don't affect non-sbt applications.
    if path.startswith((
        'sierra_adapter/common',
        'common/',
        'project/',
        'build.sbt',
        'sbt_common/'
    )):
        for project in PROJECTS:
            if task_name.startswith(project.name):
                if project.type == 'sbt_app':
                    raise ScalaChangeAndIsScalaApp()
                else:
                    raise ScalaChangeAndNotScalaApp()

    # If we can't decide if a file affects a build job, we assume it's
    # significant and run the job just-in-case.
    raise UnrecognisedFile()


def should_run_job(changed_paths, task_name):
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
            does_file_affect_build_job(path=path, task_name=task_name)
        except InsignificantFile as err:
            report[False][err.message].add(path)
        except SignificantFile as err:
            report[True][err.message].add(path)

    return (
        bool(report[True]),
        {True: dict(report[True]), False: dict(report[False])}
    )
