# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import itertools

import requests


API_URL = 'https://wellcome.tandemvault.com/api/v1'


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

    def get_upload_sets(self):
        """
        Generates a list of all upload sets in Tandem Vault
        """
        for page in itertools.count(1):
            resp = self.sess.get(
                f'{API_URL}/upload_sets',
                params={
                    'api_key': self.api_key,
                    'page': page
                }
            )
            resp.raise_for_status()
            if not resp.json():
                break
            yield from resp.json()


api = TandemVaultAPI(api_key=API_KEY)
api.create_upload_set(title='Alex test upload set')
