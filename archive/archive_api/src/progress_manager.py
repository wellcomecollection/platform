# -*- encoding: utf-8

import json
import os

import requests


class ProgressServiceError(Exception):
    """Raised if we get an unexpected error from the progress service."""


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

    def create_request(self, upload_url, callback_url):
        """
        Make a request to the progress service to start a new request.

        Returns the ID of the new ingest.

        """
        # The service expects to receive a JSON dictionary of the following
        # form:
        #
        #     {
        #         "uploadUrl": "...",
        #         "callbackUrl": "..."
        #     }
        #
        # Here "callbackUrl" is optional.  If successful, the service returns
        # a 202 Created and the new ID in the path parameter of the
        # Location header.
        #
        data = {"uploadUri": upload_url}
        if callback_url is not None:
            data["callbackUri"] = callback_url

        resp = self.sess.post(f"{self.endpoint}/progress", json=data, timeout=1)

        # The service should return an HTTP 202 if successful.  Anything
        # else should be treated as an error.
        if resp.status_code != 201:
            raise ProgressServiceError(
                "Expected HTTP 201; got %d (data=%r)" % (resp.status_code, data)
            )

        # The new ID should be sent in the path parameter of the Location
        # header.  If this header is missing, that's an error.
        try:
            location = resp.headers["Location"]
        except KeyError:
            raise ProgressServiceError(
                "No Location header in progress response; got %r (data=%r)"
                % (resp.headers, data)
            )

        # Finally, extract the ID from the location URL.
        return os.path.basename(location)

    def lookup_progress(self, id):
        """
        Look up an existing ingest request.

        Passes the response through directly (if any).

        """
        resp = self.sess.get(f"{self.endpoint}/progress/{id}", timeout=1)

        # The service should return an HTTP 200 (if present) or 404 (if not).
        # Anything else should be treated as an error.
        if resp.status_code not in (200, 404):
            raise ProgressServiceError(
                "Expected HTTP 200 or 404; got %d (id=%r)" % (resp.status_code, id)
            )
        elif resp.status_code == 404:
            raise ProgressNotFoundError(id)
        else:
            try:
                return resp.json()
            except json.JSONDecodeError as err:
                raise ProgressServiceError(err)
