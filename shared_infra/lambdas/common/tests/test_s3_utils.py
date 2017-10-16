import os
from unittest.mock import patch

from moto import mock_s3
import boto3
import pytest

from utils import s3_utils
from utils.s3_utils import S3_Identifier


@pytest.fixture
def create_source_and_destination_buckets():
    mock_s3().start()
    s3_client = boto3.client("s3")
    source_bucket_name = "test-miro-images-sync"
    s3_client.create_bucket(Bucket=source_bucket_name, ACL="private")
    destination_bucket_name = "test-miro-images-public"
    s3_client.create_bucket(Bucket=destination_bucket_name, ACL="public-read")
    yield source_bucket_name, destination_bucket_name
    mock_s3().stop()


def test_should_not_copy_asset_if_already_exists_with_same_checksum(create_source_and_destination_buckets):
    s3_client = boto3.client("s3")
    source_bucket_name, destination_bucket_name = create_source_and_destination_buckets
    image_body = b'baba'
    miro_id = "A1234567"

    source_key = f"fullsize/A0000000/{miro_id}.jpg"
    s3_client.put_object(
        Bucket=source_bucket_name,
        ACL='private',
        Body=image_body, Key=source_key)

    destination_key = f"A0000000/{miro_id}.jpg"
    s3_client.put_object(
        Bucket=destination_bucket_name,
        ACL='public-read',
        Body=image_body, Key=destination_key)

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
    }

    source_head_response = s3_client.head_object(Bucket=source_bucket_name, Key=source_key)
    source_identifier=S3_Identifier(source_bucket_name, source_key)
    destination_identifier=S3_Identifier(destination_bucket_name,destination_key)
    with patch("utils.s3_utils._copy_image_asset") as mock_copy_function:
        s3_utils.copy_asset_if_not_exists(s3_client, source_head_response, source_identifier=source_identifier, destination_identifier=destination_identifier)
        assert not mock_copy_function.called


def test_should_replace_asset_if_already_exists_with_different_content(
        create_source_and_destination_buckets):
    s3_client = boto3.client("s3")
    source_bucket_name, destination_bucket_name = create_source_and_destination_buckets
    image_body = b'baba'
    destination_prefix = "library"
    miro_id = "A1234567"
    s3_client.put_bucket_versioning(Bucket=destination_bucket_name,
                                    VersioningConfiguration={'Status': 'Enabled'})
    source_key = f"Wellcome_Images_Archive/A Images/A0000000/{miro_id}.jpg"
    s3_client.put_object(
        Bucket=source_bucket_name,
        ACL='private',
        Body=image_body, Key=source_key)

    destination_key = f"A0000000/{miro_id}.jpg"
    s3_client.put_object(
        Bucket=destination_bucket_name,
        ACL='public-read',
        Body=b"adjhgkjae", Key=destination_key)

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "S3_DESTINATION_PREFIX": destination_prefix,
    }
    source_head_response = s3_client.head_object(Bucket=source_bucket_name, Key=source_key)
    source_identifier=S3_Identifier(source_bucket_name, source_key)
    destination_identifier=S3_Identifier(destination_bucket_name,destination_key)
    s3_utils.copy_asset_if_not_exists(s3_client, source_head_response, source_identifier=source_identifier, destination_identifier=destination_identifier)

    s3_response = s3_client.get_object(Bucket=destination_bucket_name, Key=destination_key)
    assert s3_response['Body'].read() == image_body