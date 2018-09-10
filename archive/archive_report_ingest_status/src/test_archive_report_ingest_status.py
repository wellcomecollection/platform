# -*- encoding: utf-8 -*-

import uuid

import archive_report_ingest_status as report_ingest_status


def test_get_returns_status(dynamodb_resource, table_name):
    guid = str(uuid.uuid4())

    table = dynamodb_resource.Table(table_name)
    table.put_item(Item={'id': guid})

    event = {
        'request_method': 'GET',
        'id': guid
    }

    response = report_ingest_status.main(
        event=event,
        dynamodb_resource=dynamodb_resource
    )
    assert response['id'] == guid


def test_get_includes_other_dynamodb_metadata(dynamodb_resource, table_name):
    guid = str(uuid.uuid4())
    item = {'id': guid, 'fooKey': 'barValue'}

    table = dynamodb_resource.Table(table_name)
    table.put_item(Item=item)

    event = {
        'request_method': 'GET',
        'id': guid
    }

    response = report_ingest_status.main(
        event=event,
        dynamodb_resource=dynamodb_resource
    )
    assert response == item
