# -*- encoding: utf-8

import pytest
from werkzeug.exceptions import BadRequest as BadRequestError

from request_new_ingest import (
    create_archive_bag_message,
    send_new_ingest_request
)


def test_creates_bag_message_without_callback_url(guid):
    bag_url = 's3://example-bukkit/foo/bar.zip'

    resp = create_archive_bag_message(
        guid=guid,
        bag_url=bag_url,
        callback_url=None
    )
    assert resp == {
        'archiveRequestId': guid,
        'zippedBagLocation': {
            'namespace': 'example-bukkit',
            'key': 'foo/bar.zip'
        }
    }


def test_creates_bag_message_includes_callback_url(guid):
    callback_url = 'https://example.com/'

    resp = create_archive_bag_message(
        guid=guid,
        bag_url='s3://example-bukkit/foo/bar.zip',
        callback_url=callback_url
    )
    assert resp['callbackUrl'] == callback_url


def test_sends_notification_to_sns(sns_client, topic_arn, guid):
    send_new_ingest_request(
        sns_client=sns_client,
        topic_arn=topic_arn,
        ingest_request_id=guid,
        upload_url='s3://example-bukkit/foo/bar.zip',
        callback_url=None
    )

    assert len(sns_client.list_messages()) == 1


def test_sends_notification_to_sns_with_callback_url(sns_client, topic_arn, guid):
    callback_url = 'https://callback.com/?example'

    send_new_ingest_request(
        sns_client=sns_client,
        topic_arn=topic_arn,
        ingest_request_id=guid,
        upload_url='s3://example-bukkit/foo/bar.zip',
        callback_url=callback_url
    )

    sns_messages = sns_client.list_messages()
    assert len(sns_messages) == 1
    assert 'callbackUrl' in sns_messages[0][':message']
    assert sns_messages[0][':message']['callbackUrl'] == callback_url


def test_returns_new_location_no_path(sns_client, topic_arn, guid):
    resp = send_new_ingest_request(
        sns_client=sns_client,
        topic_arn=topic_arn,
        ingest_request_id=guid,
        upload_url='s3://example-bukkit/foo/bar.zip',
        callback_url=None
    )

    assert isinstance(resp, str)


def test_non_s3_bag_url_is_badrequest_error(guid):
    with pytest.raises(BadRequestError, match='Unrecognised URL scheme'):
        create_archive_bag_message(
            guid=guid,
            bag_url='https://example.org',
            callback_url=None
        )
