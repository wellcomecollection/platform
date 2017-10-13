import json
import os

import boto3
from botocore.exceptions import ClientError

from miro_utils import MiroImage
import s3_utils


def main(event, _):
    print(f"Received event:\n{event}")
    s3_client = boto3.client("s3")
    source_bucket_name = os.environ["S3_SOURCE_BUCKET"]
    destination_bucket_name = os.environ["S3_DESTINATION_BUCKET"]
    destination_prefix = os.environ["S3_DESTINATION_PREFIX"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    miro_image = MiroImage(image_info)
    key = f"Wellcome_Images_Archive/{miro_image.collection} Images/{miro_image.image_path}.jp2"
    print(key)
    try:
        source_head_response = s3_client.head_object(Bucket=source_bucket_name, Key=key)
    except ClientError as client_error:
        if client_error.response['Error']['Code'] == '404':
            print(f"No image found for MiroId {miro_image.miro_id}: skipping")
            pass
        else:
            raise
    else:
        destination_key = f"{destination_prefix}/{miro_image.image_path}.jp2"
        s3_utils.copy_asset_if_not_exists(s3_client, source_head_response['ETag'], destination_bucket_name, destination_key,
                                 source_bucket_name, key)