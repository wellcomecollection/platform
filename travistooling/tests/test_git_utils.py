# -*- encoding: utf-8

import pytest

from travistooling.git_utils import get_changed_paths, git


def test_no_change_is_empty_diff():
    assert get_changed_paths('HEAD', 'HEAD') == set()


def test_known_change_diff():
    assert get_changed_paths('1228fc9^', '1228fc9') == set([
        'travistooling/decisionmaker.py',
        'travistooling/decisions.py',
    ])


def test_error_becomes_systemexit(capsys):
    with pytest.raises(SystemExit) as err:
        git('--what')
    assert err.value.code == 129
