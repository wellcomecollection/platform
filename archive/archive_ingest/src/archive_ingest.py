# -*- encoding: utf-8 -*-
"""
Receives a message to ingest a bag giving the URL and publishes the archive event to an SNS topic.
"""

import os

import daiquiri
import json
from urllib.parse import urlparse

from wellcome_aws_utils.sns_utils import publish_sns_message
from wellcome_aws_utils.lambda_utils import log_on_error

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


def archive_bag_message(bagURL):
    """
    Generates bag archive messages.
    """
    url = urlparse(bagURL)
    if url.scheme == 's3':
        bucket = url.netloc
        key = url.path.lstrip('/')
        return {
            'namespace': bucket,
            'key': key
        }
    else:
        raise ValueError(
            f"Unrecognised url scheme: {bagURL}"
    )

@log_on_error
def main(event, _ctx=None, sns_client=None):
    logger.info(f"received {event}")

    bagURL = event['bagURL']

    message = json.dumps(archive_bag_message(bagURL))
    logger.debug(message)

    topic_arn = os.environ['INGEST_TOPIC_ARN']
    topic_name = topic_arn.split(":")[-1]

    # sns_client = sns_client or boto3.client('sns')

    publish_sns_message(
            sns_client=sns_client,
            topic_arn=topic_arn,
            message=message,
            subject=f'source: archive_ingest ({topic_name})'
    )