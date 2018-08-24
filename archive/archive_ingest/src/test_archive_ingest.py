# -*- encoding: utf-8 -*-

from uuid import UUID

import archive_ingest
import pytest


def test_sends_location_to_sns(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip')

    response = archive_ingest.main(event=request, sns_client=sns_client)

    id = str(UUID(response['id']))
    assert id

    assert response['location'] == f"/ingests/{id}"

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'ingestId': id,
        'namespace': 'wellcomecollection-assets-archive-ingest',
        'key': 'test-bag.zip'
    }


def test_sends_request_to_sns_with_callback(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip',
                             callback_url='https://workflow.wellcomecollection.org/callback?id=b1234567')

    response = archive_ingest.main(event=request, sns_client=sns_client)

    actual_id = str(UUID(response['id']))
    assert actual_id

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'ingestId': actual_id,
        'namespace': 'wellcomecollection-assets-archive-ingest',
        'key': 'test-bag.zip',
        'callbackUrl': 'https://workflow.wellcomecollection.org/callback?id=b1234567'
    }


def test_invalid_url_fails(sns_client):
    request = ingest_request('invalidUrl')

    with pytest.raises(ValueError, match="\[BadRequest\] Unrecognised url scheme: invalid"):
        archive_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_missing_url_fails(sns_client):
    request = { "body": {'unknownKey': 'aValue'}}

    with pytest.raises(KeyError, match="\[BadRequest\] Invalid request missing 'uploadUrl' in {'unknownKey': 'aValue'}"):
        archive_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_invalid_json_fails(sns_client):
    request = { "body": "not_json" }

    with pytest.raises(TypeError, match="\[BadRequest\] Invalid request not json: not_json"):
        archive_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def ingest_request(upload_url, callback_url=None):
    body = {
        'uploadUrl': upload_url
    }
    if callback_url:
        body['callbackUrl'] = callback_url
    return {
        'body': body,
        'path': '/ingests/'
    }
