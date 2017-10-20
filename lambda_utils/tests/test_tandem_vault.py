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


@pytest.fixture
def api():
    sess = requests.Session()
    with Betamax(sess) as vcr:
        vcr.use_cassette('tandem_vault')
        yield tandem_vault.TandemVaultAPI(api_key, sess=sess)
