import json


def transform(input_data):
    # only look at the bib data for now
    try:
        json_string = input_data['maybeBibRecord']['data']
        bib_record = json.loads(json_string)
    except KeyError:
        bib_record = {}

    # ignore varFields on first pass
    try:
        del bib_record['varFields']
    except KeyError:
        pass

    # unpack bibLevel
    try:
        bib_record['bibLevel'] = bib_record['bibLevel']['value']
    except KeyError:
        bib_record['bibLevel'] = None

    # unpack country
    try:
        bib_record['country'] = bib_record['country']['name']
    except KeyError:
        bib_record['country'] = None

    # unpack fixedFields
    try:
        for key, value in bib_record['fixedFields'].items():
            bib_record[f"fixed_field_{key}_{value['label']}"] = value['value']
        del bib_record['fixedFields']
    except KeyError:
        pass

    # unpack language
    try:
        bib_record['lang'] = bib_record['lang']['name']
    except KeyError:
        bib_record['lang'] = None

    # unpack locations
    try:
        bib_record['locations'] = [
            location['name'] for location in bib_record['locations']
        ]
    except KeyError:
        bib_record['locations'] = None

    # unpack material types
    try:
        bib_record['materialType'] = bib_record['materialType']['code']
    except KeyError:
        bib_record['materialType'] = None

    # unpack orders
    try:
        bib_record['order_locations'] = [
            order['location'] for order in bib_record['orders']
        ]
        bib_record['order_dates'] = [
            order['date'] for order in bib_record['orders']]
        ]
        del bib_record['orders']
    except KeyError:
        pass

    # get rid of redundant norm fields
    norm_fields = [field for field in bib_record if field.startswith('norm')]

    for field in norm_fields:
        del bib_record[field]

    return bib_record
