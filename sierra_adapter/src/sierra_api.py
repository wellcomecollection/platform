import logging
import os
import daiquiri
import requests


daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


class SierraAPI(object):
    def __init__(self, api_url, oauth_key, oauth_secret, sess=None):
        self.api_url = api_url
        self.oauth_key = oauth_key
        self.oauth_secret = oauth_secret
        self.sess = sess or requests.Session()

        self._refresh_auth_token()

        # This causes the Session to call ``raise_for_status()`` as soon
        # as a response is received, so any failing request will throw an
        # exception immediately.
        def raise_error(resp, *args, **kwargs):
            resp.raise_for_status()

        self.sess.hooks['response'].append(raise_error)

    def _refresh_auth_token(self):
        # Get an access token
        # https://sandbox.iii.com/docs/Content/zReference/authClient.htm
        resp = self.sess.post(
            f'{self.api_url}/token',
            auth=(self.oauth_key, self.oauth_secret)
        )

        access_token = resp.json()['access_token']

        self.sess.headers = {
            'Authorization': f'Bearer {access_token}',
            'Accept': 'application/json',
        }

    def _http_get(self, path, params=None):
        if params is None:
            params = {}

        def _call():
            return self.sess.get(
                f'{self.api_url}{path}',
                params=params
            )

        try:
            return _call()
        except requests.HTTPError as err:
            if err.response.status_code == 401:
                self._refresh_auth_token()
                return _call()
            else:
                raise

    def get_bibs(self):
        yield from self._http_get(
            path='/bibs',
            params={'id': '[0,]'}
        ).json()['entries']
