import s3_utils

import dateutil.parser


def s3_event():
    return {
        "Records": [
            {
                "eventVersion": "2.0",
                "eventSource": "aws:s3",
                "awsRegion": "us-east-1",
                "eventTime": "1970-01-01T00:00:00.000Z",
                "eventName": "event-type",
                "userIdentity": {
                    "principalId": "Amazon-customer-ID-of-the-user-who-caused-the-event"
                },
                "requestParameters": {
                    "sourceIPAddress": "ip-address-where-request-came-from"
                },
                "responseElements": {
                    "x-amz-request-id": "Amazon S3 generated request ID",
                    "x-amz-id-2": "Amazon S3 host that processed the request"
                },
                "s3": {
                    "s3SchemaVersion": "1.0",
                    "configurationId": "ID found in the bucket notification configuration",
                    "bucket": {
                        "name": "bucket-name",
                        "ownerIdentity": {
                            "principalId": "Amazon-customer-ID-of-the-bucket-owner"
                        },
                        "arn": "bucket-ARN"
                    },
                    "object": {
                        "key": "bucket-name",
                        "size": 1234,
                        "eTag": "object eTag",
                        "versionId": "v2",
                        "sequencer": "foo"
                    }
                }
            }
        ]
    }


def test_parse_s3_event():
    e = s3_event()

    parsed_events = s3_utils.parse_s3_record(e)
    expected_datetime = dateutil.parser.parse("1970-01-01T00:00:00.000Z")

    expected_events = [{
        "event_name": "event-type",
        "event_time": expected_datetime,
        "bucket_name": "bucket-name",
        "object_key": "bucket-name",
        "size": 1234,
        "versionId": "v2"
    }]

    assert parsed_events == expected_events

