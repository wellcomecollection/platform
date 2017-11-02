# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import logging
import os
import daiquiri
import requests


daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


class TandemVaultAPI(object):
    def __init__(self, api_url, api_key, sess=None):
        self.api_key = api_key
        self.api_url = api_url
        self.sess = sess or requests.Session()

        # This causes the Session to call ``raise_for_status()`` as soon
        # as a response is received, so any failing request will throw an
        # exception immediately.
        def raise_error(resp, *args, **kwargs):
            resp.raise_for_status()

        self.sess.hooks['response'].append(raise_error)

    def upload_image_to_tv(self, image, filename, upload_set_id):
        """
        Given an image create an asset for the
        image in TV by uploading it.  Return the asset metadata.
        """

        # Retrieve an upload signature for uploading files to S3.
        # https://tandemvault.com/docs/api/v1/assets/get_upload_signature.html
        resp = self.sess.post(
            f'{self.api_url}/assets/get_upload_signature',
            params={
                'api_key': self.api_key,
                'filename': os.path.basename(filename),
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
                'filename': os.path.basename(filename),
                'success_action_status': '201',
                'file': image,
            }
        )
        logger.debug('Response from POST to uploads.tandemstock.s3: %s', resp.text)

        # Now create an asset from the S3 upload location.
        # https://tandemvault.com/docs/api/v1/assets/create.html
        resp = self.sess.post(
            f'{self.api_url}/assets',
            params={
                'api_key': self.api_key,
                'upload_set_id': upload_set_id,
                'asset[filename]': os.path.basename(filename),
            }
        )
        logger.debug('Response from POST /assets: %s', resp.text)
        asset_data = resp.json()
        return asset_data

    def add_image_metadata(self, asset_id, metadata):
        resp = self.sess.put(
            f'{self.api_url}/assets/{asset_id}',
            params={
                'api_key': self.api_key,
                'id': asset_id,
                'asset[description]': metadata['caption'],
                'asset[photographer]': metadata['creator'],
                'asset[copyright]': metadata['copyright'],
                'asset[notes]': metadata['notes'],
                'asset[usage_terms]': metadata['usage'],
                'status': 'accepted',
            }
        )

        logger.debug(f'Response from PUT /assets/{asset_id}: %s', resp.text)

    def add_image_tags(self, asset_id, tags):
        resp = self.sess.put(
            f'{self.api_url}/assets/{asset_id}/add_tags',
            params={
                'api_key': self.api_key,
                'id': asset_id,
                'tags[]': tags,
            }
        )

        logger.debug(f'Response from PUT /assets/{asset_id}/add_tags: %s', resp.text)

    def add_image_to_collection(self, asset_id, collection_id):
        """
        Given information about an asset, add it to the Tandem Vault collection
        for the appropriate Miro prefix.
        """

        # Add an asset to a collection.
        # https://tandemvault.com/docs/api/v1/collections/add_assets.html
        resp = self.sess.put(
            f'{self.api_url}/collections/{collection_id}/add_assets',
            params={
                'api_key': self.api_key,
                'asset_ids': asset_id,
            }
        )
        logger.debug('Response from PUT /add_assets: %s', resp.text)
