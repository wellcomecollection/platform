# -*- encoding: utf-8

import json

import pytest


class TestGETIngests:
    """
    Tests for the GET /ingests/<id> endpoint.
    """
    def test_lookup_item(self, client):
        lookup_id = "F423966E-A5E5-4D91-B321-88B90D1B5154"
        resp = client.get(f"/storage/v1/ingests/{lookup_id}")

        assert resp.status_code == 200
        assert json.loads(resp.data) == {
            "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
            "progress": lookup_id,
        }

    def test_lookup_missing_item_is_404(self, client):
        lookup_id = "bad_status-404"
        resp = client.get(f"/storage/v1/ingests/{lookup_id}")

        assert resp.status_code == 404
        assert json.loads(resp.data) == {
            "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
            "description": f"No ingest found for id='bad_status-404'",
            "errorType": "http",
            "httpStatus": 404,
            "label": "Not Found",
            "type": "Error"
        }

    @pytest.mark.skip("This doesn't seem to be working right now")
    def test_post_against_lookup_endpoint_is_405(self, client, guid):
        resp = client.post(f"/storage/v1/ingests/{guid}")

        assert resp.status_code == 405
        assert json.loads(resp.data) == {
            "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
            "description": "The method is not allowed for the requested URL.",
            "errorType": "http",
            "httpStatus": 405,
            "label": "Method Not Allowed",
            "type": "Error"
        }
