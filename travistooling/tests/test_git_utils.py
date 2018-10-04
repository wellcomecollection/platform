# -*- encoding: utf-8

import pytest

from travistooling.git_utils import get_changed_paths, git


def test_no_change_is_empty_diff():
    assert get_changed_paths("HEAD", "HEAD") == set()


def test_known_change_diff():
    # By default, Travis only fetches 50 commits from GitHub when it
    # does a clone.  Because this test is looking for changes in a particular
    # commit, we need to make sure we fetch that commit before we start.
    #
    # Turning a shallow to an unshallow clone is moderately expensive, so we
    # do it in the body of the test so we can skip doing it in build jobs
    # that don't run this test.
    #
    # See https://docs.travis-ci.com/user/customizing-the-build#Git-Clone-Depth
    #
    try:
        git("fetch", "origin", "--unshallow")
    except SystemExit as err:  # pragma: no cover

        # When running tests locally, you normally have a full checkout,
        # so the command above is an error, and fails with:
        #
        #     fatal: --unshallow on a complete repository does not make sense
        #
        # In that case, we check we're seeing the expected exit code,
        # but we don't need to run this branch in the tests.
        assert err.code == 128

    assert get_changed_paths("1228fc9^", "1228fc9") == set(
        ["travistooling/decisionmaker.py", "travistooling/decisions.py"]
    )


def test_error_becomes_systemexit(capsys):
    with pytest.raises(SystemExit) as err:
        git("--what")
    assert err.value.code == 129
