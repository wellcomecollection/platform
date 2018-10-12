# -*- encoding: utf-8

import os
import subprocess
from unittest import mock

import pytest

from travistooling import travis_utils


@pytest.fixture
def cleanup_secrets():  # pragma: no cover
    yield
    subprocess.check_call(["rm", "-rf", "secrets"])


def test_not_in_travis_is_an_error():
    with pytest.raises(KeyError):
        travis_utils.branch_name()


def test_travis_branch_name_on_master():
    mock_environ = {
        "TRAVIS_PULL_REQUEST": "false",
        "TRAVIS_BRANCH": "master",
        "TRAVIS_PULL_REQUEST_BRANCH": "feature-branch",
    }
    with mock.patch.dict(os.environ, mock_environ, clear=True):
        assert travis_utils.branch_name() == "master"


def test_travis_branch_name_on_pr():
    mock_environ = {
        "TRAVIS_PULL_REQUEST": "true",
        "TRAVIS_BRANCH": "master",
        "TRAVIS_PULL_REQUEST_BRANCH": "feature-branch",
    }
    with mock.patch.dict(os.environ, mock_environ, clear=True):
        assert travis_utils.branch_name() == "feature-branch"


@pytest.mark.skipif(
    os.environ["encrypted_83630750896a_key"] == "",
    reason="Encrypted env vars are not available",
)
def test_unpack_secrets(cleanup_secrets):  # pragma: no cover
    travis_utils.unpack_secrets()
    assert os.path.exists("secrets/id_rsa")


def test_unpack_secrets_with_no_secrets_file():
    mock_environ = {
        "encrypted_83630750896a_key": "K3Y",
        "encrypted_83630750896a_iv": "1V",
    }
    with mock.patch.dict(os.environ, mock_environ, clear=True):
        with pytest.raises(SystemExit) as err:
            travis_utils.unpack_secrets()
    assert err.value.code == 1


def test_unpack_secrets_with_no_env_vars():
    with mock.patch.dict(os.environ, {}, clear=True):
        with pytest.raises(SystemExit) as err:
            travis_utils.unpack_secrets()
    assert err.value.code == 2
