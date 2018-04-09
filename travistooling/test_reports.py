# -*- encoding: utf-8

from travistooling.decisions import (
    KnownAffectsThisJob,
    KnownDoesNotAffectThisJob,
    IgnoredPath,
)
from travistooling.reports import build_report_output


def test_build_complete_report():
    report = {
        True: {
            KnownAffectsThisJob: set(['foo/bar.txt', 'foo/baz.txt']),
        },
        False: {
            IgnoredPath: set(['README.md', 'LICENSE']),
            KnownDoesNotAffectThisJob: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons to run tests ##

KnownAffectsThisJob:
 - foo/bar.txt
 - foo/baz.txt


## Reasons not to run tests ##

IgnoredPath:
 - LICENSE
 - README.md

KnownDoesNotAffectThisJob:
 - main.scala""".strip()


def test_report_only_includes_relevant_sections():
    report = {
        True: {},
        False: {
            IgnoredPath: set(['README.md', 'LICENSE']),
            KnownDoesNotAffectThisJob: set(['main.scala']),
        },
    }
    assert build_report_output(report) == """
## Reasons not to run tests ##

IgnoredPath:
 - LICENSE
 - README.md

KnownDoesNotAffectThisJob:
 - main.scala""".strip()
