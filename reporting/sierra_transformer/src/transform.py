import json


def transform(input_data):
    # only look at the bib data for now
    json_string = input_data['maybeBibRecord']['data']
    bib_record = json.loads(json_string)

    # ignore varFields on first pass
    del bib_record['varFields']

    # unpack bibLevel
    if 'bibLevel' in bib_record:
        bib_record['bibLevel'] = bib_record['bibLevel']['value']

    # unpack country
    if 'country' in bib_record:
        bib_record['country'] = bib_record['country']['name']

    # unpack fixedFields
    if 'fixedFields' in bib_record:
        for key, value in bib_record['fixedFields'].items():
            bib_record[f"fixedFields_{key}_{value['label']}"] = value['value']
        del bib_record['fixedFields']

    # unpack language
    if 'lang' in bib_record:
        bib_record['lang'] = bib_record['lang']['name']

    # unpack locations
    if 'locations' in bib_record:
        bib_record['locations'] = [
            location['name'] for location in bib_record['locations']
        ]

    return bib_record
