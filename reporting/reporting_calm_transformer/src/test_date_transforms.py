from transform import transform


def test_cleans_simple_date():
    test_dict = {"UserDate1": "01/01/1970"}
    assert transform(test_dict) == {"UserDate1": "1970-01-01"}


def test_handles_null_date():
    test_dict = {"UserDate1": None}
    assert transform(test_dict) == {"UserDate1": None}


def test_handles_empty_datestring():
    test_dict = {"UserDate1": ""}
    assert transform(test_dict) == {"UserDate1": None}


def test_handles_badly_formatted_date():
    test_dict = {"UserDate1": "a badly formatted date"}
    assert transform(test_dict) == {"UserDate1": None}
