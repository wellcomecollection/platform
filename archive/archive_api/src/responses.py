# -*- encoding: utf-8

from flask import Response, jsonify


class ContextResponse(Response):
    """
    This class adds the "@context" parameter to JSON responses before
    they're sent to the user.

    For an explanation of how this works/is used, read
    https://blog.miguelgrinberg.com/post/customizing-the-flask-response-class
    """

    @classmethod
    def force_type(cls, rv, environ=None):
        # All of our endpoints should be returning a dictionary to be
        # serialised as JSON.
        assert isinstance(rv, dict)

        assert "@context" not in rv
        rv["@context"] = "https://api.wellcomecollection.org/storage/v1/context.json"

        return super(ContextResponse, cls).force_type(jsonify(rv), environ)
