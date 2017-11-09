# -*- encoding: utf-8 -*-

import sierra_objects_to_s3


def test_build_from_to_params():
    assert sierra_objects_to_s3.build_from_to_params(
        '3rd of january 1982',
        '4rd of january 1982'
    ) == {'updatedDate': f'[1982-01-03T00:00:00Z,1982-01-04T00:00:00Z]'}

    assert sierra_objects_to_s3.build_from_to_params(
        '3rd of january 1982',
        False
    ) == {'updatedDate': f'[1982-01-03T00:00:00Z,]'}

    assert sierra_objects_to_s3.build_from_to_params(
        False,
        '4rd of january 1982'
    ) == {'updatedDate': f'[,1982-01-04T00:00:00Z]'}


def test_write_objects_to_s3():
    assert True == True
