from transform import transform


def test_cleans_simple_date():
    test_dict = {"UserDate1": "01/02/2003"}
    assert transform(test_dict) == {
        "UserDate1_raw": "01/02/2003",
        "UserDate1": "2003-01-02",
    }


def test_handles_null_date():
    test_dict = {"UserDate1": None}
    assert transform(test_dict) == {"UserDate1_raw": None, "UserDate1": None}


def test_handles_empty_datestring():
    test_dict = {"UserDate1": ""}
    assert transform(test_dict) == {"UserDate1_raw": "", "UserDate1": None}


def test_handles_badly_formatted_date():
    test_dict = {"UserDate1": "a badly formatted date"}
    assert transform(test_dict) == {
        "UserDate1_raw": "a badly formatted date",
        "UserDate1": None,
    }
