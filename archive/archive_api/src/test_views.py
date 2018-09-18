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

    def test_request_new_ingest_is_202(self, client):
        resp = client.post(f'/ingests', data={'uploadUrl': self.upload_url})
        assert resp.status_code == 202

    def test_no_uploadurl_is_badrequest(self, client):
        resp = client.post(f'/ingests')
        assert resp.status_code == 400
        assert b'No uploadUrl parameter' in resp.data

    def test_request_new_ingest_has_location_header(self, client):
        resp = client.post(f'/ingests', data={'uploadUrl': self.upload_url})
        assert 'Location' in resp.headers

        # TODO: This might need revisiting when we deploy the app behind
        # an ALB and these paths are no longer correct.
        assert resp.headers['Location'].startswith('http://localhost/ingests/')

    def test_successful_request_sends_to_sns(self, client, sns_client):
        resp = client.post(f'/ingests', data={'uploadUrl': self.upload_url})

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
        callback_url = 'https://example.com/post?callback'
        resp = client.post(f'/ingests', data={
            'uploadUrl': self.upload_url,
            'callbackUrl': callback_url
        })

        sns_messages = sns_client.list_messages()
        assert len(sns_messages) == 1
        message = sns_messages[0][':message']

        assert 'callbackUrl' in message
        assert message['callbackUrl'] == callback_url

    def test_get_against_request_endpoint_is_405(self, client):
        resp = client.get('/ingests')
        assert resp.status_code == 405


class TestReportHealthStatus:
    """
    Tests for the GET /healthcheck endpoint.
    """

    def test_get_healthcheck_endpoint_is_200_OK(self, client):
        resp = client.get(f'/healthcheck')
        assert resp.status_code == 200
        assert json.loads(resp.data) == {'status': 'OK'}
