#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Parse image records from a Miro export and push JSON derivatives to S3.

The output is a file stored in S3 which encodes Miro records as JSON objects,
one object per line.  This makes it easier for us to run one-off scripts
against an entire Miro record set, or find all the source data about a
particular record.

Usage:
  build_json_derivatives.py --bucket=<BUCKET> --src=<SRC> --dst=<DST>
  build_json_derivatives.py -h | --help

Options:
  -h --help                 Show this screen.
  --bucket=<BUCKET>         S3 bucket containing the Miro XML dumps.
  --src=<SRC>               Key of the Miro XML dump in the S3 bucket.
  --dst=<DST>               Key of the JSON derivatives in the S3 bucket.

"""

import json
import os
import tempfile

import boto3
import docopt

from utils import generate_images


def _wrap_image_data(collection, image_data):
    return {
        "collection": collection,
        "image_data": image_data
    }


def _build_collection_id(src_key):
    return src_key.split(".")[0].split("/")[-1]


def _fix_sierra_number(image):
    """
    Correct INNOPAC IDs on a handful of records where we know the data
    is incorrect.
    """
    miro_id = image["image_no_calc"]
    sierra_id = image["image_innopac_id"]

    if sierra_id == "150056628" and miro_id == "L0035213":
        image["image_innopac_id"] = "1500562x"
    elif sierra_id == "113183382" and miro_id in ("L0001138EA", "L0001138EB"):
        image["image_innopac_id"] = "1318338x"
    elif sierra_id == 'L 35411 \n\n15551040' and miro_id == 'L0035411':
        image['image_innopac_id'] = '15551040'

    print(image)


def main(bucket, src_key, dst_key, js_path="json"):
    print(f"Starting to process s3://{bucket}/{src_key}.")
    image_data = generate_images(bucket=bucket, key=src_key)

    collection = _build_collection_id(src_key)

    tmp_json = tempfile.mktemp()
    os.makedirs(os.path.dirname(tmp_json), exist_ok=True)

    s3 = boto3.client('s3')

    with open(tmp_json, 'w') as f:
        for count, img in enumerate(image_data, start=1):
            if "image_innopac_id" in img.keys():
                _fix_sierra_number(img)

            img_json_dump = json.dumps(
                _wrap_image_data(
                    collection=collection,
                    image_data=img
                ),

                # Adding the separators omits unneeded whitespace in the JSON,
                # giving us smaller files.
                separators=(',', ':'),

                # Using sort_keys gives a deterministic JSON blob
                sort_keys=True
            )

            image_id = img["image_no_calc"]

            json_object_key = f'{js_path}/{image_id}.json'

            byte_encoded_json_dump = img_json_dump.encode()

            s3.put_object(
                Bucket=bucket,
                Key=json_object_key,
                Body=byte_encoded_json_dump
            )

            print(f"Put image {image_id} ({count}) to s3://{bucket}/{json_object_key}")

            f.write(img_json_dump + '\n')

    s3.upload_file(
        Bucket=bucket,
        Key=dst_key,
        Filename=tmp_json
    )

    print(f"Put s3://{bucket}/{dst_key}. Done.")

    os.unlink(tmp_json)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    bucket = args['--bucket']
    src_key = args['--src']
    dst_key = args['--dst']

    main(bucket, src_key, dst_key)
