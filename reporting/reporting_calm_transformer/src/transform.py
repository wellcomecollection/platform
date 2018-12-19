import math
from copy import deepcopy
from dateutil.parser import parse


def convert_date_to_iso(date_string):
    try:
        return parse(date_string).date().isoformat()
    except (ValueError, TypeError):
        return None


def transform(record):
    transformed_record = deepcopy(record)
    for key, value in record.items():
        new_value = deepcopy(value)

        if isinstance(new_value, (int, float, complex)):
            if math.isnan(value):
                new_value = None

        if isinstance(new_value, list) and len(value) == 1:
            new_value = record[key][0]

        if isinstance(new_value, str):
            if new_value.startswith("'") and new_value.endswith("'"):
                new_value = new_value[1:-1]

        if key in keys_to_parse:
            transformed_record[key + "_raw"] = value
            new_value = convert_date_to_iso(new_value)

        transformed_record[key] = new_value
    return transformed_record


keys_to_parse = {
    "Modified",
    "Created",
    "UserDate1",
    "UserDate2",
    "UserDate3",
    "UserDate4",
}
