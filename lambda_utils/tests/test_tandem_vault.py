# -*- encoding: utf-8 -*-

import os

from betamax import Betamax
import pytest
import requests

from src.wellcome_lambda_utils import tandem_vault


api_key = os.environ.get('API_KEY', '<API_KEY>')

with Betamax.configure() as config:
    config.cassette_library_dir = 'cassettes'
    config.define_cassette_placeholder('<API_KEY>', api_key)

sess = requests.Session()
with Betamax(sess) as vcr:
    vcr.use_cassette('tandem_vault')
    api = tandem_vault.TandemVaultAPI(api_key, sess=sess)


def test_can_create_upload_set():
    set_id = api.create_upload_set(title='Alex test upload set')
    assert isinstance(set_id, int)


def test_can_get_upload_set():
    upload_sets = api.get_upload_sets()

    # This is a generator object, we can read some upload sets
    for _ in range(3):
        next(upload_sets)

    # And make some quick assertions about the fourth upload set
    upload_set = next(upload_sets)
    assert isinstance(upload_set, dict)
    assert 'id' in upload_set
    assert 'title' in upload_set
