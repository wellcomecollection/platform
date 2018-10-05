#!/usr/bin/env python
# -*- encoding: utf-8

import json


def is_error_response(resp, status, description=None):
    """
    Constructs an error response to match against API responses.
    """
    assert resp.status_code == status

    labels = {
        400: "Bad Request",
        404: "Not Found",
        405: "Method Not Allowed",
        500: "Internal Server Error",
    }

    expected_resp = {
        "errorType": "http",
        "httpStatus": status,
        "label": labels[status],
        "type": "Error",
        "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
    }

    if description is not None:
        expected_resp["description"] = description

    assert json.loads(resp.data) == expected_resp
    return True
