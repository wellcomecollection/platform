# -*- encoding: utf-8

import json

import requests


class BagServiceError(Exception):
    pass

class BagNotFoundError(Exception):
    pass


class BagsManager:

    def __init__(self, endpoint, sess=None):
        self.endpoint = endpoint
        self.sess = sess or requests.Session()

    def lookup_bag(self, space, id):
        resp = self.sess.get(f"{self.endpoint}/registrar/{space}/{id}", timeout=1)

        if resp.status_code not in (200, 404):
            raise BagServiceError(
                "Expected HTTP 200 or 404; got %d (id=%r)" % (resp.status_code, id)
            )
        elif resp.status_code == 404:
            raise BagNotFoundError(id)
        else:
            try:
                return resp.json()
            except json.JSONDecodeError as err:
                raise BagServiceError(err)
