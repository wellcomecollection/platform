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

    def test_request_new_ingest_is_202(self, client):
        resp = client.post(f'/ingests', data={
            'uploadUrl': 's3://example-bukkit/helloworld.zip',
        })
        assert resp.status_code == 202

    def test_get_against_request_endpoint_is_405(self, client):
        resp = client.get('/ingests')
        assert resp.status_code == 405
