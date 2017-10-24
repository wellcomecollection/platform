import json
import os

import boto3

from wellcome_lambda_utils import sns_utils
from wellcome_lambda_utils import s3_utils

from miro_utils import MiroImage


def main(event, _):
    print(f"Received event:\n{event}")
    sns_client = boto3.client("sns")

    src_bucket = os.environ["S3_SOURCE_BUCKET"]
    dst_bucket = os.environ["S3_DESTINATION_BUCKET"]

    destination_prefix = os.environ["S3_DESTINATION_PREFIX"]
    topic_arn = os.environ["TOPIC_ARN"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    subject = event['Records'][0]['Sns']['Subject']
    miro_image = MiroImage(image_info)

    src_key = f"fullsize/{miro_image.image_path}.jpg"
    dst_key = f"{destination_prefix}{miro_image.image_path}.jpg"

    if s3_utils.is_object(src_bucket, src_key):
        s3_utils.copy_object(
            src_bucket=src_bucket,
            dst_bucket=dst_bucket,
            src_key=src_key,
            dst_key=dst_key,
            lazy=True
        )

        sns_utils.publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=image_info,
            subject=f'{subject}_derivative'
        )
