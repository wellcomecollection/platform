# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import requests


API_URL = 'https://wellcome.tandemvault.com/api/v1'


class TandemVaultAPI(object):

    def __init__(self, api_key):
        self.api_key = api_key
        self.sess = requests.Session()

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


api = TandemVaultAPI(api_key=API_KEY)
api.create_upload_set(title='Alex test upload set')
