# -*- encoding: utf-8

from urllib.parse import urlparse

import daiquiri
from wellcome_aws_utils.sns_utils import publish_sns_message
from werkzeug.exceptions import BadRequest as BadRequestError


logger = daiquiri.getLogger()


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
        'zippedBagLocation': {
            'namespace': bucket,
            'key': key
        }
    }

    if callback_url is not None:
        message['callbackUrl'] = callback_url

    return message


def send_new_ingest_request(sns_client, topic_arn, ingest_request_id, upload_url, callback_url):
    """
    Create a new ingest request, and send a notification to SNS.
    """
    message = create_archive_bag_message(
        guid=ingest_request_id,
        bag_url=upload_url,
        callback_url=callback_url
    )
    logger.debug('SNS message=%r', message)

    topic_name = topic_arn.split(':')[-1]

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=message,
        subject=f'source: archive_api ({topic_name})'
    )
    logger.debug('Published %r to %r', message, topic_arn)

    return ingest_request_id
