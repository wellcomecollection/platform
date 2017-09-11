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


def main(bucket, src_key, dst_key):
    image_data = generate_images(bucket=bucket, key=src_key)

    tmp_json = tempfile.mktemp()
    os.makedirs(os.path.dirname(tmp_json), exist_ok=True)
    with open(tmp_json, 'w') as f:
        for img in image_data:

            # Adding the separators omits unneeded whitespace in the JSON,
            # giving us smaller files.
            f.write(json.dumps(img, separators=(',', ':')) + '\n')

    s3 = boto3.client('s3')
    s3.upload_file(
        Bucket=bucket,
        Key=dst_key,
        Filename=tmp_json
    )

    os.unlink(tmp_json)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    bucket = args['--bucket']
    src_key = args['--src']
    dst_key = args['--dst']

    main(bucket, src_key, dst_key)