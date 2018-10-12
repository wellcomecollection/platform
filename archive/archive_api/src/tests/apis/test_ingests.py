# -*- encoding: utf-8

import json

import pytest

from helpers import assert_is_error_response


class TestGETIngests:
    """
    Tests for the GET /ingests/<guid> endpoint.
    """

    def test_lookup_item(self, client):
        lookup_id = "F423966E-A5E5-4D91-B321-88B90D1B5154"
        resp = client.get(f"/storage/v1/ingests/{lookup_id}")
        assert resp.status_code == 200

        rv = json.loads(resp.data)
        assert (
            rv["@context"]
            == "https://api.wellcomecollection.org/storage/v1/context.json"
        )
        assert rv["id"] == lookup_id
        assert rv["type"] == "Ingest"
        assert isinstance(rv["uploadUrl"], str)
        assert isinstance(rv["callbackUrl"], str)
        assert rv["ingestType"] == {"id": "create", "type": "IngestType"}
        assert rv["status"] == {"id": "processing", "type": "IngestStatus"}
        assert isinstance(rv["createdDate"], str)
        assert isinstance(rv["lastModifiedDate"], str)
        assert len(rv["events"]) == 1
        event = rv["events"][0]
        assert isinstance(event, dict)
        assert isinstance(event["description"], str)
        assert isinstance(event["createdDate"], str)
        assert event["type"] == "ProgressEvent"

    def test_lookup_missing_item_is_404(self, client):
        lookup_id = "bad_status-404"
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
        assert resp.data == b""

    def test_no_type_is_badrequest(self, client, ingest_request):
        del ingest_request["type"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(
            resp, status=400, description="'type' is a required property"
        )

    def test_invalid_type_is_badrequest(self, client, ingest_request):
        ingest_request["type"] = "UnexpectedType"
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(
            resp, status=400, description="'UnexpectedType' is not one of ['Ingest']"
        )

    def test_no_ingest_type_is_badrequest(self, client, ingest_request):
        del ingest_request["ingestType"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(
            resp, status=400, description="'ingestType' is a required property"
        )

    @pytest.mark.parametrize(
        "ingest_type,error_description",
        [
            (
                {"id": "create", "type": "UnexpectedIngestType"},
                "'UnexpectedIngestType' is not one of ['IngestType']",
            ),
            (
                {"id": "destroy", "type": "IngestType"},
                "'destroy' is not one of ['create']",
            ),
            ({"type": "IngestType"}, "'id' is a required property"),
            ({"id": "create"}, "'type' is a required property"),
        ],
    )
    def test_invalid_ingest_type_is_badrequest(
        self, client, ingest_request, ingest_type, error_description
    ):
        ingest_request["ingestType"] = ingest_type
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(resp, status=400, description=error_description)

    def test_no_uploadurl_is_badrequest(self, client, ingest_request):
        del ingest_request["uploadUrl"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(
            resp, status=400, description="'uploadUrl' is a required property"
        )

    @pytest.mark.parametrize(
        "upload_url,error_description",
        [
            (
                "not-a-url",
                "Invalid uploadUrl:'not-a-url', is not a complete URL, '' is not a supported scheme ['s3']",
            ),
            (
                "ftp://example-bukkit/helloworld.zip",
                "Invalid uploadUrl:'ftp://example-bukkit/helloworld.zip', 'ftp' is not a supported scheme ['s3']",
            ),
            (
                "s3://example-bukkit/helloworld.zip#fragment",
                "Invalid uploadUrl:'s3://example-bukkit/helloworld.zip#fragment', 'fragment' fragment is not allowed",
            ),
        ],
    )
    def test_invalid_uploadurl_is_badrequest(
        self, client, ingest_request, upload_url, error_description
    ):
        ingest_request["uploadUrl"] = upload_url
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(resp, status=400, description=error_description)

    def test_allows_no_callback_url(self, client, ingest_request):
        del ingest_request["callbackUrl"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert resp.status_code == 201

    @pytest.mark.parametrize(
        "callback_url,error_description",
        [
            (
                "not-a-url",
                "Invalid callbackUrl:'not-a-url', is not a complete URL, '' is not a supported scheme ['http', 'https']",
            ),
            (
                "s3://example.com",
                "Invalid callbackUrl:'s3://example.com', 's3' is not a supported scheme ['http', 'https']",
            ),
        ],
    )
    def test_invalid_callback_url_is_badrequest(
        self, client, ingest_request, callback_url, error_description
    ):
        ingest_request["callbackUrl"] = callback_url
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert_is_error_response(resp, status=400, description=error_description)

    def test_request_allows_fragment_in_callback(self, client, ingest_request):
        ingest_request["callbackUrl"] += "#fragment"
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert resp.status_code == 201

    def test_request_new_ingest_has_location_header(self, client, ingest_request):
        resp = client.post("/storage/v1/ingests", json=ingest_request)
        assert "Location" in resp.headers

        # TODO: This might need revisiting when we deploy the app behind
        # an ALB and these paths are no longer correct.
        new_location = resp.headers["Location"]
        assert new_location.startswith("http://localhost/storage/v1/ingests/")

    def test_successful_request_sends_to_sns(self, client, sns_client, ingest_request):
        del ingest_request["callbackUrl"]
        resp = client.post("/storage/v1/ingests", json=ingest_request)

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][":message"]

        assert "archiveCompleteCallbackUrl" not in message

        assert message["zippedBagLocation"] == {
            "namespace": "example-bukkit",
            "key": "helloworld.zip",
        }

        # This checks that the request ID sent to SNS is the same as
        # the one we've been given to look up the request later.
        assert resp.headers["Location"].endswith(message["archiveRequestId"])

    def test_successful_request_sends_to_sns_with_callback(
        self, client, sns_client, ingest_request
    ):
        client.post("/storage/v1/ingests", json=ingest_request)

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][":message"]

        assert "archiveCompleteCallbackUrl" in message
        assert message["archiveCompleteCallbackUrl"] == ingest_request["callbackUrl"]

        resp = client.get("/storage/v1/ingests")
        assert_is_error_response(
            resp,
            status=405,
            description="The method is not allowed for the requested URL.",
        )

    def test_request_not_json_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            data="notjson",
            headers={"Content-Type": "application/json"},
        )
        assert_is_error_response(
            resp,
            status=400,
            description="The browser (or proxy) sent a request that this server could not understand.",
        )


@pytest.fixture
def ingest_request():
    return {
        "type": "Ingest",
        "ingestType": {"id": "create", "type": "IngestType"},
        "uploadUrl": "s3://example-bukkit/helloworld.zip",
        "callbackUrl": "https://example.com/post?callback",
    }
