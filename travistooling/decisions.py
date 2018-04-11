# -*- encoding: utf-8


class Decision(Exception):
    """
    The base class for all decisions.
    """
    message = None


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
    message = 'Unrecognised file, so assuming significant'


class IgnoredPath(InsignificantFile):
    message = 'Path has no effect on build tasks'


class IgnoredFileFormat(InsignificantFile):
    message = 'File format has no effect on build tasks'


class ExclusivelyAffectsThisTask(SignificantFile):
    message = 'Path is an exclusive dependency of this build task'


class ExclusivelyAffectsAnotherTask(InsignificantFile):
    def __init__(self, other_task):
        self.message = (
            'Path is an exclusive dependency of a different build task (%s)' %
            (other_task,)
        )
        super(ExclusivelyAffectsAnotherTask, self).__init__()


class CheckedByTravisFormat(SignificantFile):
    message = 'File format is checked by the travis-format task'


class ScalaChangeAndIsScalaApp(SignificantFile):
    message = 'Changes to Scala common libs affect Scala apps'


class ScalaChangeAndNotScalaApp(InsignificantFile):
    message = 'Changes to Scala common libs are irrelevant to non-Scala apps'
