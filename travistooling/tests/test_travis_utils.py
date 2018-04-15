# -*- encoding: utf-8

import os

import pytest

from travistooling import travis_utils


def test_not_in_travis_is_an_error():
    with pytest.raises(KeyError):
        travis_utils.branch_name()


def test_travis_branch_name_on_master():
    os.environ = {
        'TRAVIS_PULL_REQUEST': 'false',
        'TRAVIS_BRANCH': 'master',
        'TRAVIS_PULL_REQUEST_BRANCH': 'feature-branch',
    }
    assert travis_utils.branch_name() == 'master'


def test_travis_branch_name_on_pr():
    os.environ = {
        'TRAVIS_PULL_REQUEST': 'true',
        'TRAVIS_BRANCH': 'master',
        'TRAVIS_PULL_REQUEST_BRANCH': 'feature-branch',
    }
    assert travis_utils.branch_name() == 'feature-branch'


def test_unpack_secrets():
    travis_utils.unpack_secrets()
    assert os.path.exists('secrets/id_rsa')


def test_unpack_secrets_with_no_secrets_file():
    os.environ = {
        'encrypted_83630750896a_key': 'K3Y',
        'encrypted_83630750896a_iv': '1V',
    }
    with pytest.raises(SystemExit) as err:
        travis_utils.unpack_secrets()
    assert err.value.code == 1


def test_unpack_secrets_with_no_env_vars():
    os.environ = {}
    with pytest.raises(SystemExit) as err:
        travis_utils.unpack_secrets()
    assert err.value.code == 2
