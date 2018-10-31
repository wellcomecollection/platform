# -*- encoding: utf-8

import json

import pytest

from helpers import assert_is_error_response


class TestGETIngests:
    """
    Tests for the GET /ingests/<guid> endpoint.
    """

    def test_lookup_item(self, client):
        lookup_id = "15d76ac8-e92e-4fb5-aea4-7e5bc98dbc20"
        resp = client.get(f"/storage/v1/ingests/{lookup_id}")
        assert resp.status_code == 200

        rv = json.loads(resp.data)
        assert rv["id"] == lookup_id.lower()
        assert rv["type"] == "Ingest"
        assert isinstance(rv["uploadUrl"], str)
        assert rv["resources"] == [{"id": "digitised/b21508628", "type": "IngestResource"}]
        assert rv["ingestType"] == {"id": "create", "type": "IngestType"}
        assert rv["status"] == {"id": "completed", "type": "IngestStatus"}
        assert rv["space"] == {"id": "bububa", "type": "Space"}
        assert isinstance(rv["createdDate"], str)
        assert isinstance(rv["lastModifiedDate"], str)
        assert len(rv["events"]) == 4
        event = rv["events"][0]
        assert isinstance(event, dict)
        assert isinstance(event["description"], str)
        assert isinstance(event["createdDate"], str)
        assert event["type"] == "ProgressEvent"

    def test_lookup_missing_item_is_404(self, client):
        lookup_id = "15d76ac8-e92e-4fb5-aea4-7e5bc98dbc21"
        resp = client.get(f"/storage/v1/ingests/{lookup_id}")
        assert_is_error_response(
            resp,
            status=404,
            description="Invalid id: No ingest found for id=%r" % lookup_id,
        )

    def test_post_against_lookup_endpoint_is_405(self, client, guid):
        resp = client.post(f"/storage/v1/ingests/{guid}")
        assert_is_error_response(
            resp,
            status=405,
            description="The method is not allowed for the requested URL.",
        )


class TestPOSTIngests:
    """
    Tests for the POST /ingests endpoint.
    """

    def test_request_new_ingest_is_201(self, client, ingest_request):
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert resp.status_code == 201

    def test_no_uploadurl_is_badrequest(self, client, ingest_request):
        del ingest_request["uploadUrl"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(
            resp, status=400,
            description="The request content was malformed:\nAttempt to decode value on failed cursor: DownField(uploadUrl)"
        )

    def test_allows_no_callback_url(self, client, ingest_request):
        del ingest_request["callback"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert resp.status_code == 201

    def test_request_allows_fragment_in_callback(self, client, ingest_request):
        ingest_request["callback"]["uri"] += "#fragment"
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert resp.status_code == 201

    def test_request_new_ingest_has_location_header(self, client, ingest_request):
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert "Location" in resp.headers

        new_location = resp.headers["Location"]
        assert isinstance(new_location, str)

    def test_request_not_json_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            data="notjson",
            headers={"Content-Type": "application/json"},
        )
        assert_is_error_response(
            resp,
            status=400,
            description="The browser (or proxy) sent a request that this server could not understand."
        )


@pytest.fixture
def ingest_request():
    return {
        "type": "Ingest",
        "ingestType": {"id": "create", "type": "IngestType"},
        "uploadUrl": "s3://wellcomecollection-workflow-export-bagit/b21508628.zip",
        "callback": {
            "uri": "https://example.com/post?callback"},
        "space": {"id": "space-id", "type": "Space"},
    }
