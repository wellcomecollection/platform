# -*- encoding: utf-8 -*-

import os
import archive_ingest
import pytest


def test_sends_location_to_sns(sns_client, topic_arn):
    os.environ['INGEST_TOPIC_ARN'] = topic_arn
    request = {'bagURL': 's3://wellcomecollection-assets-archive-ingest/test-bag.zip'}

    response = archive_ingest.handler(event=request, sns_client=sns_client)

    assert response == {'received': 'OK'}

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == {
        'namespace': 'wellcomecollection-assets-archive-ingest',
        'key': 'test-bag.zip'
    }


def test_invalid_url_fails(sns_client, topic_arn):
    with pytest.raises(ValueError, match="Unrecognised url scheme: invalid"):
        os.environ['INGEST_TOPIC_ARN'] = topic_arn
        request = {'bagURL': 'invalid'}

        archive_ingest.handler(event=request, sns_client=sns_client)
    assert len(sns_client.list_messages()) == 0


def test_missing_url_fails(sns_client, topic_arn):
    with pytest.raises(ValueError, match="Invalid request missing bagURL"):
        os.environ['INGEST_TOPIC_ARN'] = topic_arn
        request = {'invalid': 'invalid'}

        archive_ingest.handler(event=request, sns_client=sns_client)
    assert len(sns_client.list_messages()) == 0


def test_invalid_json_fails(sns_client, topic_arn):
    with pytest.raises(ValueError, match="Invalid request not json dict: invalid-json"):
        os.environ['INGEST_TOPIC_ARN'] = topic_arn
        request = "invalid-json"

        archive_ingest.handler(event=request, sns_client=sns_client)
    assert len(sns_client.list_messages()) == 0
