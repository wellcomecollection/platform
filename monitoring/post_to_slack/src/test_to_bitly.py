# -*- encoding: utf-8 -*-

import os

from betamax import Betamax
import requests
import pytest

from post_to_slack import to_bitly


with Betamax.configure() as config:
    config.cassette_library_dir = 'fixtures'

    access_token = os.environ.get('BITLY_ACCESS_TOKEN', 'testtoken')
    config.define_cassette_placeholder('<ACCESS_TOKEN>', access_token)


@pytest.fixture
def sess():
    session = requests.Session()
    with Betamax(session) as vcr:
        vcr.use_cassette('to_bitly', record='once')
        yield session


@pytest.mark.parametrize('bad_token', [None, 'invalid_t0k3n'])
def test_url_with_missing_token_is_unchanged(sess, bad_token):
    url = 'https://eu-west-1.console.aws.amazon.com/cloudwatch/home'
    shortened_url = to_bitly(sess=sess, url=url, access_token=bad_token)
    assert url == shortened_url


def test_shortening_url_with_good_token(sess):
    url = 'https://us-east-1.console.aws.amazon.com/cloudwatch/home'
    shortened_url = to_bitly(sess=sess, url=url, access_token=access_token)
    assert shortened_url == 'http://amzn.to/2GQzbsA'
