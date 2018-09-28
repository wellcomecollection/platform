# -*- encoding: utf-8

import pytest

from ingests import report_ingest_status


def test_finds_present_status(dynamodb_resource, table_name, guid):
    table = dynamodb_resource.Table(table_name)
    table.put_item(Item={'id': guid})

    result = report_ingest_status(
        dynamodb_resource=dynamodb_resource,
        table_name=table_name,
        guid=guid
    )
    assert result == {'id': guid}


def test_gets_all_attributes_from_ddb(dynamodb_resource, table_name, guid):
    item = {'id': guid, 'fooKey': 'barValue'}

    table = dynamodb_resource.Table(table_name)
    table.put_item(Item=item)

    result = report_ingest_status(
        dynamodb_resource=dynamodb_resource,
        table_name=table_name,
        guid=guid
    )
    assert result == item


def test_throws_valueerror_for_missing_status(
    dynamodb_resource,
    table_name,
    guid
):
    with pytest.raises(ValueError, match='No ingest found for id'):
        report_ingest_status(
            dynamodb_resource=dynamodb_resource,
            table_name=table_name,
            guid=guid
        )
