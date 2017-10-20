# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import collections
import os
from pprint import pprint

import boto3
import dateutil.parser
import requests


API_URL = 'https://wellcome.tandemvault.com/api/v1'


Miro = collections.namedtuple('Miro', ['upload_set_id', 'collection_id'])

miro_collections = {
    'A': Miro(47806, 111597),
    'AS': Miro(47807, 111598),
    'B': Miro(47808, 111600),
    'D': Miro(47809, 111601),
    'F': Miro(47810, 111603),
    'FP': Miro(47811, 111604),
    'L': Miro(47812, 111605),
    'M': Miro(47813, 111606),
    'N:': Miro(47814, 111607),
    'S': Miro(47815, 111608),
    'V': Miro(47816, 111609),
    'W': Miro(47817, 1116011),
}


def seconds_since_epoch(dt_string):
    return dateutil.parser.parse(dt_string).epoch


class TandemVaultAPI(object):

    def __init__(self, api_key, sess=None):
        self.api_key = api_key
        self.sess = sess or requests.Session()

    def create_upload_set(self, title):
        """
        Create an upload set in TV, and return the upload set identifier.

        Note: Upload set titles are not unique in TV, so calling this method
        more than once will create multiple upload sets.
        """
        resp = self.sess.post(
            f'{API_URL}/upload_sets',
            params={
                'api_key': self.api_key,
                'upload_set[title]': title,
            }
        )
        resp.raise_for_status()
        return resp.json()['id']

    def create_collection(self, title):
        # TODO: I could only get this to create lightboxes in the parent
        # account, not collections.
        resp = self.sess.post(
            f'{API_URL}/collections',
            params={
                'api_key': self.api_key,
                # 'collection': 'foo',
                'collection[name]': title,
            }
        )
        resp.raise_for_status()
        pprint(resp.json())

    def upload_image_to_tv(self, src_bucket, src_key):
        s3 = boto3.client('s3')
        body = s3.get_object(Bucket=src_bucket, Key=src_key)['Body']

        # Retrieve permissions for uploading to S3
        resp = self.sess.post(
            f'{API_URL}/assets/get_upload_signature',
            params={
                'api_key': self.api_key,
                'filename': os.path.basename(src_key),
                'content_type': 'image/jpeg',
            }
        )
        resp.raise_for_status()
        upload_data = resp.json()
        pprint(upload_data)

        # Use the response from the previous request to upload the
        # asset to S3
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
        resp.raise_for_status()
        s3_data = resp.text
        pprint(s3_data)

        # Now add the asset to Tandem Vault
        resp = self.sess.post(
            f'{API_URL}/assets',
            params={
                'api_key': self.api_key,
                'upload_set_id': miro_collections['B'].upload_set_id,
                'asset[filename]': os.path.basename(src_key),
            }
        )
        resp.raise_for_status()
        asset_data = resp.json()
        pprint(asset_data)

tv = TandemVaultAPI(API_KEY)
tv.upload_image_to_tv('miro-images-sync', 'fullsize/B0008000/B0008752.jpg')