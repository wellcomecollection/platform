# -*- encoding: utf-8

import pytest
from datetime import (datetime, timezone, timedelta)

from botocore.exceptions import ClientError

from ingests import (
    IngestProgress,
    create_ingest_progress
)

bag_url = 's3://example-bukkit/foo/bar.zip'
callback_url = 'https://example.com/post?callback'


def test_creates_ingest_progress_without_callback(dynamodb_resource, table_name, guid):
    create_ingest_progress(
        IngestProgress(guid, bag_url),
        dynamodb_resource,
        table_name)

    expected_item = initialised_progress_item(guid, bag_url)

    assert_stored_progress(expected_item, dynamodb_resource, table_name)


def test_creates_ingest_progress_with_callback(dynamodb_resource, table_name, guid):
    create_ingest_progress(
        IngestProgress(guid, bag_url, callback_url),
        dynamodb_resource,
        table_name)

    expected_item = initialised_progress_item(guid, bag_url, callback_url)

    assert_stored_progress(expected_item, dynamodb_resource, table_name)


def test_raises_if_id_is_invalid(dynamodb_resource, table_name):
    with pytest.raises(ValueError, match='is not a valid ID'):
        create_ingest_progress(
            IngestProgress("", bag_url, callback_url),
            dynamodb_resource,
            table_name)


def test_raises_if_id_is_already_saved(dynamodb_resource, table_name, guid):
    with pytest.raises(ValueError, match=f"Cannot create IngestProgress, id already exists '{guid}'."):
        create_ingest_progress(
            IngestProgress(guid, bag_url, callback_url),
            dynamodb_resource,
            table_name)

        create_ingest_progress(
            IngestProgress(guid, bag_url, callback_url),
            dynamodb_resource,
            table_name)


def test_raises_for_all_other_errors(dynamodb_resource, guid):
    with pytest.raises(ClientError) as err:
        create_ingest_progress(
            IngestProgress(guid, bag_url, callback_url),
            dynamodb_resource=dynamodb_resource,
            table_name='DoesNotExist'
        )
    assert err.value.response['Error']['Code'] == 'ResourceNotFoundException'


def initialised_progress_item(guid, upload_url, callback_url=None):
    item = {
        '@context': 'https://api.wellcomecollection.org/storage/v1/context.json',
        'id': guid,
        'type': 'Ingest',
        'ingestType': {
            'id': 'create',
            'type': 'IngestType'
        },
        'uploadUrl': upload_url,
        'description': 'Ingest requested',
        'createdDate': '@recent',
        'lastModifiedDate': '@recent'
    }
    if callback_url is not None:
        item.update({'callbackUrl': callback_url})
    return item


def assert_stored_progress(expected_item, dynamodb_resource, table_name):
    table = dynamodb_resource.Table(table_name)
    result = table.get_item(Key={'id': expected_item['id']})
    saved_item = result['Item']

    assert_dynamic_fields(saved_item, expected_item)
    assert saved_item == expected_item


def assert_dynamic_fields(actual_dict, expected_dict):
    for key, value in expected_dict.items():
        if value == '@recent':
            try:
                date_str = actual_dict.get(key, "")
                assert_iso_date_is_recent(date_str)
            except ValueError:
                raise ValueError(f"Expected recent ISO date {key}: '{date_str}' in {actual_dict}")
            expected_dict[key] = actual_dict[key]


def assert_iso_date_is_recent(actual_date_str, seconds_delta=1):
    # We expect a UTC ISO 8601 time with a timezone portion that includes colons.
    # Python < 3.7 strptime('%z') cannot parse the colons so replace +00:00 with a timezone
    # that can be parsed ('+0000').
    #
    # This step should *not* be needed in python >= 3.7
    actual_date_str = actual_date_str.replace('+00:00', '+0000')

    actual_date = datetime.strptime(actual_date_str, '%Y-%m-%dT%H:%M:%S.%f%z')
    assert datetime.now(timezone.utc) - actual_date < timedelta(seconds=seconds_delta)
