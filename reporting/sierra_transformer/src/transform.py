import json
from copy import deepcopy
from datetime import datetime, timedelta
from dateutil.parser import parse


def transform(input_data):
    # only look at the bib data for now
    try:
        json_string = input_data["maybeBibRecord"]["data"]
        bib_record = json.loads(json_string)
    except (KeyError, TypeError):
        bib_record = {}

    transformed = deepcopy(bib_record)

    # ignore varFields on this first pass
    try:
        del transformed["varFields"]
    except KeyError:
        pass

    # unpack fixedFields
    try:
        del transformed["fixedFields"]
        for key, value in bib_record["fixedFields"].items():
            transformed[f"fixed_field_{key}_{value['label']}"] = value["value"]
    except KeyError:
        pass

    # get rid of redundant norm fields
    norm_fields = [field for field in bib_record if field.startswith("norm")]
    for field in norm_fields:
        del transformed[field]

    # unpack bibLevel
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="bibLevel",
        subfields_to_keep="value",
    )

    # unpack country
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="country",
        subfields_to_keep="name",
    )

    # unpack language
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="lang",
        subfields_to_keep="name",
    )

    # unpack material types
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="materialType",
        subfields_to_keep="code",
    )

    # unpack locations
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="locations",
        subfields_to_keep=["name", "code"],
    )

    # unpack orders. note that in the second `unpack()` we view the transformed
    # record in order to flatten the semi-unpacked order locations
    transformed = unpack(
        view_record=bib_record,
        edit_record=transformed,
        field_name="orders",
        subfields_to_keep=["location", "date"],
    )

    transformed = unpack(
        view_record=transformed,
        edit_record=transformed,
        field_name="orders_location",
        subfields_to_keep=["name", "code"],
    )

    # parse the order dates
    try:
        transformed["orders_date"] = [
            parse(date) for date in transformed["orders_date"]
        ]
    except KeyError:
        pass

    # parse publish year
    transformed = parse_year_int_to_date(
        view_record=bib_record, edit_record=transformed, field_name="publishYear"
    )

    return transformed


def parse_year_int_to_date(view_record, edit_record, field_name):
    try:
        date_to_parse = deepcopy(view_record[field_name])
        del edit_record[field_name]
        year_from = datetime(date_to_parse, 1, 1)
        edit_record[field_name + "_from"] = year_from
        edit_record[field_name + "_to"] = (
            year_from + timedelta(days=365) - timedelta(seconds=1)
        )

    except KeyError:
        pass

    return edit_record


def unpack(view_record, edit_record, field_name, subfields_to_keep):
    """
    Parameters
    ----------
    view_record : dict
        original, unchanged record to inspect
    edit_record : dict
        a modified/modifiable copy of the original record, to which we will
        make changes. this is ultimately the unpacked version of the record
        which we will pass to elasticsearch
    field_name : str
        the field whose data we want to unpack
    subfields to keep : str, list
        the subfields of the field_name which we want to retain in the
        modified, flatter version of the data
    delete : bool
        should the field be deleted from the transformed record

    Returns
    -------
    edit_record : dict
        the modified record with an unpacked form of the specified field
    """
    try:
        data_to_unpack = deepcopy(view_record[field_name])
        del edit_record[field_name]

        if isinstance(subfields_to_keep, str):
            subfields_to_keep = [subfields_to_keep]

        if isinstance(data_to_unpack, dict):
            data_to_unpack = [data_to_unpack]

        for subfield in subfields_to_keep:
            edit_record[field_name + "_" + subfield] = [
                field[subfield] for field in data_to_unpack if subfield in field
            ]

    except KeyError:
        pass

    return edit_record
