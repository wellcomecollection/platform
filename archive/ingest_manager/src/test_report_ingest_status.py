# -*- encoding: utf-8

from report_ingest_status import report_ingest_status


def test_finds_present_status(dynamodb_resource, table_name, guid):
    table = dynamodb_resource.Table(table_name)
    table.put_item(Item={'id': guid})

    result = report_ingest_status(
        dynamodb_resource=dynamodb_resource,
        table_name=table_name,
        guid=guid
    )
    assert result == {'id': guid}
