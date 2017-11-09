import base64
import requests
import os

import betamax
from betamax import Betamax
import pytest

import sierra_api


@pytest.fixture
def api():
    """Returns an instance of the API with Betamax enabled."""
    # Read environment config.
    oauthkey = os.environ.get("SIERRA_OAUTH_KEY", "sierra_key")
    oauthsec = os.environ.get("SIERRA_OAUTH_SECRET", "sierra_secret")

    # The OAuth key and secret are initially passed with HTTP Basic Auth,
    # so make sure we stub this out in recorded responses.
    config = Betamax.configure()
    config.define_cassette_placeholder(
        '<SIERRA_OAUTH>',
        base64.b64encode(
            f'{oauthkey}:{oauthsec}'.encode('utf-8')
        ).decode('utf-8')
    )

    # Now set up a recorder.
    session = requests.Session()
    recorder = Betamax(session, cassette_library_dir='/cassettes')

    # Then create an API session in the recorder, and pass it up to the
    # test session.
    with recorder.use_cassette('sierra_api'):
        api_url = 'https://libsys.wellcomelibrary.org/iii/sierra-api/v3'
        yield sierra_api.SierraAPI(api_url, oauthkey, oauthsec, session)


def test_can_get_bibs_from_api(api):
    expected_length = 29

    bibs = api.get_objects("/bibs", params={
        'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
    })

    actual_length = len(bibs)
    assert actual_length == expected_length

    results = list(bibs)
    assert len(results) == expected_length


def test_can_get_items_from_api(api):
    expected_length = 50

    bibs = api.get_objects("/items", {
        'updatedDate': "[2013-12-10T17:16:35Z,2013-12-13T21:34:35Z]"
    })

    actual_length = len(bibs)
    assert actual_length == expected_length

    results = list(bibs)
    assert len(results) == expected_length
