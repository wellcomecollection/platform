#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Test the effect of using shared HTTP sessions when using the Sierra API.

Usage: sierra_session_tester.py --oauthkey=<KEY> --oauthsec=<SEC>
       sierra_session_tester.py -h | --help

Options:
  --oauthkey=<KEY>    OAuth key for the Sierra API
  --oauthsec=<SEC>    OAuth secret for the Sierra API



Background
~~~~~~~~~~

When developing the Sierra reader, we noticed increasing slowdowns between
successive responses.  Page 1 took 1s, page 2 took 2s, page 3 took 4s, etc.
We traced this to ~something funky happening when we used shared HTTP sessions.
If we created a new session for each page, everything was fine.

This is a little test harness we wrote to observe this behaviour.  It fetches
the first 1000 results in a window, and prints the time between responses.

Note: the window is hard-coded.  You may need to adjust it if this window
has emptied since we wrote the script!

"""

import time

import docopt
import requests


class SierraAPI(object):
    def __init__(self, api_url, oauth_key, oauth_secret, use_shared_session):
        self.api_url = api_url
        self.oauth_key = oauth_key
        self.oauth_secret = oauth_secret
        self.use_shared_session = use_shared_session

        if use_shared_session:
            self.sess = requests.Session()
        else:
            self.sess = None

        self.responses = []
        self._refresh_auth_token()

    # This causes the Session to call ``raise_for_status()`` as soon
    # as a response is received, so any failing request will throw an
    # exception immediately.
    #
    # It also logs the entire response object for later debugging.
    def raise_error(self, resp, *args, **kwargs):
        self.responses.append(resp)
        resp.raise_for_status()

    @property
    def sess(self):
        # Return the shared Session object if we have one, or a new
        # Session object if not.
        if self.use_shared_session:
            self._sess.hooks['response'] = [self.raise_error]
            _sess = self._sess
        else:
            _sess = requests.Session()
            _sess.hooks['response'].append(self.raise_error)

        if hasattr(self, 'headers'):
            self._sess.headers = self.headers
        return _sess

    @sess.setter
    def sess(self, value):
        self._sess = value

    def _refresh_auth_token(self):
        # Get an access token
        # https://sandbox.iii.com/docs/Content/zReference/authClient.htm
        resp = self.sess.post(
            f'{self.api_url}/token',
            auth=(self.oauth_key, self.oauth_secret)
        )

        access_token = resp.json()['access_token']

        self.headers = {
            'Authorization': f'Bearer {access_token}',
            'Accept': 'application/json',
            'Connection': 'close',
            # 'Connection' :'Keep-Alive',
            # 'Keep-Alive': 'max=1',
            # 'Cookie': 'jcontainerId=.jcontainer-public1',
        }

    def _http_get(self, path, params):
        def _call():
            get_url = f'{self.api_url}{path}'

            from contextlib import closing

            with closing(self.sess.get(get_url, params=params)) as resp:
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
                i = 0
                t = time.time()
                t0 = t
                while True:
                    try:
                        i += len(_self.objs['entries'])
                        print(i, time.time() - t0)
                        t0 = time.time()
                        yield from _self.objs['entries']
                        last_id = int(_self.objs['entries'][-1]['id']) + 1
                        _self.objs = _get(last_id)
                    except KeyError:
                        break

            def next(_self):
                return next(_self._gen)

        o = ObjectIterable()

        return o


def run_test(*args, **kwargs):
    s = SierraAPI(
        "https://libsys.wellcomelibrary.org/iii/sierra-api/v4",
        *args, **kwargs
    )

    bibs = s.get_objects("/bibs", params={
        'updatedDate': '[2018-01-12T07:48:08.040617+00:00,2018-01-12T08:18:08.040617+00:00]',
        "fields": "updatedDate,createdDate,deletedDate,deleted,suppressed,available,lang,title,author,materialType,bibLevel,publishYear,catalogDate,country,orders,normTitle,normAuthor,locations,fixedFields,varFields",
        'limit': 50,
    })

    for i, _ in enumerate(bibs):
        if i == 1000:
            break

    return s


if __name__ == '__main__':
    args = docopt.docopt(__doc__)
    oauth_key = args['--oauthkey']
    oauth_secret = args['--oauthsec']

    print('=' * 79)
    print('Using a shared session')
    print('=' * 79)
    run_test(
        use_shared_session=True,
        oauth_key=oauth_key,
        oauth_secret=oauth_secret
    )

    print('\n' * 1)
    #
    print('=' * 79)
    print('Ignoring shared sessions')
    print('=' * 79)
    run_test(
        use_shared_session=False,
        oauth_key=oauth_key,
        oauth_secret=oauth_secret
    )
