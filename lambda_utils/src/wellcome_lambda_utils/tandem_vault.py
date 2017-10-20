# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import collections
from pprint import pprint

import requests


API_URL = 'https://wellcome.tandemvault.com/api/v1'


Miro = collections.namedtuple('Miro', ['upload_set', 'collection_id'])

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
