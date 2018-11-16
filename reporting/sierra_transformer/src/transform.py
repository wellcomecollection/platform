from datetime import datetime, timedelta
from dateutil.parser import parse
import json


def transform(input_data):
    # only look at the bib data for now
    try:
        json_string = input_data['maybeBibRecord']['data']
        bib_record = json.loads(json_string)
    except (KeyError, TypeError):
        bib_record = {}

    # ignore varFields on first pass
    try:
        del bib_record['varFields']
    except KeyError:
        pass

    # unpack fixedFields
    try:
        for key, value in bib_record['fixedFields'].items():
            bib_record[f"fixed_field_{key}_{value['label']}"] = value['value']
        del bib_record['fixedFields']
    except KeyError:
        pass

    # unpack bibLevel
    bib_record['bibLevel_value'] = unpack(
        record=bib_record,
        field_name='bibLevel',
        subfields_to_keep='value'
    )
    try:
        del bib_record['bibLevel']
    except KeyError:
        pass

    # unpack country
    bib_record['country_name'] = unpack(
        record=bib_record,
        field_name='country',
        subfields_to_keep='name'
    )
    try:
        del bib_record['country']
    except KeyError:
        pass

    # unpack language
    bib_record['lang_name'] = unpack(
        record=bib_record,
        field_name='lang',
        subfields_to_keep='name'
    )
    try:
        del bib_record['lang']
    except KeyError:
        pass

    # unpack material types
    bib_record['materialType_code'] = unpack(
        record=bib_record,
        field_name='materialType',
        subfields_to_keep='code'
    )
    try:
        del bib_record['materialType']
    except KeyError:
        pass

    # unpack locations
    locations_name, locations_code = unpack(
        record=bib_record,
        field_name='locations',
        subfields_to_keep=['name', 'code']
    )
    bib_record['locations_name'] = locations_name
    bib_record['locations_code'] = locations_code
    try:
        del bib_record['locations']
    except KeyError:
        pass

    # unpack orders
    orders_date = unpack(
        record=bib_record,
        field_name='orders',
        subfields_to_keep='date'
    )
    try:
        bib_record['orders_date'] = [parse(date) for date in orders_date]
    except:
        pass

    try:
        orders = bib_record['orders']
        order_location_codes, order_location_names = unpack(
            record=orders,
            field_name='location',
            subfields_to_keep=['name', 'code']
        )
        bib_record['order_location_codes'] = order_location_codes
        bib_record['order_location_names'] = order_location_names
        del bib_record['orders']
    except KeyError:
        pass

    # parse publish year
    year_from, year_to = parse_year_int_to_date(bib_record, 'publishYear')
    bib_record['publishYear_from'] = year_from
    bib_record['publishYear_to'] = year_to
    try:
        del bib_record['publishYear']
    except KeyError:
        pass

    # get rid of redundant norm fields
    norm_fields = [field for field in bib_record if field.startswith('norm')]
    for field in norm_fields:
        del bib_record[field]

    return bib_record


def parse_year_int_to_date(bib_record, field_name):
    try:
        year_from = datetime(bib_record[field_name], 1, 1)
        year_to = year_from + timedelta(days=365) - timedelta(seconds=1)
    except KeyError:
        year_from = None
        year_to = None
    return year_from, year_to


def unpack(record, field_name, subfields_to_keep):
    if isinstance(subfields_to_keep, str):
        subfields_to_keep = [subfields_to_keep]

    try:
        data_to_unpack = record[field_name]
        if isinstance(data_to_unpack, dict):
            data_to_unpack = [data_to_unpack]

        unpacked = [
            [field[subfield] for field in data_to_unpack]
            for subfield in subfields_to_keep
        ]

    except (KeyError, TypeError):
        unpacked = [[None] for subfield in subfields_to_keep]

    return unpacked
