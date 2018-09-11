# -*- encoding: utf-8

import pytest
from werkzeug.exceptions import BadRequest as BadRequestError

from request_new_ingest import create_archive_bag_message


def test_bad_bag_url_is_badrequest_error(guid):
    with pytest.raises(BadRequestError, match='Unrecognised URL scheme'):
        create_archive_bag_message(
            guid=guid,
            bag_url='https://example.org',
            callback_url=None
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
        'bagLocation': {
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
