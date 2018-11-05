# -*- encoding: utf-8

from flask import Response, jsonify
from werkzeug.wsgi import ClosingIterator


class ContextResponse(Response):
    """
    This class adds the "@context" parameter to JSON responses before
    they're sent to the user.

    For an explanation of how this works/is used, read
    https://blog.miguelgrinberg.com/post/customizing-the-flask-response-class
    """

    def __init__(self, response, *args, **kwargs):
        """
        Unmarshal the response as provided by Flask-RESTPlus, add the
        @context parameter, then repack it.
        """
        if isinstance(response, ClosingIterator):
            response = b"".join([char for char in response])

        # Some requests (e.g. POST /ingests) return an empty response body,
        # so we shouldn't try to add a parameter.
        if not response:
            return super().__init__(response, *args, **kwargs)

        # The @context may already be provided if we've been through the
        # force_type method below.  We also don't add a context if we're
        # looking at the healthcheck endpoint.
        return super().__init__(response, *args, **kwargs)

    @classmethod
    def force_type(cls, rv, environ=None):
        # All of our endpoints should be returning a dictionary to be
        # serialised as JSON.
        assert isinstance(rv, dict)

        return super().force_type(jsonify(rv), environ)
