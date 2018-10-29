import json
import transformer_example


def create_sns_message(bucket_name, id, key):
    return {
        "Records": [
            {
                "EventSource": "aws:sns",
                "EventSubscriptionArn": "arn:aws:sns:eu-west-1:1111111111111111:reporting_miro_reindex_topic:7f684445-0aec-4dee-b3fc-aafc0e9afd67",
                "EventVersion": "1.0",
                "Sns": {
                    "Message": '{"id":"'
                    + id
                    + '","version":2,"location":{"namespace":"'
                    + bucket_name
                    + '","key":"'
                    + key
                    + '"}}',
                    "MessageAttributes": {},
                    "MessageId": "108920ea-19a2-550f-8c2e-74a7163ad107",
                    "Signature": "fxkrB6/WGHY8HkFqfsD4Ir6PtvIw97GZSXW7evIYposeyp/X+/TyOXOTkHo4WltsbJGL/udnRHP9gnBdZ7OWvynJMM76KPnb9/d8pRfb6EH6nD0kaTURQ1GIK7/UHiArtTqP8CHPCr+jxhcWHrX3WsxZV3jq4mkXm2PROFfvsVM3uGiinIFoXFJmBfTUvxygpOIaeg69nG+7CVkTvcggW+Tpm89KNZ+oWbkwrdMggDio23HbvgvW4caGSQ2Pha64LnW+hX1V4Opa+Sw8of47qPZJ1/YLK+NVNvyYO9ykSvbrrO1+wsj3aN60uE4DIqLPnP5fkrTydzBiw==",
                    "SignatureVersion": "1",
                    "SigningCertUrl": "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-ac565b8b1a6c5d992d285f9598aa1d9b.pem",
                    "Subject": "HybridRecordSender",
                    "Timestamp": "2018-10-26T12:49:28.829Z",
                    "TopicArn": "arn:aws:sns:eu-west-1:1111111111111111:reporting_miro_reindex_topic",
                    "Type": "Notification",
                    "UnsubscribeUrl": "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe",
                },
            }
        ]
    }


def create_s3_hybrid_data():
    return {
        "MiroCollection": "images-2",
        "data": '{"image_image_desc":"A horse\'s neck: diagonal feathered whorl.\\nRoyal Veterinary College."}',
        "id": "miro/A0000001",
        "sourceId": "A0000001",
        "sourceName": "miro",
        "version": 1,
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


def test(s3_client, bucket, elasticsearch_client, elasticsearch_index):
    id = "V0010033"
    elasticsearch_doctype = "example"
    hybrid_data = create_s3_hybrid_data()
    key = "33/V0010033/0.json"

    given_s3_has(s3_client, bucket, key, json.dumps(hybrid_data))

    event = create_sns_message(bucket, id, key)

    transformer_example.main(
        event,
        {},
        s3_client,
        elasticsearch_client,
        elasticsearch_index,
        elasticsearch_doctype,
    )

    es_record = elasticsearch_client.get(elasticsearch_index, elasticsearch_doctype, id)

    assert es_record["_source"] == hybrid_data
