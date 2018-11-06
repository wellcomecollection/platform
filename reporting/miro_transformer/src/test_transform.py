from transform import transform


def test_cleans_simple_date():
    test_dict = {"data": '{"some_date": "01/01/1970"}'}
    assert transform(test_dict) == {"some_date": "1970-01-01"}


def test_handles_null_date():
    test_dict = {"data": '{"some_date": null}'}
    assert transform(test_dict) == {"some_date": None}


def test_handles_empty_datestring():
    test_dict = {"data": '{"some_date": ""}'}
    assert transform(test_dict) == {"some_date": ""}


def test_handles_badly_formatted_date():
    test_dict = {"data": '{"some_date": "a badly formatted date"}'}
    assert transform(test_dict) == {"some_date": "a badly formatted date"}
