# -*- encoding: utf-8 -*-

import os
import uuid

import pytest

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


def test_throws_valueerror_if_called_with_post_event():
    event = {
        'request_method': 'POST'
    }

    with pytest.raises(ValueError, match='Expected request_method=GET'):
        report_ingest_status.main(event=event)


def test_throws_keyerror_if_no_table_name_set():
    # This environment variable is populated by the run_around_tests()
    # fixture, which has autouse=True.
    del os.environ['TABLE_NAME']
    assert 'TABLE_NAME' not in os.environ

    event = {
        'request_method': 'GET',
        'id': str(uuid.uuid4())
    }

    with pytest.raises(KeyError, match='TABLE_NAME'):
        report_ingest_status.main(event=event)


def test_throws_valueerror_if_no_id_in_request():
    event = {
        'request_method': 'GET'
    }

    with pytest.raises(ValueError, match='Expected "id" in request'):
        report_ingest_status.main(event=event)
