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
                    'TopicArn': topic_arn
                }
            }
        ]
    }

    os.environ["BUCKET"] = bucket

    wiretap.main(event, s3_client=s3_client)

    resp = s3_client.list_objects(Bucket=bucket)
    assert "Contents" in resp, resp
    objects = resp["Contents"]

    assert len(objects) == 1
    s3_client.get_object(Bucket=bucket, Key=objects[0]["Key"])
