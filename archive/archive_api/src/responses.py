# -*- encoding: utf-8

import json

from flask import Response, jsonify


class ContextResponse(Response):
    """
    This class adds the "@context" parameter to JSON responses before
    they're sent to the user.

    For an explanation of how this works/is used, read
    https://blog.miguelgrinberg.com/post/customizing-the-flask-response-class
    """
    context_url = "https://api.wellcomecollection.org/storage/v1/context.json"

    def __init__(self, response, **kwargs):
        # Here we unmarshal the response as provided by Flask-RESTPlus, add
        # the @context parameter, then repack it.
        rv = json.loads(response)

        # The @context may already be provided if we've been through the
        # force_type method below.
        if "@context" in rv:
            return super(ContextResponse, self).__init__(response, **kwargs)
        else:
            rv["@context"] = self.context_url
            return super(ContextResponse, self).__init__(json.dumps(rv), **kwargs)

    @classmethod
    def force_type(cls, rv, environ=None):
        # All of our endpoints should be returning a dictionary to be
        # serialised as JSON.
        assert isinstance(rv, dict)

        assert "@context" not in rv, rv
        rv["@context"] = cls.context_url

        return super(ContextResponse, cls).force_type(jsonify(rv), environ)
