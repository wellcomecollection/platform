# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

import collections

import requests


API_URL = 'https://wellcome.tandemvault.com/api/v1'


Miro = collections.namedtuple('Miro', ['upload_set'])

upload_sets = {
    'A: Animal images': Miro(47806),
    'AS: Family life': Miro(47807),
    'B: External biomedical': Miro(47808),
    'D: Footage': Miro(47809),
    'F: Microfilm': Miro(47810),
    'FP: Family life': Miro(47811),
    'L: Library (historical)': Miro(47812),
    'M: Library (historical)': Miro(47813),
    'N: Clinical': Miro(47814),
    'S: Slide collection': Miro(47815),
    'V: Iconographic works': Miro(47816),
    'W: Publishing group Intl health': Miro(47817),
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


titles = [
    'A: Animal images',
    'AS: Family life',
    'B: External biomedical',
    'D: Footage',
    'F: Microfilm',
    'FP: Family life',
    'L: Library (historical)',
    'M: Library (historical)',
    'N: Clinical',
    'S: Slide collection',
    'V: Iconographic works',
    'W: Publishing group Intl health',
]


api = TandemVaultAPI(api_key=API_KEY)
for t in titles:
    print(t, api.create_upload_set(title=t))
