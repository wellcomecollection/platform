# -*- encoding: utf-8

from travistooling.decisions import (
    KnownAffectsThisTask,
    KnownDoesNotAffectThisTask,
    IgnoredPath,
)
from travistooling.reports import build_report_output


def test_build_complete_report():
    report = {
        True: {
            KnownAffectsThisTask: set(['foo/bar.txt', 'foo/baz.txt']),
        },
        False: {
            IgnoredPath: set(['README.md', 'LICENSE']),
            KnownDoesNotAffectThisTask: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons to run tests ##

KnownAffectsThisTask:
 - foo/bar.txt
 - foo/baz.txt


## Reasons not to run tests ##

IgnoredPath:
 - LICENSE
 - README.md

KnownDoesNotAffectThisTask:
 - main.scala""".strip()


def test_report_only_includes_relevant_sections():
    report = {
        True: {},
        False: {
            IgnoredPath: set(['README.md', 'LICENSE']),
            KnownDoesNotAffectThisTask: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons not to run tests ##

IgnoredPath:
 - LICENSE
 - README.md

KnownDoesNotAffectThisTask:
 - main.scala""".strip()
