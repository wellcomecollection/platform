# -*- encoding: utf-8 -*-
"""
Receives a message to ingest a bag giving the URL and publishes the archive event to an SNS topic.
"""

import os
import uuid

import boto3
import daiquiri

from urllib.parse import urlparse
from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_aws_utils.sns_utils import publish_sns_message

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


def post_ingest_request(event, sns_client):
    topic_arn = os.environ['TOPIC_ARN']

    request = event['body']
    path = event.get('path', '')

    try:
        upload_url = request['uploadUrl']
        callback_url = request.get('callbackUrl', None)
    except TypeError:
        raise TypeError(f"[BadRequest] Invalid request not json: {request}")
    except KeyError as keyError:
        raise KeyError(f"[BadRequest] Invalid request missing '{keyError.args[0]}' in {request}")

    ingest_request_id = str(uuid.uuid4())
    logger.debug('ingest_request_id: %r', ingest_request_id)

    message = archive_bag_message(ingest_request_id, upload_url, callback_url)
    logger.debug(f"sns-message: {message}")

    topic_name = topic_arn.split(":")[-1]

    sns_client = sns_client or boto3.client('sns')

    publish_sns_message(
        sns_client=sns_client,
        topic_arn=topic_arn,
        message=message,
        subject=f"source: archive_ingest ({topic_name})"
    )
    logger.debug(f"published: {message} to {topic_arn}")

    return {
        'id': ingest_request_id,
        'location': join_url((path, ingest_request_id))
    }


def get_ingest_status(event, dynamodb_resource):
    dynamodb_resource = dynamodb_resource or boto3.resource('dynamodb')
    table_name = os.environ['TABLE_NAME']
    id = event['id']

    table = dynamodb_resource.Table(table_name)

    item = table.get_item(
        Key={'id': id}
    )
    return item['Item']


def archive_bag_message(archive_request_id, bag_url, callback_url):
    """
    Generates bag archive messages.
    """
    url = urlparse(bag_url)
    if url.scheme == 's3':
        bucket = url.netloc
        key = url.path.lstrip('/')
        msg = {
            'archiveRequestId': archive_request_id,
            'bagLocation': {
                'namespace': bucket,
                'key': key
            }
        }
        if callback_url:
            msg['callbackUrl'] = callback_url
        return msg
    else:
        raise ValueError(f"[BadRequest] Unrecognised url scheme: {bag_url}")


def join_url(path_segments):
    return '/' + '/'.join(path_segment.strip('/') for path_segment in path_segments)


@log_on_error
def main(event, context=None, dynamodb_resource=None, sns_client=None):
    logger.debug('received %r', event)

    request_method = event.get('request_method', 'POST')

    if request_method.upper() == 'POST':
        return post_ingest_request(event, sns_client)
    elif request_method.upper() == 'GET':
        return get_ingest_status(event, dynamodb_resource)
