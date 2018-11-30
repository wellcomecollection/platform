import math
from transform import transform


def raw_data():
    return {
        "quote_before": "'some text",
        "quote_after": "some text'",
        "quote_both": "'some text'",
        "single_element_list": ["single list item"],
        "nan_field": math.nan,
    }


def test_strips_quoted_strings():
    transformed = transform(raw_data())
    assert transformed["quote_before"] == "some text"
    assert transformed["quote_after"] == "some text"
    assert transformed["quote_both"] == "some text"


def test_unpacks_single_element_lists():
    transformed = transform(raw_data())
    assert transformed["single_element_list"] == "single list item"


def test_transforms_nan_to_none():
    transformed = transform(raw_data())
    assert transformed["nan_field"] is None
