# -*- encoding: utf-8

import json

from helpers import assert_is_error_response


def test_get_root_endpoint_is_404(client):
    resp = client.get("/")
    assert_is_error_response(
        resp,
        status=404,
        description="The requested URL was not found on the server.  If you entered the URL manually please check your spelling and try again.",
    )


def test_get_unknown_endpoint_is_404(client):
    resp = client.get("/foo")
    assert_is_error_response(
        resp,
        status=404,
        description="The requested URL was not found on the server.  If you entered the URL manually please check your spelling and try again.",
    )


class TestReportHealthStatus:
    """
    Tests for the GET /healthcheck endpoint.
    """

    def test_get_healthcheck_endpoint_is_200_OK(self, client):
        resp = client.get("/storage/v1/healthcheck")
        assert resp.status_code == 200
        assert json.loads(resp.data) == {"status": "OK"}
