#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Build a deployment ZIP for a Lambda, and upload it to Amazon S3.

Usage:
  publish_lambda_zip.py <PATH> --bucket=<BUCKET> --key=<KEY>
  publish_lambda_zip.py -h | --help

Options:
  <PATH>                Path to the source code for the Lambda
  --bucket=<BUCKET>     Name of the Amazon S3 bucket
  --key=<KEY>           Key for the upload ZIP file

"""

import os
import shutil
import subprocess
import tempfile
import zipfile

import boto3
import docopt


ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

UTILS_LIB = os.path.join(ROOT, 'lambdas', 'common', 'utils')

ZIP_DIR = os.path.join(ROOT, '.lambda_zips')


def create_zip(src, dst):
    """
    Zip a source directory into a target directory.

    Based on https://stackoverflow.com/a/14569017/1558022
    """
    name = f'{dst}.zip'
    with zipfile.ZipFile(name, 'w', zipfile.ZIP_DEFLATED) as zf:
        abs_src = os.path.abspath(src)
        for dirname, subdirs, files in os.walk(src):
            for filename in files:
                absname = os.path.abspath(os.path.join(dirname, filename))
                arcname = absname[len(abs_src) + 1:]
                zf.write(absname, arcname)
    return name


def build_lambda_local(path, name):
    """
    Construct a Lambda ZIP bundle on the local disk.  Returns the path to
    the constructed ZIP bundle.

    :param path: Path to the Lambda source code.
    """
    print(f'*** Building Lambda ZIP for {name}')
    target = tempfile.mkdtemp()

    # Copy all the associated files to the Lambda directory
    for f in os.listdir(path):
        if not f.startswith(('test_', '.', 'requirements.txt')):
            shutil.copy(
                src=os.path.join(path, f),
                dst=os.path.join(target, os.path.basename(f))
            )

    # Copy the contents of the Lambda common lib
    shutil.copytree(UTILS_LIB, os.path.join(target, 'utils'))

    # Now install any additional pip dependencies.
    reqs_file = os.path.join(path, 'requirements.txt')
    if os.path.exists(reqs_file):
        print(f'*** Installing dependencies from requirements.txt')
        subprocess.check_call([
            'pip', 'install', '--requirement', reqs_file, '--target', target
        ])
    else:
        print(f'*** No requirements.txt found')

    print(f'*** Creating zip bundle for {name}')
    os.makedirs(ZIP_DIR, exist_ok=True)
    return create_zip(target, os.path.join(ZIP_DIR, name))


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    path = args['<PATH>']
    key = args['--key']
    bucket = args['--bucket']

    client = boto3.client('s3')
    name = os.path.basename(key) + '.zip'
    filename = build_lambda_local(path=path, name=name)

    print(f'*** Uploading {filename} to S3')
    client.upload_file(
        Bucket=bucket,
        Filename=filename,
        Key=key
    )
