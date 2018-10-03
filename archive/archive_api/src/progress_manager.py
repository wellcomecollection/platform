# -*- encoding: utf-8

import requests


class ProgressError(Exception):
    """Raised if we get an unexpected response from the progress service."""
    pass


class ProgressManager:
    """
    Handles requests to/from the progress service.
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
        data = {'uploadUrl': upload_url}
        if callback_url is not None:
            data['callbackUrl'] = callback_url

        resp = self.sess.post(f'{self.endpoint}/progress', data=data)
        print(resp.headers)
        return 'foo'
