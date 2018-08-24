# -*- encoding: utf-8 -*-

import archive_ingest
import pytest


def test_sends_location_to_sns(sns_client, topic_arn):
    request = {'uploadUrl': 's3://wellcomecollection-assets-archive-ingest/test-bag.zip'}

    response = archive_ingest.handler(request=request, sns_client=sns_client)

    assert response == {'received': 'OK'}

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'namespace': 'wellcomecollection-assets-archive-ingest',
        'key': 'test-bag.zip'
    }


def test_invalid_url_fails(sns_client):
    request = {'uploadUrl': 'invalid'}

    with pytest.raises(ValueError, match="Unrecognised url scheme: invalid"):
        archive_ingest.handler(request=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_missing_url_fails(sns_client):
    request = {'unknownKey': 'aValue'}

    with pytest.raises(KeyError, match="Invalid request missing 'uploadUrl' in {'unknownKey': 'aValue'}"):
        archive_ingest.handler(request=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0


def test_invalid_json_fails(sns_client):
    request = "not_json"

    with pytest.raises(TypeError, match="Invalid request not json: not_json"):
        archive_ingest.handler(request=request, sns_client=sns_client)

    assert len(sns_client.list_messages()) == 0
