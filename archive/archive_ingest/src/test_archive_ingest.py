# -*- encoding: utf-8 -*-

import os
import pytest
import archive_ingest
import json

@pytest.yield_fixture(autouse=True)
def test_sends_location_to_sns(sns_client, topic_arn):
    os.environ['INGEST_TOPIC_ARN'] = topic_arn
    request = {'bagURL': 's3://wellcomecollection-assets-archive-ingest/test-bag.zip'}

    archive_ingest.main(event=request, sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == json.dumps({
        'namespace': 'wellcomecollection-assets-archive-ingest',
        'key': 'test-bag.zip'
    })
