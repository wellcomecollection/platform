# -*- encoding: utf-8

import json


class TestReportIngestStatus:

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
