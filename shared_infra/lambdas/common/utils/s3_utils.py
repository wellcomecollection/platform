import collections

from botocore.exceptions import ClientError

S3_Identifier = collections.namedtuple(
    'S3_Identifier',
    'bucket_name key'
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


def copy_asset_if_not_exists(s3_client, source_head_response, source_identifier, destination_identifier):
    try:
        destination_head_response = s3_client.head_object(Bucket=destination_identifier.bucket_name,
                                                          Key=destination_identifier.key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(
                f"Destination bucket has no image")
            _copy_image_asset(s3_client, source_identifier, destination_identifier)
            pass
        else:
            raise
    else:
        if not source_head_response['ETag'] == destination_head_response['ETag']:
            print(
                f"Destination bucket has image with different hash")
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