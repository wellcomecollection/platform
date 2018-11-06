import json
import miro_transformer
from reporting_pipeline.test_pipeline import given_s3_has, create_sns_message


def create_miro_hybrid_data(raw_data):
    return {
        "MiroCollection": "images",
        "data": raw_data,
        "id": "miro/A0000001",
        "sourceId": "A0000001",
        "sourceName": "miro",
        "version": 1,
    }


def raw_data():
    return """{
                \"an_array\": [
                    \"a value\",
                    \"another value\"
                ],
                \"an_array_of_date_from\": [
                            \"22/04/2017\",
                            \"19/12/2011\"],
                \"string_field\": \"a string\",
                \"null_field\": null,
                \"single_date_from\": \"24/04/2004\",
                \"dict_field\": {
                      \"a\": \"a_value\"
                  },
                \"image_source\": \"ignored field\"
            }"""


def transformed_data():
    return {
        "an_array": ["a value", "another value"],
        "an_array_of_date_from": ["2017-04-22", "2011-12-19"],
        "string_field": "a string",
        "null_field": None,
        "single_date_from": "2004-04-24",
        "dict_field": {"a": "a_value"},
    }


def test_saves_record_in_es(
    s3_client, bucket, elasticsearch_client, elasticsearch_index
):
    id = "V0010033"
    elasticsearch_doctype = "example"
    hybrid_data = create_miro_hybrid_data(raw_data())
    key = "33/V0010033/0.json"

    given_s3_has(s3_client, bucket, key, json.dumps(hybrid_data))
    event = create_sns_message(bucket, id, key)

    miro_transformer.main(
        event,
        {},
        s3_client,
        elasticsearch_client,
        elasticsearch_index,
        elasticsearch_doctype,
    )

    es_record = elasticsearch_client.get(elasticsearch_index, elasticsearch_doctype, id)

    assert es_record["_source"] == transformed_data()
