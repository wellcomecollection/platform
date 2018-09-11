# -*- encoding: utf-8 -*-
"""
This Lambda receives a GET request to return the state of an ingest.

Quoting from RFC 002 at commit ea310c1 on master:

    Request:

    GET /ingests/xx-xx-xx-xx

    Response:

    {
      "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
      "id": "{guid}",
      "type": "Ingest",
      "ingestType": {
        "id": "create",
        "type": "IngestType"
      },
      "uploadUrl": "s3://source-bucket/source-path/source-bag.zip",
      "callbackUrl": "https://workflow.wellcomecollection.org/callback?id=b1234567",
      "bag": {
        "id": "{id}",
        "type": "Bag"
      },
      "result": {
        "id": "success",
        "type": "IngestResult"
      },
      "events": [ ... ]
    }

"""

import os

import boto3
import daiquiri
from wellcome_aws_utils.lambda_utils import log_on_error

from api_gateway_errors import (
    BadRequestError,
    MethodNotAllowedError,
    NotFoundError
)

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


@log_on_error
def main(event, context=None, dynamodb_resource=None, sns_client=None):
    logger.debug('received %r', event)

    table_name = os.environ['TABLE_NAME']
    dynamodb_resource = dynamodb_resource or boto3.resource('dynamodb')

    request_method = event['request_method']
    if request_method != 'GET':
        raise MethodNotAllowedError(
            'Expected request_method=GET, got {request_method!r}'
        )

    try:
        guid = event['id']
    except KeyError:
        raise BadRequestError('Expected "id" in request, got {event!r}')

    table = dynamodb_resource.Table(table_name)
    item = table.get_item(
        Key={'id': guid}
    )

    try:
        return item['Item']
    except KeyError:
        raise NotFoundError('No ingest process with id={guid!r}')
