import json
import os

import boto3
from botocore.exceptions import ClientError

from miro_utils import MiroImage
import sns_utils
import s3_utils


def copy_and_forward_message(s3_client,
                             sns_client,
                             image_info,
                             source_bucket_name,
                             source_key,
                             destination_bucket_name,
                             destination_key,
                             source_hash,
                             topic_arn):
    s3_utils.copy_asset_if_not_exists(s3_client, source_hash, destination_bucket_name, destination_key,
                             source_bucket_name, source_key)
    sns_utils.publish_sns_message(
        sns_client,
        topic_arn,
        image_info)


def main(event, _):
    print(f"Received event:\n{event}")
    sns_client = boto3.client("sns")
    s3_client = boto3.client("s3")
    source_bucket_name = os.environ["S3_SOURCE_BUCKET"]
    destination_bucket_name = os.environ["S3_DESTINATION_BUCKET"]
    topic_arn = os.environ["TOPIC_ARN"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    miro_image = MiroImage(image_info)
    source_key = f"fullsize/{miro_image.image_path}"

    try:
        source_head_response = s3_client.head_object(Bucket=source_bucket_name, Key=source_key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for MiroId {miro_image.miro_id}: skipping")
            pass
        else:
            raise
    else:
        destination_key = miro_image.image_path
        copy_and_forward_message(s3_client,
                                 sns_client,
                                 image_info,
                                 source_bucket_name,
                                 source_key,
                                 destination_bucket_name,
                                 destination_key,
                                 source_head_response['ETag'],
                                 topic_arn)
