import base64
import requests
import os

import betamax
from betamax import Betamax

import sierra_api


oauthkey = os.environ.get("SIERRA_OAUTH_KEY", "sierra_key")
oauthsec = os.environ.get("SIERRA_OAUTH_SECRET", "sierra_secret")

api_url = 'https://libsys.wellcomelibrary.org/iii/sierra-api/v3'

config = Betamax.configure()
config.define_cassette_placeholder(
    '<SIERRA_OAUTH>',
    base64.b64encode(
        f'{oauthkey}:{oauthsec}'.encode('utf-8')
    ).decode('utf-8')
)

session = requests.Session()
recorder = betamax.Betamax(
    session,
    cassette_library_dir='/cassettes'
)


def test_can_get_bibs_from_api():
    with recorder.use_cassette('sierra_bibs'):
        expected_length = 29

        api = sierra_api.SierraAPI(api_url, oauthkey, oauthsec, session)

        bibs = api.get_objects("/bibs", {
            'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
        })

        actual_length = len(bibs)
        assert actual_length == expected_length

        results = list(bibs)
        assert len(results) == expected_length


def test_can_get_items_from_api():
    with recorder.use_cassette('sierra_items'):
        expected_length = 50

        api = sierra_api.SierraAPI(api_url, oauthkey, oauthsec, session)

        bibs = api.get_objects("/items", {
            'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
        })

        actual_length = len(bibs)
        assert actual_length == expected_length

        results = list(bibs)
        assert len(results) == expected_length
