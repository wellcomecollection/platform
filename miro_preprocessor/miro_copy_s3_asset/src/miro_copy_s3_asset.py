import json
import os
import re

import boto3
from botocore.exceptions import ClientError

import sns_utils


def get_content_md5(source_head_response):
    return source_head_response['ResponseMetadata']['HTTPHeaders']['Content-MD5']


def copy_image_asset(s3_client, source_bucket_name, source_key, destination_bucket_name, destination_key):
    s3_client.copy_object(
        CopySource={
            'Bucket': source_bucket_name,
            'Key': source_key
        },
        Bucket=destination_bucket_name,
        Key=destination_key)


def main(event, _):
    sns_client = boto3.client("sns")
    s3_client = boto3.client("s3")
    source_bucket_name = os.environ["S3_SOURCE_BUCKET"]
    destination_bucket_name = os.environ["S3_DESTINATION_BUCKET"]
    topic_arn = os.environ["TOPIC_ARN"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    image_data = image_info['image_data']
    miro_id = image_data['image_no_calc']
    result = re.match(r"(?P<shard>[A-Z]+[0-9]{4})", miro_id)
    shard = f"{result.group('shard')}000"
    key = f"fullsize/{shard}/{miro_id}.jpg"

    try:
        source_head_response = s3_client.head_object(Bucket=source_bucket_name, Key=key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for MiroId {miro_id}: skipping")
            pass
        else:
            raise
    else:
        destination_key = f"{shard}/{miro_id}.jpg"
        try:
            destination_head_response = s3_client.head_object(Bucket=destination_bucket_name, Key=destination_key)
        except ClientError as client_error:
            if client_error.response['Error']['Code'] == '404':
                print(f"Destination bucket has no image: copying from {source_bucket_name}/{key} to {destination_bucket_name}/{destination_key}")
                copy_image_asset(s3_client, source_bucket_name, key, destination_bucket_name, destination_key)
                pass
            else:
                raise
        else:
            if not get_content_md5(source_head_response) == get_content_md5(destination_head_response):
                print(f"Destination bucket has image with different hash: copying from {source_bucket_name}/{key} to {destination_bucket_name}/{destination_key}")
                copy_image_asset(s3_client, source_bucket_name, key, destination_bucket_name, destination_key)

        sns_utils.publish_sns_message(
            sns_client,
            topic_arn,
            image_info)
