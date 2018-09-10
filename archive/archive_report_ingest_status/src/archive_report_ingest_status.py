# -*- encoding: utf-8 -*-
"""
Receives a message to ingest a bag giving the URL and publishes the archive event to an SNS topic.
"""

import os

import boto3
import daiquiri

from wellcome_aws_utils.lambda_utils import log_on_error

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


def get_ingest_status(event, dynamodb_resource, table_name):
    guid = event['id']
    table = dynamodb_resource.Table(table_name)

    item = table.get_item(
        Key={'id': guid}
    )
    return item['Item']


@log_on_error
def main(event, context=None, dynamodb_resource=None, sns_client=None):
    logger.debug('received %r', event)

    table_name = os.environ['TABLE_NAME']
    dynamodb_resource = dynamodb_resource or boto3.resource('dynamodb')

    request_method = event['request_method']
    if request_method != 'GET':
        raise ValueError(
            'Expected request_method=GET, got %r' % request_method
        )

    return get_ingest_status(event, dynamodb_resource, table_name=table_name)
