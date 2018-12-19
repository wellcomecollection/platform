import json
import datetime
from copy import deepcopy
from transform import transform, unpack


def build_bib_record():
    bib_record = {
        "string_field": "some string",
        "boolean_field": True,
        "single_dict_single_subfield": {"a": "b"},
        "single_dict_multiple_subfields": {"a": "b", "x": "y"},
        "multiple_dicts_single_subfield": [{"a": "b"}, {"a": "c"}],
        "multiple_dicts_multiple_subfields": [
            {"a": "b", "x": "y"},
            {"a": "c", "x": "z"},
        ],
        "orders_date": ["2003-02-01T00:00:00"],
        "publishYear": 1970,
        "empty_string": "",
    }
    return bib_record


def build_sierra_transformable():
    bib_record = build_bib_record()
    sierra_transformable = {
        "sierraId": {"recordNumber": "3075974"},
        "maybeBibRecord": {
            "id": {"recordNumber": "3075974"},
            "data": json.dumps(bib_record),
            "modifiedDate": "2018-11-12T11:55:59Z",
        },
        "itemRecords": {},
    }
    return sierra_transformable


def test_parses_year_int_to_date():
    raw_data = build_sierra_transformable()
    transformed = transform(raw_data)
    from_date = transformed["publishYear_from"]
    to_date = transformed["publishYear_to"]
    assert from_date == datetime.datetime(1970, 1, 1, 0, 0, 0)
    assert to_date == datetime.datetime(1970, 12, 31, 23, 59, 59)


def test_parses_order_dates():
    raw_data = build_sierra_transformable()
    transformed = transform(raw_data)
    order_dates = transformed["orders_date"]
    assert order_dates == [datetime.datetime(2003, 2, 1, 0, 0, 0)]


def test_transform_preserves_unspecified_fields():
    raw_data = build_sierra_transformable()
    transformed = transform(raw_data)
    raw_bib_record = json.loads(raw_data["maybeBibRecord"]["data"])
    assert transformed["string_field"] == raw_bib_record["string_field"]


def test_unpacks_single_dict_single_subfield():
    raw_data = build_bib_record()
    transformed = deepcopy(raw_data)
    transformed = unpack(
        view_record=raw_data,
        edit_record=transformed,
        field_name="single_dict_single_subfield",
        subfields_to_keep="a",
    )
    assert transformed["single_dict_single_subfield_a"] == ["b"]


def test_unpacks_single_dict_multiple_subfields():
    raw_data = build_bib_record()
    transformed = deepcopy(raw_data)
    transformed = unpack(
        view_record=raw_data,
        edit_record=transformed,
        field_name="single_dict_multiple_subfields",
        subfields_to_keep=["a", "x"],
    )
    assert transformed["single_dict_multiple_subfields_a"] == ["b"]
    assert transformed["single_dict_multiple_subfields_x"] == ["y"]


def test_unpacks_multiple_dicts_single_subfield():
    raw_data = build_bib_record()
    transformed = deepcopy(raw_data)
    transformed = unpack(
        view_record=raw_data,
        edit_record=transformed,
        field_name="multiple_dicts_single_subfield",
        subfields_to_keep="a",
    )
    assert transformed["multiple_dicts_single_subfield_a"] == ["b", "c"]


def test_unpacks_multiple_dicts_multiple_subfields():
    raw_data = build_bib_record()
    transformed = deepcopy(raw_data)
    transformed = unpack(
        view_record=raw_data,
        edit_record=transformed,
        field_name="multiple_dicts_multiple_subfields",
        subfields_to_keep=["a", "x"],
    )
    assert transformed["multiple_dicts_multiple_subfields_a"] == ["b", "c"]
    assert transformed["multiple_dicts_multiple_subfields_x"] == ["y", "z"]


def test_leaves_missing_subfield_empty():
    raw_data = build_bib_record()
    transformed = deepcopy(raw_data)
    transformed = unpack(
        view_record=raw_data,
        edit_record=transformed,
        field_name="single_dict_single_subfield",
        subfields_to_keep=["a", "missing_subfield"],
    )
    assert transformed["single_dict_single_subfield_a"] == ["b"]
    assert transformed["single_dict_single_subfield_missing_subfield"] == []
