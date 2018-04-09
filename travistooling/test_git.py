# -*- encoding: utf-8

from travistooling import git


def test_no_change_is_empty_diff():
    assert git.changed_files('HEAD', 'HEAD') == set()


def test_known_change_diff():
    assert git.changed_files('1228fc9^', '1228fc9') == set([
        'travistooling/decisionmaker.py',
        'travistooling/decisions.py',
    ])
