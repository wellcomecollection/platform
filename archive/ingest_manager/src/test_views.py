# -*- encoding: utf-8

import json


class TestReportIngestStatus:

    def test_lookup_item(self, client, dynamodb_resource, table_name, guid):
        table = dynamodb_resource.Table(table_name)
        table.put_item(Item={'id': guid})

        resp = client.get(f'/ingests/{guid}')
        assert resp.status_code == 200
        assert json.loads(resp.data) == {'id': guid}
