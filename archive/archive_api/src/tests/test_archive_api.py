# -*- encoding: utf-8

import json


class TestReportHealthStatus:
    """
    Tests for the GET /healthcheck endpoint.
    """

    def test_get_healthcheck_endpoint_is_200_OK(self, client):
        resp = client.get("/storage/v1/healthcheck")
        assert resp.status_code == 200
        assert json.loads(resp.data) == {"status": "OK"}
