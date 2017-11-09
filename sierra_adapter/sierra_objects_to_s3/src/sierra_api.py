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
            get_url = f'{self.api_url}{path}'
            resp = self.sess.get(
                f'{self.api_url}{path}',
                params=params
            )

            logger.debug(
                f'Response from GET {get_url}: {resp.text}'
            )

            return resp

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
            json_response = self._http_get(
                path=path,
                params=merged_params
            ).json()

            self._current_response = json_response

            return self._current_response
        except requests.HTTPError as err:

            # When requesting a set of objects that is empty
            # the API will return a 404, so substitute for an
            # empty list.
            if err.response.status_code == 404:
                return []
            else:
                raise

    def get_objects(self, path, params=None):
        if params is None:
            params = {}

        def _get(id):
            return self._get_objects_from_id(
                path=path,
                id=id,
                params=params
            )

        class ObjectIterable(object):
            def __init__(_self):
                _self.objs = _get(0)

            def __len__(_self):
                return self._current_response['total']

            def __iter__(_self):
                yield from _self.objs['entries']
                last_id = int(_self.objs['entries'][-1]['id']) + 1
                _self.objs = _get(last_id)

            def next(_self):
                return next(_self._gen)

        o = ObjectIterable()

        return o
