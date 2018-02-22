# -*- encoding: utf-8 -*-

import pytest

import travistooling as tooling


@pytest.mark.parametrize('path, task, expected_result', [
    ('sierra_adapter/terraform/main.tf', 'sierra_adapter-test', False),
    ('sierra_adapter/terraform/main.tf', 'loris-test', False),

    ('sierra_adapter/Makefile', 'sierra_adapter-test', True),
    ('sierra_adapter/Makefile', 'loris-test', False),
    ('sierra_adapter/Makefile', 'unknown-task', True),

    ('sierra_adapter/common/main.scala', 'sierra_adapter-test', True),
    ('sierra_adapter/common/main.scala', 'loris-test', False),
    ('sierra_adapter/common/main.scala', 's3_demultiplexer-test', False),
    ('sierra_adapter/common/main.scala', 'sierra_window_generator-test', False),
    ('sierra_adapter/common/main.scala', 'unknown-task', True),

    ('project/Dependencies.scala', 'sierra_adapter-test', True),
    ('project/Dependencies.scala', 'sierra_window_generator-test', False),
])
def test_affects_test(path, task, expected_result):
    assert tooling.affects_tests(path=path, task=task) == expected_result
