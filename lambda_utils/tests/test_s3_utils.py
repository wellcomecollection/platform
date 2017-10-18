# -*- encoding: utf-8 -*-

import boto3
from botocore.exceptions import ClientError
from moto import mock_s3
import pytest

from src.wellcome_lambda_utils import s3_utils


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
        client = boto3.client('s3')
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
