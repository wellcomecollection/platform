import math
from transform import transform


def test_strips_double_quoted_strings():
    raw_data = {
        "quote_before": "'some text",
        "quote_after": "some text'",
        "quote_both": "'some text'",
    }
    transformed = transform(raw_data)
    assert transformed["quote_before"] == "'some text"
    assert transformed["quote_after"] == "some text'"
    assert transformed["quote_both"] == "some text"


def test_unpacks_single_element_lists():
    raw_data = {"single_element_list": ["single list item"]}
    transformed = transform(raw_data)
    assert transformed["single_element_list"] == "single list item"


def test_transforms_nan_to_none():
    raw_data = {"nan_field": math.nan}
    transformed = transform(raw_data)
    assert transformed["nan_field"] is None
