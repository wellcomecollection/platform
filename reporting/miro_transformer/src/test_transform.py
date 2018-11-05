import pytest
from transform import transform


def test_cleans_simple_date():
    test_dict = {'data': '{"date": "01/01/1970"}'}
    assert transform(test_dict) == {'date': '1970-01-01'}


def test_handles_null_date():
    test_dict = {'data': '{"date": null}'}
    assert transform(test_dict) == {'date': None}


def test_handles_empty_datestring():
    test_dict = {'data': '{"date": ""}'}
    assert transform(test_dict) == {'date': None}


def test_handles_badly_formatted_date():
    with pytest.raises(ValueError):
        test_dict = {'data': '{"date": "a badly formatted date"}'}
        transform(test_dict)
