# -*- encoding: utf-8 -*-
"""
Utilities for interacting with the Tandem Vault API.
"""

# from wellcome_lambda_utils.http_utils import http_post
from http_utils import http_post

API_URL = 'https://wellcome.tandemvault.com/api/v1'


def create_upload_set(api_key, title):
    """
    Create an upload set in Tandem Vault, and return the upload
    set identifier.
    """
    resp = http_post(
        f'{API_URL}/upload_sets',
        params={
            'api_key': api_key,
            'upload_set[title]': title
        }
    )
    return resp.json()['id']


create_upload_set(API_KEY, title='Alex test upload set')
