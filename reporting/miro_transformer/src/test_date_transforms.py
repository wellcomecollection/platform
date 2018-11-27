from transform import transform


def test_cleans_simple_date():
    test_dict = {"data": {"all_amendment_date": "01/01/1970"}}
    assert transform(test_dict) == {"all_amendment_date": "1970-01-01"}


def test_handles_null_date():
    test_dict = {"data": {"all_amendment_date": None}}
    assert transform(test_dict) == {"all_amendment_date": None}


def test_handles_empty_datestring():
    test_dict = {"data": {"all_amendment_date": ""}}
    assert transform(test_dict) == {"all_amendment_date": ""}


def test_allows_badly_formatted_date():
    test_dict = {"data": {"all_amendment_date": "a badly formatted date"}}
    assert transform(test_dict) == {"all_amendment_date": "a badly formatted date"}
