import json


def transform(input_data):
    # only look at the bib data for now
    json_string = input_data["maybeBibRecord"]["data"]
    bib_record = json.loads(json_string)

    # ignore varFields on first pass
    del bib_record["varFields"]

    # unpack bibLevel
    if "bibLevel" in bib_record:
        try:
            bib_record["bibLevel"] = bib_record["bibLevel"]["value"]
        except KeyError:
            bib_record["bibLevel"] = None

    # unpack country
    if "country" in bib_record:
        bib_record["country"] = bib_record["country"]["name"]

    # unpack fixedFields
    if "fixedFields" in bib_record:
        for key, value in bib_record["fixedFields"].items():
            bib_record[f"fixed_field_{key}_{value['label']}"] = value["value"]
        del bib_record["fixedFields"]

    # unpack language
    if "lang" in bib_record:
        try:
            bib_record["lang"] = bib_record["lang"]["name"]
        except KeyError:
            bib_record["lang"] = None

    # unpack locations
    if "locations" in bib_record:
        bib_record["locations"] = [
            location["name"] for location in bib_record["locations"]
        ]

    # unpack material types
    if "materialType" in bib_record:
        bib_record["materialType"] = bib_record["materialType"]["code"]

    # unpack orders
    if "orders" in bib_record:
        for order in bib_record["orders"]:
            bib_record["order_locations"] = order["location"]
            bib_record["order_dates"] = order["date"]
        del bib_record["orders"]

    # get rid of redundant norm fields
    norm_fields = [field for field in bib_record if field.startswith("norm")]

    for field in norm_fields:
        del bib_record[field]

    return bib_record
