import collections

import boto3
from botocore.exceptions import ClientError

S3_Identifier = collections.namedtuple(
    'S3_Identifier',
    'bucket_name key'
)


def is_object(bucket, key):
    """
    Checks if an object exists in S3.  Returns True/False.
    """
    client = boto3.client('s3')
    try:
        client.head_object(Bucket=bucket, Key=key)
    except ClientError as err:
        if err.response['Error']['Code'] == '404':
            return False
        else:
            raise
    else:
        return True


def copy_object(src_bucket, src_key, dst_bucket, dst_key, lazy=False):
    """
    Copy an object from one S3 bucket to another.

    If you pass ``lazy=True``, the object will only be copied if the
    destination object either:
     * does not exist, or
     * does exist, but has the same ETag as the source object

    """
    client = boto3.client('s3')
    if not is_object(bucket=src_bucket, key=src_key):
        raise ValueError(
            f'Tried to copy missing object ({src_bucket}, {src_key})'
        )

    def should_copy():
        if not lazy:
            return True

        if not is_object(bucket=dst_bucket, key=dst_key):
            return True

        src_resp = client.head_object(Bucket=src_bucket, Key=src_key)
        dst_resp = client.head_object(Bucket=dst_bucket, Key=dst_key)
        return src_resp['ETag'] == dst_resp['ETag']

    if should_copy():
        return client.copy_object(
            CopySource={'Bucket': src_bucket, 'Key': src_key},
            Bucket=dst_bucket,
            Key=dst_key
        )


def _copy_image_asset(s3_client, source_identifier, destination_identifier):
    print(f"copying from {source_identifier.bucket_name}/{source_identifier.key} to {destination_identifier.bucket_name}/{destination_identifier.key}")
    s3_client.copy_object(
        CopySource={
            'Bucket': source_identifier.bucket_name,
            'Key': source_identifier.key
        },
        Bucket=destination_identifier.bucket_name,
        Key=destination_identifier.key)


def _is_same_etag(destination_head_response, source_head_response):
    return source_head_response['ETag'] == destination_head_response['ETag']


def copy_asset_if_not_exists(s3_client, source_head_response, source_identifier, destination_identifier):
    try:
        destination_head_response = s3_client.head_object(Bucket=destination_identifier.bucket_name,
                                                          Key=destination_identifier.key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"Destination bucket has no image")
            pass
        else:
            raise
    if not destination_head_response or not _is_same_etag(destination_head_response, source_head_response):
        _copy_image_asset(s3_client, source_identifier, destination_identifier)


def exec_if_key_exists(s3_client, source_identifier, function):
    try:
        source_head_response = s3_client.head_object(Bucket=source_identifier.bucket_name, Key=source_identifier.key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for key {source_identifier.key}: skipping")
            pass
        else:
            raise
    else:
        function(s3_client=s3_client, source_identifier=source_identifier, source_head_response=source_head_response)
