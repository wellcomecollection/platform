import json
import datetime
from transform import transform, unpack, parse_year_int_to_date


def build_test_data(full=False):
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
        "iso_date": "1970-01-01T00:00:00Z",
        "date_year": 1970,
        "empty_string": "",
    }

    if full:
        return {
            "sierraId": {"recordNumber": "3075974"},
            "maybeBibRecord": {
                "id": {"recordNumber": "3075974"},
                "data": json.dumps(bib_record),
                "modifiedDate": "2018-11-12T11:55:59Z",
            },
            "itemRecords": {},
        }

    else:
        return bib_record


def test_parses_year_int_to_date_from():
    raw_data = build_test_data()
    parsed_from, _ = parse_year_int_to_date(raw_data, "date_year")
    assert parsed_from == datetime.datetime(1970, 1, 1, 0, 0, 0)


def test_parses_year_int_to_date_to():
    raw_data = build_test_data()
    _, parsed_to = parse_year_int_to_date(raw_data, "date_year")
    assert parsed_to == datetime.datetime(1970, 12, 31, 23, 59, 59)


def test_transform_preserves_unspecified_fields():
    raw_data = build_test_data(full=True)
    transformed = transform(raw_data)
    raw_bib_record = json.loads(raw_data["maybeBibRecord"]["data"])
    assert transformed["string_field"] == raw_bib_record["string_field"]


def test_unpacks_single_dict_single_subfield():
    raw_data = build_test_data()
    unpacked = unpack(raw_data, "single_dict_single_subfield", "a")
    assert unpacked == [["b"]]


def test_unpacks_single_dict_multiple_subfields():
    raw_data = build_test_data()
    unpacked = unpack(raw_data, "single_dict_multiple_subfields", ["a", "x"])
    assert unpacked == [["b"], ["y"]]


def test_unpacks_multiple_dicts_single_subfield():
    raw_data = build_test_data()
    unpacked = unpack(raw_data, "multiple_dicts_single_subfield", "a")
    assert unpacked == [["b", "c"]]


def test_unpacks_multiple_dicts_multiple_subfields():
    raw_data = build_test_data()
    unpacked = unpack(raw_data, "multiple_dicts_multiple_subfields", ["a", "x"])
    assert unpacked == [["b", "c"], ["y", "z"]]
