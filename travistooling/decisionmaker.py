# -*- encoding: utf-8
"""
An important part of our Travis setup is that we skip build jobs where
possible, as this means we can merge pull requests more quickly.
A full build can take nearly two hours across 25 tasks, and that keeps
going up.  This file contains the logic for answering the question:

    Does file X affect build job Y?

"""

import os

from travistooling.decisions import (
    IgnoredFileFormat,
    IgnoredPath,
    KnownAffectsThisJob,
    KnownDoesNotAffectThisJob,
    UnrecognisedFile
)
from travistooling.git import ROOT
from travistooling.parse_makefiles import get_projects


# Cache the Makefile information in a global variable, so we only have to
# load it once.
PROJECTS = list(get_projects(ROOT))


def does_file_affect_build_job(path, job_name):
    # Catch all the common file types that we care about for travis-format.
    if (
        job_name == 'travis-format' and
        path.endswith(('.scala', '.tf', '.py', '.json', '.ttl'))
    ):
        raise KnownAffectsThisJob(path)

    # These extensions and paths never have an effect on tests.
    if path.endswith(('.md', '.png', '.graffle', '.tf')):
        raise IgnoredFileFormat(path)

    # These paths never have an effect on tests.
    if path in ['LICENSE', ] or path.startswith(('misc/', 'ontologies/')):
        raise IgnoredPath(path)

    # Some directories only affect one task.
    #
    # For example, the ``catalogue_api/api`` directory only contains code
    # for the api Scala app, so changes in this directory cannot affect
    # any other task.
    exclusive_directories = {
        os.path.relpath(t.exclusive_path, start=ROOT): t.name for t in PROJECTS
    }

    for dir_name, job_name_prefix in exclusive_directories.items():
        if path.startswith(dir_name):
            if job_name.startswith(job_name_prefix):
                raise KnownAffectsThisJob(path)
            else:
                raise KnownDoesNotAffectThisJob(path)

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
            if job_name.startswith(project.name):
                if project.type == 'sbt_app':
                    raise KnownAffectsThisJob(path)
                else:
                    raise KnownDoesNotAffectThisJob(path)

    # If we can't decide if a file affects a build job, we assume it's
    # significant and run the job just-in-case.
    raise UnrecognisedFile(path)
