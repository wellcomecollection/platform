import json
from .hybrid_record_pipeline import process_messages


def create_sns_message(bucket_name, id, key):
    return {
        "Records": [
            {
                "Sns": {
                    "Message": f'{{"id":"{id}","version":1,"location":{{"namespace":"{bucket_name}","key":"{key}"}}}}',
                    "MessageAttributes": {},
                    "MessageId": "0cf7d798-64c8-45a7-a7bf-a9ebc94d1108",
                    "Type": "Notification",
                }
            }
        ]
    }


def given_s3_has(s3_client, bucket, key, data):
    s3_client.put_object(
        ACL="public-read",
        Bucket=bucket,
        Key=key,
        Body=data,
        CacheControl="max-age=0",
        ContentType="application/json",
    )


def identity_transform(record):
    return record


def test_saves_record_in_es(
    s3_client, bucket, elasticsearch_client, elasticsearch_index
):

    id = "V0000001"
    elasticsearch_doctype = "example"
    hybrid_data = '{"foo": "bar"}'
    key = "00/V0000001/0.json"

    given_s3_has(s3_client, bucket, key, json.dumps(hybrid_data))
    event = create_sns_message(bucket, id, key)

    process_messages(
        event,
        identity_transform,
        s3_client,
        elasticsearch_client,
        elasticsearch_index,
        elasticsearch_doctype,
    )

    es_record = elasticsearch_client.get(elasticsearch_index, elasticsearch_doctype, id)

    assert es_record["_source"] == json.loads(hybrid_data)
