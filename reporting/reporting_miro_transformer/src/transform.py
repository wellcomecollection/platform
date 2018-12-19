from dateutil.parser import parse


def transform(miro_transformable):
    transformed = drop_redundant_fields(miro_transformable, keys_to_drop)
    transformed = clean_dates(transformed, keys_to_parse)
    return transformed


def drop_redundant_fields(original_data, keys_to_drop):
    """
    strip out any of the original VHS fields which are either empty, sparse,
    redundantly filled, or just generally unwanted in our ES index
    """
    clean_data = {
        key: value for key, value in original_data.items() if key not in keys_to_drop
    }
    return clean_data


def clean_dates(data, keys_to_parse):
    """
    elasticsearch's default sort is alphabetical, and sorting dd/mm/yyyy dates
    alphabetically is rubbish
    """
    for key, value in data.items():
        if key in keys_to_parse:
            if isinstance(value, str):
                data[key] = convert_date_to_iso(value)
            elif isinstance(value, list):
                data[key] = [convert_date_to_iso(date) for date in value]
            else:
                pass
    return data


def convert_date_to_iso(date_string):
    try:
        return parse(date_string).date().isoformat()
    except (ValueError, TypeError):
        return None


keys_to_parse = {
    "all_amendment_date",
    "image_artwork_date_from",
    "image_artwork_date_to",
    "image_award_date_from",
    "image_award_date_to",
    "image_iap_catalog_date",
    "image_innopac_id_date",
    "image_int_date_alias.image_int_date_from",
    "image_int_date_alias.image_int_date_to",
    "image_int_date_from",
    "image_int_date_to",
    "image_pub_date_from",
    "image_pub_date_to",
    "image_pub_printing_date_from",
    "image_pub_printing_date_to",
    "image_tech_coloured_date_from",
    "image_tech_coloured_date_to",
    "image_tech_retouched_date_from",
    "image_tech_retouched_date_to",
    "image_tech_scanned_date_from",
    "image_tech_scanned_date_to",
    "image_wellcome_pub_date_from",
    "image_wellcome_pub_date_to",
}

keys_to_drop = {
    "_index",
    "_score",
    "_type",
    "all_draft",
    "all_psoft_export",
    "image_biomed_close_up",
    "image_corp_commissioned_by",
    "image_corp_contact_details",
    "image_corp_institution",
    "image_corp_keywords",
    "image_corp_photographer_freelance",
    "image_corp_purpose",
    "image_corp_shoot_sheet_number",
    "image_funding_type",
    "image_iap_invalid",
    "image_icd_note",
    "image_phys_notes",
    "image_source",
    "image_tech_manipulated_date_from",
    "image_tech_manipulated_date_to",
    "image_tech_manipulated_date",
    "image_web_img_filename",
    "image_web_img_size",
}
