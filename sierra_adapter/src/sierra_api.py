import logging
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

    def _http_get(self, path, params):
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

    def _get_objects_from_id(self, path, id, params):
        id_param = {'id': f'[{id},]'}
        merged_params = {
            **id_param,
            **params
        }
        try:
            return self._http_get(
                path=path,
                params=merged_params
            ).json()['entries']
        except requests.HTTPError as err:

            # When requesting a set of objects that is empty
            # the API will return a 404, so substitute for an
            # empty list.
            if err.response.status_code == 404:
                return []
            else:
                raise

    def get_objects(self, path, params=None):
        def _get(id):
            return self._get_objects_from_id(
                path=path,
                id=id,
                params=params
            )

        if params is None:
            params = {}

        objs = _get(0)

        while objs:
            yield from objs
            objs = _get(int(objs[-1]['id']) + 1)
