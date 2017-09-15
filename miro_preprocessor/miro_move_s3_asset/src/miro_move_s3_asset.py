import json
import os
import re

import boto3
from botocore.exceptions import ClientError

import sns_utils


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
        s3_client.head_object(Bucket=source_bucket_name, Key=key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for MiroId {miro_id}: skipping")
            pass
        else:
            raise
    else:
        destination_key = f"{shard}/{miro_id}.jpg"
        s3_client.copy(
            CopySource={
                'Bucket': source_bucket_name,
                'Key': key
            },
            Bucket=destination_bucket_name,
            Key=destination_key)
        sns_utils.publish_sns_message(
            sns_client,
            topic_arn,
            image_info
        )
