from transform import transform


def test_cleans_simple_date():
    test_dict = {"data": '{"test_date_from": "01/01/1970"}'}
    assert transform(test_dict) == {"test_date_from": "1970-01-01"}


def test_handles_null_date():
    test_dict = {"data": '{"test_date_from": null}'}
    assert transform(test_dict) == {"test_date_from": None}


def test_handles_empty_datestring():
    test_dict = {"data": '{"test_date_from": ""}'}
    assert transform(test_dict) == {"test_date_from": ""}


def test_handles_badly_formatted_date():
    test_dict = {"data": '{"test_date_from": "a badly formatted date"}'}
    assert transform(test_dict) == {"test_date_from": ""}
