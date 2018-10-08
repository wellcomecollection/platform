# # -*- encoding: utf-8
#
# import json
#
#
#
#
# class TestRequestNewIngest:
#     """
#     Tests for the POST /ingests endpoint.
#     """
#
#     upload_url = "s3://example-bukkit/helloworld.zip"
#     callback_url = "https://example.com/post?callback"
#
#     def test_request_new_ingest_is_202(self, client):
#         resp = client.post("/storage/v1/ingests", json=ingests_post(self.upload_url))
#         assert resp.status_code == 202
#         assert resp.data == b""
#
#     def test_no_type_is_badrequest(self, client):
#         resp = client.post("/storage/v1/ingests", json={})
#         assert resp.status_code == 400
#         assert b"'type' is a required property" in resp.data
#
#     def test_invalid_type_is_badrequest(self, client):
#         resp = client.post("/storage/v1/ingests", json={"type": "UnexpectedType"})
#         assert resp.status_code == 400
#         assert b"'UnexpectedType' is not one of ['Ingest']" in resp.data
#
#     def test_no_ingest_type_is_badrequest(self, client):
#         resp = client.post("/storage/v1/ingests", json={"type": "Ingest"})
#         assert resp.status_code == 400
#         assert b"'ingestType' is a required property" in resp.data
#
#     def test_invalid_ingest_type_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             json={"type": "Ingest", "ingestType": {"type": "UnexpectedIngestType"}},
#         )
#         assert resp.status_code == 400
#         assert b"'UnexpectedIngestType' is not one of ['IngestType']" in resp.data
#
#     def test_no_uploadurl_is_badrequest(self, client):
#         resp = client.post("/storage/v1/ingests", json=ingests_post())
#         assert resp.status_code == 400
#         assert b"'uploadUrl' is a required property" in resp.data
#
#     def test_invalid_uploadurl_is_badrequest(self, client):
#         resp = client.post("/storage/v1/ingests", json=ingests_post("not-a-url"))
#         assert resp.status_code == 400
#         assert b"Invalid uploadUrl:'not-a-url', is not a complete URL" in resp.data
#
#     def test_invalid_scheme_uploadurl_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             json=ingests_post("ftp://example-bukkit/helloworld.zip"),
#         )
#         assert resp.status_code == 400
#         assert (
#             b"Invalid uploadUrl:'ftp://example-bukkit/helloworld.zip', 'ftp' is not a supported scheme ['s3']"
#             in resp.data
#         )
#
#     def test_uploadurl_with_fragments_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             json=ingests_post("s3://example-bukkit/helloworld.zip#fragment"),
#         )
#         assert resp.status_code == 400
#         assert (
#             b"Invalid uploadUrl:'s3://example-bukkit/helloworld.zip#fragment', 'fragment' fragment is not allowed"
#             in resp.data
#         )
#
#     def test_invalid_callback_url_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests", json=ingests_post(self.upload_url, "not-a-url")
#         )
#         assert resp.status_code == 400
#         assert b"Invalid callbackUrl:'not-a-url', is not a complete URL" in resp.data
#
#     def test_invalid_scheme_callback_url_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             json=ingests_post(self.upload_url, "s3://example.com"),
#         )
#         assert resp.status_code == 400
#         print(repr(resp.data))
#         assert (
#             b"Invalid callbackUrl:'s3://example.com', 's3' is not a supported scheme ['http', 'https']"
#             in resp.data
#         )
#
#     def test_request_allows_fragment_in_callback(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             json=ingests_post(self.upload_url, f"{self.callback_url}#fragment"),
#         )
#         assert resp.status_code == 202
#
#     def test_request_new_ingest_has_location_header(self, client):
#         resp = client.post("/storage/v1/ingests", json=ingests_post(self.upload_url))
#         assert "Location" in resp.headers
#
#         # TODO: This might need revisiting when we deploy the app behind
#         # an ALB and these paths are no longer correct.
#         new_location = resp.headers["Location"]
#         assert new_location.startswith("http://localhost/storage/v1/ingests/")
#
#     def test_successful_request_sends_to_sns(self, client, sns_client):
#         resp = client.post("/storage/v1/ingests", json=ingests_post(self.upload_url))
#
#         sns_messages = sns_client.list_messages()
#         assert len(sns_messages) == 1
#         message = sns_messages[0][":message"]
#
#         assert message["zippedBagLocation"] == {
#             "namespace": "example-bukkit",
#             "key": "helloworld.zip",
#         }
#
#         # This checks that the request ID sent to SNS is the same as
#         # the one we've been given to look up the request later.
#         assert resp.headers["Location"].endswith(message["archiveRequestId"])
#
#     def test_successful_request_sends_to_sns_with_callback(self, client, sns_client):
#         client.post(
#             "/storage/v1/ingests", json=ingests_post(self.upload_url, self.callback_url)
#         )
#
#         sns_messages = sns_client.list_messages()
#         assert len(sns_messages) == 1
#         message = sns_messages[0][":message"]
#
#         assert "callbackUrl" in message
#         assert message["callbackUrl"] == self.callback_url
#
#         resp = client.get("/storage/v1/ingests")
#         assert resp.status_code == 405
#
#     def test_request_not_json_is_badrequest(self, client):
#         resp = client.post(
#             "/storage/v1/ingests",
#             data="notjson",
#             headers={"Content-Type": "application/json"},
#         )
#         assert resp.status_code == 400
#         assert (
#             b"The browser (or proxy) sent a request that this server could not understand"
#             in resp.data
#         )
#
#
# class TestReportHealthStatus:
#     """
#     Tests for the GET /healthcheck endpoint.
#     """
#
#     def test_get_healthcheck_endpoint_is_200_OK(self, client):
#         resp = client.get("/storage/v1/healthcheck")
#         assert resp.status_code == 200
#         assert json.loads(resp.data)["status"] == "OK"
#
#
# def ingests_post(upload_url=None, callback_url=None):
#     request = {"type": "Ingest", "ingestType": {"id": "create", "type": "IngestType"}}
#     if upload_url:
#         request.update({"uploadUrl": upload_url})
#     if callback_url:
#         request.update({"callbackUrl": callback_url})
#     return request
