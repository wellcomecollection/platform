# -*- encoding: utf-8 -*-


from snapshot_convertor_job_generator import _run


def createS3Event(bucket_name, object_key, event_name):
    return {
        "Records": [
            {
                "eventTime": "1970-01-01T00:00:00.000Z",
                "eventName": event_name,
                "s3": {
                    "bucket": {
                        "name": bucket_name
                    },
                    "object": {
                        "key": object_key,
                        "size": 0,
                        "versionId": "v0"
                    }
                }
            }
        ]
    }


def test_snapshot_convertor_job_generator_sends_message_for_object_created_event(sns_client, topic_arn):
    bucket_name = "bukkit"
    object_key = "test0001.json"
    event_name = "s3:ObjectCreated:Any"

    event = createS3Event(
        bucket_name=bucket_name,
        object_key=object_key,
        event_name=event_name
    )

    expected_job = {
        "bucketName": bucket_name,
        "objectKey": object_key
    }

    _run(
        event=event,
        sns_client=sns_client,
        topic_arn=topic_arn
    )

    actual_messages = [m[':message'] for m in sns_client.list_messages()]

    assert actual_messages == [expected_job]


def test_snapshot_convertor_job_generator_does_not__send_message_for_any_other_event(sns_client, topic_arn):
    bucket_name = "bukkit"
    object_key = "test0001.json"
    event_name = "s3:SomethingElse:Any"

    event = createS3Event(
        bucket_name=bucket_name,
        object_key=object_key,
        event_name=event_name
    )

    _run(
        event=event,
        sns_client=sns_client,
        topic_arn=topic_arn
    )

    assert len(sns_client.list_messages()) == 0
