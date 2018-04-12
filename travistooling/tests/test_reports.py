# -*- encoding: utf-8

from travistooling.decisions import (
    ExclusivelyAffectsThisTask,
    IgnoredPath,
    ScalaChangeAndNotScalaApp,
)
from travistooling.reports import build_report_output


def test_build_complete_report():
    report = {
        True: {
            ExclusivelyAffectsThisTask.message: set(['foo/bar.txt', 'foo/baz.txt']),
        },
        False: {
            IgnoredPath.message: set(['README.md', 'LICENSE']),
            ScalaChangeAndNotScalaApp.message: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons to run tests ##

Path is an exclusive dependency of this build task:
 - foo/bar.txt
 - foo/baz.txt


## Reasons not to run tests ##

Path has no effect on build tasks:
 - LICENSE
 - README.md

Changes to Scala common libs are irrelevant to non-Scala apps:
 - main.scala""".strip()


def test_report_only_includes_relevant_sections():
    report = {
        True: {},
        False: {
            IgnoredPath.message: set(['README.md', 'LICENSE']),
            ScalaChangeAndNotScalaApp.message: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons not to run tests ##

Path has no effect on build tasks:
 - LICENSE
 - README.md

Changes to Scala common libs are irrelevant to non-Scala apps:
 - main.scala""".strip()
