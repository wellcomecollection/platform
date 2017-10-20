import json
import os
import re

import boto3

from wellcome_lambda_utils.miro_utils import MiroImage
from wellcome_lambda_utils import s3_utils
from wellcome_lambda_utils import sns_utils


class MiroKeyIdMismatchException(Exception):
    pass


def _find_key_matching_regex(keys, regex):
    for key in keys:
        if re.match(regex, key) is not None:
            return key
    return None


def _find_exact_match_before_hyphen(keys, prefix):
    return _find_key_matching_regex(keys, prefix + r"-.*((\.jp2)|(\.JP2))")


def _find_exact_match(keys, prefix):
    return _find_key_matching_regex(keys, prefix + r"((\.jp2)|(\.JP2))")


def _select_best_key(keys, prefix):
    exact_match = _find_exact_match(keys, prefix)
    if exact_match is not None:
        return exact_match
    matching_key = _find_exact_match_before_hyphen(keys, prefix)
    if matching_key is not None:
        return matching_key
    else:
        raise MiroKeyIdMismatchException(f"Unable to match prefix {prefix} with keys {keys}")


def _generate_key_prefix(miro_image):
    key_prefix = f"Wellcome_Images_Archive/{miro_image.collection} Images/{miro_image.image_path}"
    print(key_prefix)

    return key_prefix


def _list_matching_image_keys(s3_client, src_bucket, key_prefix):
    list_response = s3_client.list_objects_v2(Bucket=src_bucket, Prefix=key_prefix)

    if 'Contents' in list_response.keys():
        keys = [x['Key'] for x in list_response['Contents']]
        return keys

    return None


def main(event, _):
    print(f"Received event:\n{event}")

    s3_client = boto3.client("s3")
    sns_client = boto3.client("sns")

    src_bucket = os.environ["S3_SOURCE_BUCKET"]
    dst_bucket = os.environ["S3_DESTINATION_BUCKET"]
    destination_prefix = os.environ["S3_DESTINATION_PREFIX"]
    topic_arn = os.environ["TOPIC_ARN"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    subject = event['Records'][0]['Sns']['Subject']

    miro_image = MiroImage(image_info)

    key_prefix = _generate_key_prefix(miro_image)

    keys = _list_matching_image_keys(s3_client, src_bucket, key_prefix)

    if keys is not None:
        src_key = _select_best_key(keys, key_prefix)
        dst_key = f"{destination_prefix}{miro_image.image_path}.jp2"

        if s3_utils.is_object(src_bucket, src_key):
            s3_utils.copy_object(
                src_bucket=src_bucket,
                dst_bucket=dst_bucket,
                src_key=src_key,
                dst_key=dst_key
            )

            sns_utils.publish_sns_message(
                sns_client=sns_client,
                topic_arn=topic_arn,
                message=image_info,
                subject=f'{subject}_master'
            )
