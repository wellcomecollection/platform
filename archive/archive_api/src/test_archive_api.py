# -*- encoding: utf-8

import json


class TestReportIngestStatus:
    """
    Tests for the GET /ingests/<guid> endpoint.
    """

    def test_lookup_item(self, client, dynamodb_resource, table_name, guid):
        table = dynamodb_resource.Table(table_name)
        table.put_item(Item={'id': guid})

        resp = client.get(f'/ingests/{guid}')
        assert resp.status_code == 200
        assert json.loads(resp.data) == {'id': guid}

    def test_lookup_missing_item_is_404(self, client, guid):
        resp = client.get(f'/ingests/{guid}')
        assert resp.status_code == 404
        assert (b'No ingest found for id=%r' % guid) in resp.data

    def test_post_against_lookup_endpoint_is_405(self, client, guid):
        resp = client.post(f'/ingests/{guid}')
        assert resp.status_code == 405


class TestRequestNewIngest:
    """
    Tests for the POST /ingests endpoint.
    """

    upload_url = 's3://example-bukkit/helloworld.zip'
    callback_url = 'https://example.com/post?callback'

    def test_request_new_ingest_is_202(self, client):
        resp = client.post(
            f'/ingests',
            json=ingests_post(self.upload_url))
        assert resp.status_code == 202
        assert resp.data == b''

    def test_no_type_is_badrequest(self, client):
        resp = client.post(f'/ingests', json={})
        assert resp.status_code == 400
        assert b'No type parameter' in resp.data

    def test_invalid_type_is_badrequest(self, client):
        resp = client.post(f'/ingests', json={'type': 'UnexpectedType'})
        assert resp.status_code == 400
        assert b'Invalid type: UnexpectedType parameter' in resp.data

    def test_no_ingest_type_is_badrequest(self, client):
        resp = client.post(f'/ingests', json={'type': 'Ingest'})
        assert resp.status_code == 400
        assert b'No ingestType parameter' in resp.data

    def test_invalid_ingest_type_is_badrequest(self, client):
        resp = client.post(f'/ingests', json={'type': 'Ingest',
                                              'ingestType': {'type': 'UnexpectedIngestType'}})
        assert resp.status_code == 400
        assert b"Invalid ingestType: {'type': 'UnexpectedIngestType'} parameter" in resp.data

    def test_no_uploadurl_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post())
        assert resp.status_code == 400
        assert b'No uploadUrl parameter' in resp.data

    def test_invalid_uploadurl_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post('not-a-url'))
        assert resp.status_code == 400
        assert b"Invalid url in uploadUrl: not-a-url" in resp.data

    def test_invalid_scheme_uploadurl_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post('ftp://example-bukkit/helloworld.zip'))
        assert resp.status_code == 400
        assert b"Invalid url in uploadUrl: ftp://example-bukkit/helloworld.zip" in resp.data

    def test_uploadurl_with_fragments_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post('s3://example-bukkit/helloworld.zip#fragment'))
        assert resp.status_code == 400
        assert b"Invalid url in uploadUrl: s3://example-bukkit/helloworld.zip#fragment" in resp.data

    def test_invalid_callback_url_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post(self.upload_url, 'not-a-url'))
        assert resp.status_code == 400
        assert b"Invalid url in callbackUrl: not-a-url" in resp.data

    def test_invalid_scheme_callback_url_is_badrequest(self, client):
        resp = client.post(f'/ingests', json=ingests_post(self.upload_url, 's3://example.com'))
        assert resp.status_code == 400
        assert b"Invalid url in callbackUrl: s3://example.com" in resp.data

    def test_request_allows_fragment_in_callback(self, client):
        resp = client.post(f'/ingests', json=ingests_post(self.upload_url, f'{self.callback_url}#fragment'))
        assert resp.status_code == 202

    def test_request_new_ingest_has_location_header(self, client):
        resp = client.post(f'/ingests', json=ingests_post(self.upload_url))
        assert 'Location' in resp.headers

        # TODO: This might need revisiting when we deploy the app behind
        # an ALB and these paths are no longer correct.
        assert resp.headers['Location'].startswith('http://localhost/ingests/')

    def test_successful_request_sends_to_sns(self, client, sns_client):
        resp = client.post(f'/ingests', json=ingests_post(self.upload_url))

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][':message']

        assert message['bagLocation'] == {
            'namespace': 'example-bukkit',
            'key': 'helloworld.zip'
        }

        # This checks that the request ID sent to SNS is the same as
        # the one we've been given to look up the request later.
        assert resp.headers['Location'].endswith(message['archiveRequestId'])

    def test_successful_request_sends_to_sns_with_callback(self, client, sns_client):
        client.post(f'/ingests', json=ingests_post(self.upload_url, self.callback_url))

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][':message']

        assert 'callbackUrl' in message
        assert message['callbackUrl'] == self.callback_url

    def test_successful_request_creates_progress(self, client, dynamodb_resource, table_name):
        response = client.post(f'/ingests', json=ingests_post(self.upload_url, self.callback_url))

        assert 'Location' in response.headers
        request_id = response.headers['Location'].split('/')[-1]

        table = dynamodb_resource.Table(table_name)
        dynamo_response = table.get_item(Key={'id': request_id})
        assert 'Item' in dynamo_response
        progress = dynamo_response['Item']
        assert progress['uploadUrl'] == self.upload_url
        assert progress['callbackUrl'] == self.callback_url

    def test_get_against_request_endpoint_is_405(self, client):
        resp = client.get('/ingests')
        assert resp.status_code == 405

    def test_request_not_json_is_badrequest(self, client):
        resp = client.post(
            f'/ingests',
            data="not-json",
            headers={'Content-Type': 'application/json'})
        assert resp.status_code == 400
        assert b'Invalid json in request' in resp.data

    def test_request_not_json_content_type_is_badrequest(self, client):
        resp = client.post(
            f'/ingests',
            data=json.dumps({'uploadUrl': self.upload_url}),
            headers={'Content-Type': 'text/plain'})
        assert resp.status_code == 400
        assert b'Mimetype expected to be application/json' in resp.data


class TestReportHealthStatus:
    """
    Tests for the GET /healthcheck endpoint.
    """

    def test_get_healthcheck_endpoint_is_200_OK(self, client):
        resp = client.get(f'/healthcheck')
        assert resp.status_code == 200
        assert json.loads(resp.data) == {'status': 'OK'}


def ingests_post(upload_url=None, callback_url=None):
    request = {
        "type": "Ingest",
        "ingestType": {
            "id": "create",
            "type": "IngestType"
        }
    }
    if upload_url:
        request.update({'uploadUrl': upload_url})
    if callback_url:
        request.update({'callbackUrl': callback_url})
    return request
