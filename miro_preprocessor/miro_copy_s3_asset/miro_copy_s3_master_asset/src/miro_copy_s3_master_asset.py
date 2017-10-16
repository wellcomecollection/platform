from functools import partial
import json
import os

import boto3

from miro_utils import MiroImage
from s3_utils import S3_Identifier
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
    destination_key = f"{destination_prefix}/{miro_image.image_path}.jp2"
    source_identifier = S3_Identifier(source_bucket_name, key)
    destination_identifier = S3_Identifier(destination_bucket_name, destination_key)
    s3_utils.exec_if_key_exists(s3_client, source_identifier=source_identifier,
                                function=partial(s3_utils.copy_asset_if_not_exists, destination_identifier=destination_identifier))
