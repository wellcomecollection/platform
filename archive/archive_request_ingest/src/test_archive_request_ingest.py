# -*- encoding: utf-8 -*-

import os
from uuid import UUID

import pytest

import archive_request_ingest as request_ingest
from archive_request_ingest import BadRequestError, MethodNotAllowedError


def test_post_sends_location_to_sns(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip')

    response = request_ingest.main(event=request, sns_client=sns_client)

    id = str(UUID(response['id']))
    assert id

    assert response['location'] == f"/ingests/{id}"

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'archiveRequestId': id,
        'bagLocation': {
            'namespace': 'wellcomecollection-assets-archive-ingest',
            'key': 'test-bag.zip'
        }
    }


def test_sends_request_to_sns_with_callback(sns_client, topic_arn):
    request = ingest_request(upload_url='s3://wellcomecollection-assets-archive-ingest/test-bag.zip',
                             callback_url='https://workflow.wellcomecollection.org/callback?id=b1234567')

    response = request_ingest.main(event=request, sns_client=sns_client)

    actual_id = str(UUID(response['id']))
    assert actual_id

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'archiveRequestId': actual_id,
        'bagLocation': {
            'namespace': 'wellcomecollection-assets-archive-ingest',
            'key': 'test-bag.zip'
        },
        'callbackUrl': 'https://workflow.wellcomecollection.org/callback?id=b1234567'
    }


def test_invalid_url_fails(sns_client, topic_arn):
    request = ingest_request('invalidUrl')

    with pytest.raises(BadRequestError, match="\[BadRequest\] Unrecognised url scheme: 'invalidUrl'"):
        request_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_missing_url_fails(sns_client, topic_arn):
    request = {
        'body': {'unknownKey': 'aValue'},
        'request_method': 'POST'
    }

    with pytest.raises(BadRequestError, match="\[BadRequest\] Invalid request missing 'uploadUrl' in {'unknownKey': 'aValue'}"):
        request_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_invalid_json_fails(sns_client, topic_arn):
    request = {
        'body': 'not_json',
        'request_method': 'POST'
    }

    with pytest.raises(BadRequestError, match="\[BadRequest\] Invalid request not json: 'not_json'"):
        request_ingest.main(event=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_throws_error_if_called_with_get_event():
    event = {
        'request_method': 'GET'
    }

    with pytest.raises(MethodNotAllowedError,
                       match='Expected request_method=POST'):
        request_ingest.main(event=event)


def test_throws_keyerror_if_no_topic_arn_set():
    assert 'TOPIC_ARN' not in os.environ

    event = ingest_request(upload_url='s3://bukkit/example.zip')

    with pytest.raises(KeyError, match='TOPIC_ARN'):
        request_ingest.main(event=event)


def ingest_request(upload_url, callback_url=None):
    body = {
        'uploadUrl': upload_url
    }
    if callback_url:
        body['callbackUrl'] = callback_url
    return {
        'body': body,
        'request_method': 'POST',
        'path': '/ingests/'
    }
