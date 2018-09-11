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

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


class BadRequestError(ValueError):
    """
    Raised if the user makes a malformed request.  Becomes an HTTP 400 error.
    """
    prefix = os.environ.get('error_bad_request', 'BadRequest')
    def __init__(self, message):
        super().__init__(f'[{self.prefix}] {message}')


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
        raise BadRequestError('Expected "id" in request, got {event!r}')

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
