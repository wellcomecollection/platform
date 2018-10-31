# -*- encoding: utf-8

import json

import requests


class ProgressServiceError(Exception):
    """Raised if we get an unexpected error from the progress service."""

    def __init__(self, status_code, message):
        self.status_code = status_code
        self.message = message


class ProgressNotFoundError(Exception):
    """Raised if we try to look up a progress that doesn't exist."""


class ProgressManager:
    """
    Handles requests to/from the progress service.

    The progress service is a separate, internal-only app running in ECS.
    It manages connections to the progress tracking table in DynamoDB --
    we should never query that table directly, only through this service.

    """

    def __init__(self, endpoint, sess=None):
        self.endpoint = endpoint
        self.sess = sess or requests.Session()

    def create_request(self, request_json):
        resp = self.sess.post(f"{self.endpoint}/progress", json=request_json, timeout=1)

        # The service should return an HTTP 202 if successful.  Anything
        # else should be treated as an error.
        if resp.status_code != 201:
            raise ProgressServiceError(resp.status_code, resp.content.decode("utf-8"))
        return resp.headers["Location"], resp.json()

    def lookup_progress(self, id):
        """
        Look up an existing ingest request.

        Passes the response through directly (if any).

        """
        resp = self.sess.get(f"{self.endpoint}/progress/{id}", timeout=1)

        # The service should return an HTTP 200 (if present) or 404 (if not).
        # Anything else should be treated as an error.
        if resp.status_code not in (200, 404):
            raise ProgressServiceError(resp.status_code, resp.content.decode("utf-8"))
        elif resp.status_code == 404:
            raise ProgressNotFoundError(id)
        else:
            try:
                return resp.json()
            except json.JSONDecodeError as err:
                raise ProgressServiceError(500, err.message)
