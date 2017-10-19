from functools import partial
import json
import os
import re

import boto3

from wellcome_lambda_utils.miro_utils import MiroImage
from wellcome_lambda_utils.s3_utils import S3_Identifier
from wellcome_lambda_utils import s3_utils


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


def main(event, _):
    print(f"Received event:\n{event}")
    s3_client = boto3.client("s3")
    source_bucket_name = os.environ["S3_SOURCE_BUCKET"]
    destination_bucket_name = os.environ["S3_DESTINATION_BUCKET"]
    destination_prefix = os.environ["S3_DESTINATION_PREFIX"]

    image_info = json.loads(event['Records'][0]['Sns']['Message'])
    miro_image = MiroImage(image_info)
    key_prefix = f"Wellcome_Images_Archive/{miro_image.collection} Images/{miro_image.image_path}"
    print(key_prefix)
    destination_key = f"{destination_prefix}{miro_image.image_path}.jp2"
    list_response = s3_client.list_objects_v2(Bucket=source_bucket_name, Prefix=key_prefix)

    if 'Contents' in list_response.keys():
        keys = [x['Key'] for x in list_response['Contents']]
        key = _select_best_key(keys, key_prefix)
        source_identifier = S3_Identifier(source_bucket_name, key)
        destination_identifier = S3_Identifier(destination_bucket_name, destination_key)
        s3_utils.exec_if_key_exists(s3_client, source_identifier=source_identifier,
                                    function=partial(s3_utils.copy_asset_if_not_exists,
                                                     destination_identifier=destination_identifier))
