#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Usage:
  run.py --src-bucket=<BUCKET> [--dst-bucket=<BUCKET>] --src-key=<SRC> --dst-key=<DST> [--delete-original] --location=<LOCATION> --creator=<CREATOR> --keywords=<KEYWORDS> --intended-usage=<USAGE> --copyright=<COPYRIGHT>
  run.py -h | --help

Options:
  -h --help              Show this message.
  --src-bucket=<BUCKET>  Source bucket for the TIFF image in S3.
  --dst-bucket=<BUCKET>  Destination bucket (if absent, will use the source).
  --src-key=<SRC>        Source key.
  --dst-key=<DST>        Destination key.
  --delete-original      Delete the original source key.

"""

import os
import subprocess
import tempfile

import boto3
import docopt


args = docopt.docopt(__doc__)

src_bucket = args['--src-bucket']
dst_bucket = args['--dst-bucket'] or src_bucket
src_key = args['--src-key']
dst_key = args['--dst-key']
delete_original = bool(args['--delete-original'])

# Metadata to be embedded
location = args['--location']
creator = args['--creator']
keywords = args['--keywords']
usage = args['--intended-usage']
copyright = args['--copyright']

client = boto3.client('s3')
_, tmp_fp = tempfile.mkstemp()

try:
    client.download_file(Bucket=src_bucket, Key=src_key, Filename=tmp_fp)

    # Magic happens here...

    client.upload_file(Bucket=dst_bucket, Key=dst_key, Filename=tmp_fp)

    if delete_original:
        client.delete_object(Bucket=src_bucket, Key=src_key)
finally:
    os.unlink(tmp_fp)
