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

    try:
        guid = event['id']
    except KeyError:
        raise ValueError(
            'Expected "id" in request, got %r' % event
        )

    table = dynamodb_resource.Table(table_name)
    item = table.get_item(
        Key={'id': guid}
    )

    # TODO: @@AWLC The correct response is surely a 404 if the item isn't
    # found, but this will throw a KeyError.  How do we get a 404 to be
    # emitted by API Gateway?
    #
    # Similarly above, the absence of an 'id' should be a 400, and doing
    # something other than a GET should be a 405, not a 500.
    #
    return item['Item']
