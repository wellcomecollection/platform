from functools import partial
import json
import os

import boto3

from miro_utils import MiroImage
import sns_utils
from s3_utils import S3_Identifier
import s3_utils


def copy_and_forward_message(s3_client,
                             sns_client,
                             image_info,
                             topic_arn,
                             source_identifier,
                             destination_identifier,
                             source_head_response):
    s3_utils.copy_asset_if_not_exists(s3_client=s3_client,
                                      source_head_response=source_head_response,
                                      source_identifier=source_identifier,
                                      destination_identifier=destination_identifier)
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
    source_key = f"fullsize/{miro_image.image_path}.jpg"
    destination_key = f"{miro_image.image_path}.jpg"
    source_identifier = S3_Identifier(source_bucket_name, source_key)
    destination_identifier = S3_Identifier(destination_bucket_name, destination_key)
    s3_utils.exec_if_key_exists(s3_client, source_identifier=source_identifier,
                            function=partial(
                                copy_and_forward_message,
                                sns_client=sns_client,
                                image_info=image_info,
                                topic_arn=topic_arn,
                                destination_identifier=destination_identifier))
