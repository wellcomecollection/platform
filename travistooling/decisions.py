# -*- encoding: utf-8


class Decision(Exception):
    """
    The base class for all decisions.
    """


class SignificantFile(Decision):
    """
    This file might an effect on the outcome of the current build task.
    """
    is_significant = True


class InsignificantFile(Decision):
    """
    This file does not have an effect on the outcome of the current build task.
    """
    is_significant = False


class UnrecognisedFile(SignificantFile):
    """
    We cannot determine if this file has an effect on the current build task.
    """


class IgnoredPath(InsignificantFile):
    """
    This path never has an effect on build tasks.
    """


class IgnoredFileFormat(IgnoredPath):
    """
    This file format never has an effect on build tasks.
    """


class KnownAffectsThisTask(SignificantFile):
    """
    This file has a known effect on this build task.
    """


class KnownDoesNotAffectThisTask(InsignificantFile):
    """
    This file is known not to affect this build task.
    """
