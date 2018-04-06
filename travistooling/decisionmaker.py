# -*- encoding: utf-8
"""
An important part of our Travis setup is that we skip build jobs where
possible, as this means we can merge pull requests more quickly.
A full build can take nearly two hours across 25 tasks, and that keeps
going up.  This file contains the logic for answering the question:

    Does file X affect build job Y?

"""

from travistooling.decisions import UnrecognisedFile


def does_file_affect_build_job(path, job_name):
    # If we can't decide if a file affects a build job, we assume it's
    # significant and run the job just-in-case.
    raise UnrecognisedFile(path=path)
