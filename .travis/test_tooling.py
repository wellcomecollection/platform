# -*- encoding: utf-8 -*-

import pytest

import tooling


@pytest.mark.parametrize('path, task, expected_result', [
    ('/sierra_adapter/terraform/main.tf', 'sierra_adapter-test', False),
    ('/sierra_adapter/terraform/main.tf', 'loris-test', False),
])
def test_affects_test(path, task, expected_result):
    assert tooling.affects_tests(path=path, task=task) == expected_result
