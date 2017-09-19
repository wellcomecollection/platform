import json
import os
import re

import boto3
from botocore.exceptions import ClientError

import sns_utils


def copy_and_forward_message(s3_client,
                             sns_client,
                             image_info,
                             source_bucket_name,
                             source_key,
                             source_etag,
                             destination_bucket_name,
                             destination_key,
                             topic_arn):
    try:
        s3_client.copy_object(
            CopySourceIfNoneMatch=source_etag,
            CopySource={
                'Bucket': source_bucket_name,
                'Key': source_key
            },
            Bucket=destination_bucket_name,
            Key=destination_key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '412':
            print(f"Image {destination_bucket_name}/{destination_key} already exists with same ETag: skipping copy")
            pass
        else:
            raise

    sns_utils.publish_sns_message(
            sns_client,
            topic_arn,
            image_info)


def get_content_md5(head_response):
    print(head_response)
    return head_response['ResponseMetadata']['HTTPHeaders']['Content-MD5']


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
        copy_and_forward_message(s3_client,
                                 sns_client,
                                 image_info,
                                 source_bucket_name,
                                 key,
                                 source_head_response['ETag'],
                                 destination_bucket_name,
                                 destination_key,
                                 topic_arn)
