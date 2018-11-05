import os
import json
import miro_transformer
from reporting_pipeline.test_pipeline import given_s3_has, create_sns_message


def load_test_data():
    '''
    load some raw MIRO json and a transformed version to test whether the
    transform is being correctly applied
    '''
    current_path = os.path.dirname(__file__)
    raw_data_path = os.path.join(current_path, 'test_data/raw_data.json')
    transformed_data_path = os.path.join(current_path, 'test_data/transformed_data.json')

    with open(raw_data_path) as f:
        raw_data = f.read()

    with open(transformed_data_path) as f:
        transformed_data = json.load(f)

    return raw_data, transformed_data


def create_miro_hybrid_data(raw_data):
    return {
        "MiroCollection": "images-2",
        "data": raw_data,
        "id": "miro/A0000001",
        "sourceId": "A0000001",
        "sourceName": "miro",
        "version": 1,
    }


def test_saves_record_in_es(
    s3_client, bucket, elasticsearch_client, elasticsearch_index
):
    raw_data, transformed_data = load_test_data()
    id = "V0010033"
    elasticsearch_doctype = "example"
    hybrid_data = create_miro_hybrid_data(raw_data)
    key = "33/V0010033/0.json"

    given_s3_has(s3_client, bucket, key, json.dumps(hybrid_data))
    event = create_sns_message(bucket, id, key)

    miro_transformer.main(
        event,
        {},
        s3_client,
        elasticsearch_client,
        elasticsearch_index,
        elasticsearch_doctype
    )

    es_record = elasticsearch_client.get(elasticsearch_index, elasticsearch_doctype, id)

    assert es_record["_source"] == transformed_data
