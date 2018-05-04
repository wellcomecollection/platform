# -*- encoding: utf-8

import pytest

from travistooling.sbt_dependency_checker import does_path_depend_on_library


@pytest.mark.parametrize('path, library_name, expected_result', [
    ('catalogue_api/api', 'messaging', False),
    ('catalogue_api/api', 'elasticsearch', True),
])
def test_does_path_depend_on_library(path, library_name, expected_result):
    actual_result = does_path_depend_on_library(
        path=path, library_name=library_name
    )
    assert actual_result == expected_result
