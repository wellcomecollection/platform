# -*- encoding: utf-8 -*-

import dateutil.parser

import boto3
from botocore.exceptions import ClientError
from moto import mock_s3
import pytest

from src.wellcome_lambda_utils import s3_utils


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


class TestIsObject(object):
    @mock_s3
    def test_detects_existing_object(self):
        client = boto3.client('s3')

        # Create a bucket and an object
        client.create_bucket(Bucket='bukkit')

        # First check we don't think the object exists
        assert not s3_utils.is_object(bucket='bukkit', key='myfile.txt')
        client.put_object(Bucket='bukkit', Key='myfile.txt', Body=b'hello world')

        # Now check we can detect its existence
        assert s3_utils.is_object(bucket='bukkit', key='myfile.txt')

    @mock_s3
    def test_does_not_detect_missing_object(self):
        client = boto3.client('s3')
        client.create_bucket(Bucket='bukkit')
        assert not s3_utils.is_object(bucket='bukkit', key='doesnotexist.py')

    @mock_s3
    def test_other_errors_are_raised(self):
        with pytest.raises(ClientError):
            s3_utils.is_object(bucket='notabukkit', key='forbidden.txt')


class TestCopyObject(object):
    @mock_s3
    def test_throws_error_if_src_does_not_exist(self):
        client = boto3.client('s3')
        client.create_bucket(Bucket='bukkit')

        with pytest.raises(ValueError) as err:
            s3_utils.copy_object(
                src_bucket='bukkit', src_key='doesnotexist.txt',
                dst_bucket='bukkit2', dst_key='doesnotexist.txt'
            )
        assert 'Tried to copy missing object' in err.value.args[0]

    @mock_s3
    def test_throws_error_if_dst_bucket_does_not_exist(self):
        client = boto3.client('s3')
        client.create_bucket(Bucket='bukkit')
        client.put_object(Bucket='bukkit', Key='f.txt', Body=b'hello world')

        with pytest.raises(ClientError):
            s3_utils.copy_object(
                src_bucket='bukkit', src_key='f.txt',
                dst_bucket='doesnotexistbukkit', dst_key='f.txt'
            )

    @mock_s3
    def test_copies_file_if_dst_key_does_not_exist(self):
        client = boto3.client('s3')
        client.create_bucket(Bucket='bukkit')
        client.create_bucket(Bucket='newbukkit')
        client.put_object(Bucket='bukkit', Key='f.txt', Body=b'hello world')

        s3_utils.copy_object(
            src_bucket='bukkit', src_key='f.txt',
            dst_bucket='newbukkit', dst_key='f.txt'
        )

        assert s3_utils.is_object(bucket='newbukkit', key='f.txt')

    @pytest.mark.skip(reason="""
        Blocked on https://github.com/spulec/moto/issues/1271 as versioning
        in moto doesn't work
    """)
    @mock_s3
    @pytest.mark.parametrize('lazy, expected_version', [
        (False, '1'),
        (True, '0'),
    ])
    def test_copies_file_if_dst_key_exists_but_not_lazy(
            self, lazy, expected_version
    ):
        client = boto3.client('s3')

        # First create the same file in both buckets.  We enable versioning
        # so we can check when files change.
        for b in ['bukkit', 'newbukkit']:
            client.create_bucket(Bucket=b)
            client.put_bucket_versioning(
                Bucket=b,
                VersioningConfiguration={'Status': 'Enabled'}
            )
            client.put_object(Bucket=b, Key='f.txt', Body=b'hello world')

        resp = client.get_object(Bucket='newbukkit', Key='f.txt')
        assert resp['VersionId'] == '0'

        for _ in range(3):
            s3_utils.copy_object(
                src_bucket='bukkit', src_key='f.txt',
                dst_bucket='newbukkit', dst_key='f.txt'
            )

        resp = client.get_object(Bucket='newbukkit', Key='f.txt')
        assert resp['VersionId'] == expected_version


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


@mock_s3
def test_write_dicts_to_s3():
    client = boto3.client('s3')
    client.create_bucket(Bucket='bukkit')

    s3_utils.write_dicts_to_s3(
        bucket='bukkit', key='dicts.txt',
        dicts=[{'a': 1, 'b': 2}, {'c': 3, 'd': 4}]
    )

    assert s3_utils.is_object(bucket='bukkit', key='dicts.txt')
    body = client.get_object(Bucket='bukkit', Key='dicts.txt')['Body'].read()
    assert body == b'{"a":1,"b":2}\n{"c":3,"d":4}'
