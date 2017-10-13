from botocore.exceptions import ClientError


def _copy_image_asset(s3_client,
                      source_bucket_name,
                      source_key,
                      destination_bucket_name,
                      destination_key):
    s3_client.copy_object(
        CopySource={
            'Bucket': source_bucket_name,
            'Key': source_key
        },
        Bucket=destination_bucket_name,
        Key=destination_key)


def copy_asset_if_not_exists(s3_client, source_hash, destination_bucket_name, destination_key,
                             source_bucket_name, source_key):
    try:
        destination_head_response = s3_client.head_object(Bucket=destination_bucket_name, Key=destination_key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(
                f"Destination bucket has no image: copying from {source_bucket_name}/{source_key} to {destination_bucket_name}/{destination_key}")
            _copy_image_asset(s3_client, source_bucket_name, source_key, destination_bucket_name, destination_key)
            pass
        else:
            raise
    else:
        if not source_hash == destination_head_response['ETag']:
            print(
                f"Destination bucket has image with different hash: copying from {source_bucket_name}/{source_key} to {destination_bucket_name}/{destination_key}")
            _copy_image_asset(s3_client, source_bucket_name, source_key, destination_bucket_name, destination_key)

