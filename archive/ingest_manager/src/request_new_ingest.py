# -*- encoding: utf-8

from urllib.parse import urlparse

from werkzeug.exceptions import BadRequest as BadRequestError


def create_archive_bag_message(guid, bag_url, callback_url):
    """
    Generates bag archive messages.
    """
    parsed_bag_url = urlparse(bag_url)
    if parsed_bag_url.scheme != 's3':
        raise BadRequestError(f'Unrecognised URL scheme: {bag_url!r}')

    bucket = parsed_bag_url.netloc
    key = parsed_bag_url.path.lstrip('/')

    message = {
        'archiveRequestId': guid,
        'bagLocation': {
            'namespace': bucket,
            'key': key
        }
    }

    if callback_url is not None:
        message['callbackUrl'] = callback_url

    return message
