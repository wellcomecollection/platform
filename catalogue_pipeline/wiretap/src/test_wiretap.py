import datetime as dt
import json
import os

import wiretap


def test_messages_are_copied_to_s3(s3_client, bucket):
    message = "message!"
    topic_arn = "arn:aws:sns:eu-west-1:5865:catalogue_pipeline_es_ingest"
    event = {
        'Records': [
            {
                'Sns': {
                    'Message': message,
                    'TopicArn': topic_arn,
                    'Timestamp': dt.datetime.now().isoformat()
                }
            },
            {
                'Sns': {
                    'Message': message * 2,
                    'TopicArn': topic_arn,
                    'Timestamp': dt.datetime.now().isoformat()
                }
            },
            {
                'Sns': {
                    'Message': message * 3,
                    'TopicArn': topic_arn,
                    'Timestamp': dt.datetime.now().isoformat()
                }
            }
        ]
    }

    os.environ["BUCKET"] = bucket

    wiretap.main(event, s3_client=s3_client)

    resp = s3_client.list_objects(Bucket=bucket)
    assert "Contents" in resp, resp
    objects = resp["Contents"]

    assert len(objects) == len(event['Records'])

    expected_bodies = [r['Sns'] for r in event['Records']]

    actual_bodies = set()
    for obj in objects:
        key = obj['Key']
        body = s3_client.get_object(Bucket=bucket, Key=key)['Body'].read()
        actual_bodies.add(body)

    for b in actual_bodies:
        assert json.loads(b) in expected_bodies
