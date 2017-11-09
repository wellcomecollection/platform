import base64
import requests
import os

from betamax import Betamax
import pytest

import sierra_api


# Read environment config.
oauthkey = os.environ.get("SIERRA_OAUTH_KEY", "sierra_key")
oauthsec = os.environ.get("SIERRA_OAUTH_SECRET", "sierra_secret")


@pytest.fixture
def recorder():
    """Returns an instance of the API with Betamax enabled."""

    # The OAuth key and secret are initially passed with HTTP Basic Auth,
    # so make sure we stub this out in recorded responses.
    config = Betamax.configure()
    config.define_cassette_placeholder(
        '<SIERRA_OAUTH>',
        base64.b64encode(
            f'{oauthkey}:{oauthsec}'.encode('utf-8')
        ).decode('utf-8')
    )

    session = requests.Session()

    # Now set up a recorder and session
    return {
        'api_url': 'https://libsys.wellcomelibrary.org/iii/sierra-api/v3',
        'oauthkey': oauthkey,
        'oauthsec': oauthsec,
        'session': session,
        'betamax': Betamax(session, cassette_library_dir='/cassettes')
    }


@pytest.fixture
def api(recorder):
    # Create an API session in the recorder, and pass it up to the
    # test session.
    with recorder['betamax'].use_cassette('sierra_api'):
        yield sierra_api.SierraAPI(
            recorder['api_url'],
            recorder['oauthkey'],
            recorder['oauthsec'],
            recorder['session']
        )
