import json
from datetime import datetime

def transform(record):
    """
    Parameters
    ----------
    record : dict
        the raw data from VHS in a malleable dict format

    Returns
    -------
    transformed_record : dict
        record with necessary transformations applied, ready to be formatted for
        elasticsearch ingestion
    """
    original_data = json.loads(record["data"])
    transformed = drop_redundant_fields(original_data, keys_to_drop)
    transformed = clean_dates(transformed)
    return transformed


def drop_redundant_fields(original_data, keys_to_drop):
    """
    strip out any of the original VHS fields which are either empty, sparse,
    redundantly filled, or just generally unwanted in our ES index
    """
    clean_data = {
        key: value 
        for key, value in original_data.items() 
        if key not in keys_to_drop
    }
    return clean_data


def clean_dates(data):
    '''
    elasticsearch's default sort is alphabetical, and sorting dd/mm/yyyy dates
    alphabetically is rubbish
    '''
    for key, value in data.items():
        if 'date' in key and value is not None:
            if type(value) == str:
                data[key] = convert_date_to_iso(value)
            elif type(value) == list:
                data[key] = [convert_date_to_iso(date) for date in value]
    return data


def convert_date_to_iso(date_string):
    if date_string is None or date_string is '':
        date = None
    else:
        date = datetime.strptime(date_string, '%d/%m/%Y').date().isoformat()
    return date


keys_to_drop = [
    '_index',
    '_score',
    '_type',
    'all_draft',
    'all_psoft_export',
    'image_biomed_close_up',
    'image_corp_commissioned_by',
    'image_corp_contact_details',
    'image_corp_institution',
    'image_corp_keywords',
    'image_corp_photographer_freelance',
    'image_corp_purpose',
    'image_corp_shoot_sheet_number',
    'image_funding_type',
    'image_iap_image_no',
    'image_iap_invalid',
    'image_icd_note',
    'image_no_calc',
    'image_phys_notes',
    'image_source',
    'image_tech_manipulated_date_from',
    'image_tech_manipulated_date_to',
    'image_tech_manipulated_date',
    'image_web_img_filename',
    'image_web_img_size'
]