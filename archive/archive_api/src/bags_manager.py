# -*- encoding: utf-8

import json
import os

import requests


class BagServiceError(Exception):
    """Raised if we get an unexpected error from the progress service."""


class BagNotFoundError(Exception):
    """Raised if we try to look up a progress that doesn't exist."""


class BagsManager:
    """
    Handles requests to/from the progress service.

    The progress service is a separate, internal-only app running in ECS.
    It manages connections to the progress tracking table in DynamoDB --
    we should never query that table directly, only through this service.

    """

    def __init__(self, endpoint, sess=None):
        self.endpoint = endpoint
        self.sess = sess or requests.Session()

    def lookup_bag(self, space, id):
        """
        Look up an existing ingest request.

        Passes the response through directly (if any).

        """
        resp = self.sess.get(f"{self.endpoint}/registrar/{space}/{id}", timeout=1)

        # The service should return an HTTP 200 (if present) or 404 (if not).
        # Anything else should be treated as an error.
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
