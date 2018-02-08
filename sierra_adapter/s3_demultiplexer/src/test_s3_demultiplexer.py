# -*- encoding: utf-8 -*-

import json

from s3_demultiplexer import main


def test_end_to_end_demultiplexer(s3_client, sns_client, topic_arn):
    records = [
        {'colour': 'red', 'letter': 'R'},
        {'colour': 'green', 'letter': 'G'},
        {'colour': 'blue', 'letter': 'B'},
    ]

    s3_client.create_bucket(Bucket='bukkit')
    s3_client.put_object(
        Bucket='bukkit',
        Key='test0001.json',
        Body=json.dumps(records)
    )

    event = {
        "Records": [
            {
                "eventTime": "1970-01-01T00:00:00.000Z",
                "eventName": "event-type",
                "s3": {
                    "bucket": {
                        "name": "bukkit"
                    },
                    "object": {
                        "key": "test0001.json",
                        "size": len(json.dumps(records)),
                        "versionId": "v2"
                    }
                }
            }
        ]
    }

    main(
        event=event,
        s3_client=s3_client,
        sns_client=sns_client
    )

    actual_messages = [m[':message'] for m in sns_client.list_messages()]
    assert actual_messages == records
