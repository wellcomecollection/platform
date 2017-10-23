# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import logging
import os
import re

import attr
import boto3
import daiquiri
import dateutil.parser
import requests

daiquiri.setup(level=logging.INFO)

logger = daiquiri.getLogger(__name__)

API_URL = 'https://wellcome.tandemvault.com/api/v1'


@attr.s
class MiroCollection:
    """
    Represents an old Miro collection, as stored in Tandem Vault.
    """
    upload_set_id = attr.ib()
    collection_id = attr.ib()


# These collections were created partially using the code below, partially
# by hand.  Because they're a fixed set, we just hard-code the details here,
# rather than trying to derive them programatically from the API.
miro_collections = {
    'A': MiroCollection(47806, 111597),  # noqa
    'AS': MiroCollection(47807, 111598),  # noqa
    'B': MiroCollection(47808, 111600),  # noqa
    'D': MiroCollection(47809, 111601),  # noqa
    'F': MiroCollection(47810, 111603),  # noqa
    'FP': MiroCollection(47811, 111604),  # noqa
    'L': MiroCollection(47812, 111605),  # noqa
    'M': MiroCollection(47813, 111606),  # noqa
    'N:': MiroCollection(47814, 111607),  # noqa
    'S': MiroCollection(47815, 111608),  # noqa
    'V': MiroCollection(47816, 111609),  # noqa
    'W': MiroCollection(47817, 1116011),  # noqa
}


def seconds_since_epoch(dt_string):
    return dateutil.parser.parse(dt_string).epoch


def miro_prefix(s3_key):
    """
    Given an S3 key (e.g. 'B0007000/B00070001.jpg'), return the Miro prefix.
    """
    filename = os.path.basename(s3_key)
    return re.search(r'^[A-Z]+', filename).group(0)


class TandemVaultAPI(object):
    def __init__(self, api_key, sess=None):
        self.api_key = api_key
        self.sess = sess or requests.Session()

        # This causes the Session to call ``raise_for_status()`` as soon
        # as a response is received, so any failing request will throw an
        # exception immediately.
        def raise_error(resp, *args, **kwargs):
            resp.raise_for_status()

        self.sess.hooks['response'].append(raise_error)

    def upload_image_to_tv(self, s, src_key):
        """
        Given an image in one of our S3 buckets, create an asset for the
        image in TV by uploading it.  Return the asset metadata.
        """
        s3 = boto3.client('s3')
        # TODO: Add reference to bucket
        src_bucket = "not_a_real_bucket"

        logger.info('Uploading image from S3: bucket=%s, key=%s', src_bucket, src_key)
        body = s3.get_object(Bucket=src_bucket, Key=src_key)['Body']

        # Retrieve an upload signature for uploading files to S3.
        # https://tandemvault.com/docs/api/v1/assets/get_upload_signature.html
        resp = self.sess.post(
            f'{API_URL}/assets/get_upload_signature',
            params={
                'api_key': self.api_key,
                'filename': os.path.basename(src_key),
                'content_type': 'image/jpeg',
            }
        )
        logger.debug('Response from POST /assets/get_upload_signature: %s', resp.text)
        upload_data = resp.json()

        # Use the upload signature from the previous request to upload the
        # file to S3.  This is a standard S3 POST upload:
        # https://aws.amazon.com/articles/browser-uploads-to-s3-using-html-post-forms/
        resp = self.sess.post(
            'http://uploads.tandemstock.com.s3.amazonaws.com/',
            files={
                'Content-Type': 'image/jpeg',
                'Content-Disposition': 'attachment',
                'AWSAccessKeyId': upload_data['access_key_id'],
                'Signature': upload_data['signature'],
                'policy': upload_data['policy'],
                'acl': 'private',
                'key': upload_data['key'],
                'filename': os.path.basename(src_key),
                'success_action_status': '201',
                'file': body,
            }
        )
        logger.debug('Response from POST to uploads.tandemstock.s3: %s', resp.text)

        # Now create an asset from the S3 upload location.
        # https://tandemvault.com/docs/api/v1/assets/create.html
        prefix = miro_prefix(src_key)
        resp = self.sess.post(
            f'{API_URL}/assets',
            params={
                'api_key': self.api_key,
                'upload_set_id': miro_collections[prefix].upload_set_id,
                'asset[filename]': os.path.basename(src_key),
            }
        )
        logger.debug('Response from POST /assets: %s', resp.text)
        asset_data = resp.json()
        return asset_data

    def add_image_to_collection(self, asset_data):
        """
        Given information about an asset, add it to the Tandem Vault collection
        for the appropriate Miro prefix.
        """
        prefix = miro_prefix(asset_data['filename'])
        collection_id = miro_collections[prefix].collection_id

        # Add an asset to a collection.
        # https://tandemvault.com/docs/api/v1/collections/add_assets.html
        resp = self.sess.put(
            f'{API_URL}/collections/{collection_id}/add_assets',
            params={
                'api_key': self.api_key,
                'asset_ids': asset_data['id']
            }
        )
        logger.debug('Response from PUT /add_assets: %s', resp.text)
