# -*- encoding: utf-8

import pytest

from travistooling.make_utils import make


def test_working_make_cmd_is_ok():
    make('format', '--dry-run')


def test_bad_make_cmd_is_error():
    with pytest.raises(SystemExit) as err:
        make('doesnotexist', '--dry-run')
    assert err.value.code == 2
