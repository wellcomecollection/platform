# -*- encoding: utf-8

import json

from helpers import assert_is_error_response


class TestGET:
    """
    Tests for the GET /ingests/<guid> endpoint.
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
        assert_is_error_response(
            resp,
            status=404,
            description="Invalid id: No ingest found for id=%r" % lookup_id
        )

    def test_post_against_lookup_endpoint_is_405(self, client, guid):
        resp = client.post(f"/storage/v1/ingests/{guid}")
        assert_is_error_response(
            resp,
            status=405,
            description="The method is not allowed for the requested URL."
        )


class TestPOST:
    """
    Tests for the POST /ingests endpoint.
    """

    upload_url = "s3://example-bukkit/helloworld.zip"
    callback_url = "https://example.com/post?callback"

    def test_request_new_ingest_is_201(self, client):
        resp = client.post(
            "/storage/v1/ingests", json=_create_ingest_request(self.upload_url)
        )
        assert resp.status_code == 201
        assert resp.data == b""

    def test_no_type_is_badrequest(self, client):
        resp = client.post("/storage/v1/ingests", json={})
        assert_is_error_response(
            resp,
            status=400,
            description="'ingestType' is a required property, 'type' is a required property, 'uploadUrl' is a required property"
        )

    def test_invalid_type_is_badrequest(self, client):
        resp = client.post("/storage/v1/ingests", json={"type": "UnexpectedType"})
        assert_is_error_response(
            resp,
            status=400,
            description="'ingestType' is a required property, 'uploadUrl' is a required property, 'UnexpectedType' is not one of ['Ingest']"
        )

    def test_no_ingest_type_is_badrequest(self, client):
        resp = client.post("/storage/v1/ingests", json={"type": "Ingest", "uploadUrl": "http://example.net"})
        assert_is_error_response(
            resp,
            status=400,
            description="'ingestType' is a required property"
        )

    def test_invalid_ingest_type_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json={"type": "Ingest", "ingestType": {"type": "UnexpectedIngestType"}},
        )
        assert_is_error_response(
            resp,
            status=400,
            # TODO: Really?  You want an ID?
            description="'uploadUrl' is a required property, 'id' is a required property, 'UnexpectedIngestType' is not one of ['IngestType']"
        )

    def test_no_uploadurl_is_badrequest(self, client):
        resp = client.post("/storage/v1/ingests", json=_create_ingest_request())
        assert_is_error_response(
            resp,
            status=400,
            description="'uploadUrl' is a required property"
        )

    def test_invalid_uploadurl_is_badrequest(self, client):
        resp = client.post("/storage/v1/ingests", json=_create_ingest_request("not-a-url"))
        assert_is_error_response(
            resp,
            status=400,
            description="Invalid uploadUrl:'not-a-url', is not a complete URL, '' is not a supported scheme ['s3']"
        )

    def test_invalid_scheme_uploadurl_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request("ftp://example-bukkit/helloworld.zip"),
        )
        assert_is_error_response(
            resp,
            status=400,
            description="Invalid uploadUrl:'ftp://example-bukkit/helloworld.zip', 'ftp' is not a supported scheme ['s3']"
        )

    def test_uploadurl_with_fragments_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request("s3://example-bukkit/helloworld.zip#fragment"),
        )
        assert_is_error_response(
            resp,
            status=400,
            description="Invalid uploadUrl:'s3://example-bukkit/helloworld.zip#fragment', 'fragment' fragment is not allowed"
        )

    def test_invalid_callback_url_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request(self.upload_url, "not-a-url"),
        )
        assert_is_error_response(
            resp,
            status=400,
            description="Invalid callbackUrl:'not-a-url', is not a complete URL, '' is not a supported scheme ['http', 'https']"
        )

    def test_invalid_scheme_callback_url_is_badrequest(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request(self.upload_url, "s3://example.com"),
        )
        assert_is_error_response(
            resp,
            status=400,
            description="Invalid callbackUrl:'s3://example.com', 's3' is not a supported scheme ['http', 'https']"
        )

    def test_request_allows_fragment_in_callback(self, client):
        resp = client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request(
                self.upload_url, f"{self.callback_url}#fragment"
            ),
        )
        assert resp.status_code == 201

    def test_request_new_ingest_has_location_header(self, client):
        resp = client.post(
            "/storage/v1/ingests", json=_create_ingest_request(self.upload_url)
        )
        assert "Location" in resp.headers

        # TODO: This might need revisiting when we deploy the app behind
        # an ALB and these paths are no longer correct.
        new_location = resp.headers["Location"]
        assert new_location.startswith("http://localhost/storage/v1/ingests/")

    def test_successful_request_sends_to_sns(self, client, sns_client):
        resp = client.post(
            "/storage/v1/ingests", json=_create_ingest_request(self.upload_url)
        )

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][":message"]

        assert message["zippedBagLocation"] == {
            "namespace": "example-bukkit",
            "key": "helloworld.zip",
        }

        # This checks that the request ID sent to SNS is the same as
        # the one we've been given to look up the request later.
        assert resp.headers["Location"].endswith(message["archiveRequestId"])

    def test_successful_request_sends_to_sns_with_callback(self, client, sns_client):
        client.post(
            "/storage/v1/ingests",
            json=_create_ingest_request(self.upload_url, self.callback_url),
        )

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][":message"]

        assert "archiveCompleteCallbackUrl" in message
        assert message["archiveCompleteCallbackUrl"] == self.callback_url

        resp = client.get("/storage/v1/ingests")
        assert_is_error_response(
            resp,
            status=405,
            description="The method is not allowed for the requested URL."
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
            description="The browser (or proxy) sent a request that this server could not understand."
        )


def _create_ingest_request(upload_url=None, callback_url=None):
    request = {"type": "Ingest", "ingestType": {"id": "create", "type": "IngestType"}}
    if upload_url:
        request["uploadUrl"] = upload_url
    if callback_url:
        request["callbackUrl"] = callback_url
    return request
