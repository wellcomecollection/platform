# -*- encoding: utf-8

import os

import pytest

from travistooling import travisenv


def test_not_in_travis_is_an_error():
    with pytest.raises(KeyError):
        travisenv.branch_name()


def test_travis_branch_name_on_master():
    os.environ = {
        'TRAVIS_PULL_REQUEST': 'false',
        'TRAVIS_BRANCH': 'master',
        'TRAVIS_PULL_REQUEST_BRANCH': 'feature-branch',
    }
    assert travisenv.branch_name() == 'master'


def test_travis_branch_name_on_pr():
    os.environ = {
        'TRAVIS_PULL_REQUEST': 'true',
        'TRAVIS_BRANCH': 'master',
        'TRAVIS_PULL_REQUEST_BRANCH': 'feature-branch',
    }
    assert travisenv.branch_name() == 'feature-branch'
