# -*- encoding: utf-8 -*-
"""
Receives a message to ingest a bag giving the URL and publishes the archive event to an SNS topic.
"""

import os
from urllib.parse import urlparse

import boto3
import daiquiri

from wellcome_aws_utils.sns_utils import publish_sns_message
from wellcome_aws_utils.lambda_utils import log_on_error

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


def archive_bag_message(bag_url):
    """
    Generates bag archive messages.
    """
    url = urlparse(bag_url)
    if url.scheme == 's3':
        bucket = url.netloc
        key = url.path.lstrip('/')
        return {
            'namespace': bucket,
            'key': key
        }
    else:
        raise ValueError(f"Unrecognised url scheme: {bag_url}")


@log_on_error
def handler(request, _ctx=None, sns_client=None):
    logger.debug('received %r', request)

    topic_arn = os.environ['TOPIC_ARN']

    try:
        upload_url = request['uploadUrl']
    except TypeError:
        raise TypeError(f"Invalid request not json: {request}")
    except KeyError:
        raise KeyError(f"Invalid request missing 'uploadUrl' in {request}")

    message = archive_bag_message(upload_url)
    logger.debug(f"sns-message: {message}")

    topic_name = topic_arn.split(":")[-1]

    sns_client = sns_client or boto3.client('sns')

    publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message,
            subject=f'source: archive_ingest ({topic_name})'
    )
    logger.debug(f"published: {message} to {topic_arn}")

    response = {'received': 'OK'}
    return response
