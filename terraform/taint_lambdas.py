#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Taint any Lambda functions whose files have changed since the last plan.

Our Lambda functions are delivered as ZIP files that are sent to AWS Lambda.
There's a ``source_code_hash`` on the ``aws_lambda_function`` Terraform
resource (https://www.terraform.io/docs/providers/aws/r/lambda_function.html),
but this only seems to trigger an update to the source hash.

"""

import glob
import hashlib
import json
import os
import subprocess


ROOT = os.path.relpath(subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip())

LAMBDAS = os.path.join(ROOT, 'lambdas')

HASHES = os.path.join(ROOT, '.lambda_hashes.json')


def read_old_hashes():
    """Read any cached hashes."""
    try:
        return json.load(open(HASHES))
    except Exception:
        return {}


def update_hashes():
    """Update the cached hashes."""
    result = {}
    for dirname in os.listdir(LAMBDAS):
        if not os.path.isdir(os.path.join(LAMBDAS, dirname)):
            continue
        files = glob.glob('%s/*.py' % os.path.join(LAMBDAS, dirname))
        new_hashes = {f: _md5(f) for f in files}
        result[dirname] = new_hashes
    json.dump(result, open(HASHES, 'w'))


def list_lambdas():
    """Return a list of Lambdas."""
    return [
        d
        for d in os.listdir(LAMBDAS)
        if os.path.isdir(os.path.join(LAMBDAS, d)) and d != 'common'
    ]


def _md5(fname):
    """Returns the MD5 checksum of a file."""
    # http://stackoverflow.com/a/3431838/1558022
    hash_md5 = hashlib.md5()
    with open(fname, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


def taint_lambda(lambda_name):
    """
    Taint a Lambda in Terraform in such a way that a new ZIP file will
    be uploaded on the new plan.
    """
    print('Tainting Lambda %s' % lambda_name)
    subprocess.Popen([
        'terraform', 'taint', '-module', 'lambda_%s' % lambda_name,
        'aws_lambda_function.lambda_function'
    ]).communicate()


def has_lambda_dir_changed(lambda_name, hashes):
    """
    Returns True/False if a Lambda directory has changed.
    """
    files = glob.glob('%s/*.py' % os.path.join(LAMBDAS, lambda_name))
    new_hashes = {f: _md5(f) for f in files}
    return hashes != new_hashes


if __name__ == '__main__':
    print('Checking to see if any Lambdas should be tainted...')
    old_hashes = read_old_hashes()

    # If there weren't any old hashes, taint everything
    if old_hashes == {}:
        print('No cached hashes found, tainting all Lambdas')
        for lambda_name in list_lambdas():
            taint_lambda(lambda_name)

    # If the common lib has changed, taint everything
    elif has_lambda_dir_changed('common', old_hashes.get('common', {})):
        print('Common lib has changed, tainting all Lambdas')
        for lambda_name in list_lambdas():
            taint_lambda(lambda_name)

    else:
        for lambda_name in list_lambdas():
            hashes_for_lambda = old_hashes.get(lambda_name, {})
            if has_lambda_dir_changed(lambda_name, hashes=hashes_for_lambda):
                taint_lambda(lambda_name)

    update_hashes()
