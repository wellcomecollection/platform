# -*- encoding: utf-8


class Decision(Exception):
    """
    The base class for all decisions.
    """
    path = None

    def __init__(self, path):
        self.path == path
        super(Decision, self).__init__()


class SignificantFile(Decision):
    """
    This file might an effect on the outcome of the current build job.
    """
    is_significant = True


class InsignificantFile(Decision):
    """
    This file does not have an effect on the outcome of the current build job.
    """
    is_significant = False


class UnrecognisedFile(SignificantFile):
    """
    We cannot determine if this file has an effect on the current build job.
    """


class IgnoredPath(InsignificantFile):
    """
    This path never has an effect on build jobs.
    """


class IgnoredFileFormat(IgnoredPath):
    """
    This file format never has an effect on build jobs.
    """


class KnownAffectsThisJob(SignificantFile):
    """
    This file has a known effect on this build job.
    """


class KnownDoesNotAffectThisJob(InsignificantFile):
    """
    This file is known not to affect this build job.
    """
