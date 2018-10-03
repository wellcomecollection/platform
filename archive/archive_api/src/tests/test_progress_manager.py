# -*- encoding: utf-8

import betamax
import pytest
import requests

from progress_manager import ProgressManager


@pytest.fixture(scope='session')
def sess():
    with betamax.Betamax.configure() as config:
        config.cassette_library_dir = 'tests/cassettes'

    session = requests.Session()
    with betamax.Betamax(session) as vcr:
        vcr.use_cassette('test_progress_manager')
        yield session


@pytest.fixture(scope='session')
def progress_manager(sess):
    # AWLC: Currently these cassettes were recorded by running against
    # an imitation progress app written in Flask.
    #
    # At some point, swap out the cassettes for genuine responses from
    # a real progress app.
    #
    return ProgressManager(endpoint='http://localhost:6000', sess=sess)


def test_can_create_ingest_request(progress_manager):
    progress_manager.create_request(
        upload_url='http://example.org/',
        callback_url=None
    )


def test_can_create_ingest_request_with_callback_url(progress_manager):
    progress_manager.create_request(
        upload_url='http://example.org',
        callback_url='http://callback.net?id=123'
    )
