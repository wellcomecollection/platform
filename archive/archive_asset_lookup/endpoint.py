import os
import tempfile
import logging
import boto3
import botocore
import settings
from boto3.dynamodb.conditions import Key


def lambda_handler(event, context):

    # setup logging and AWS clients
    logger = logging.getLogger()
    dynamodb = boto3.resource('dynamodb', region_name=settings.AWS_REGION)
    s3 = boto3.resource('s3', region_name=settings.AWS_REGION)

    logger.info('got event{}'.format(event))

    # validate event - check if we have an identifier
    if not event_is_valid(event):
        logger.info('invalid event')
        return "invalid request"

    identifier = event["identifier"]

    # query our asset lookup table and get a response

    table = dynamodb.Table(settings.TABLE_NAME)

    response = table.query(
        KeyConditionExpression=Key('identifier').eq(identifier)
    )

    data_items = response['Items']

    # validate if we have any items in our response
    if not data_items_populated(data_items):
        logger.info('no data for identifier')
        return "no data for identifier"

    # we are just expecting to use a single row
    if len(data_items) > 1:
        logger.info('multiple results for identifier')
        return "multiple results for identifier"

    # should just have a single item
    data = data_items[0]

    # validate if the item has the fields we are expecting to use
    if not data_is_valid(data):
        logger.info('invalid data')
        return "invalid data"

    # get a temporary file name to use (usage is safe for windows too)
    # the file is created and requested not to be deleted, then we close it
    # so it can be written to without lock interference.

    temp_file = tempfile.NamedTemporaryFile(delete=False)
    temp_file.close()

    # attempt to download the content pointed to by the table into the file
    try:
        s3.Bucket(settings.BUCKET_NAME) \
            .download_file(data['s3'], temp_file.name)
    except botocore.exceptions.ClientError as e:
        # check for 404 or something worse
        if e.response['Error']['Code'] == "404":
            logger.info("object does not exist.")
            return "could not load storage manifest from s3"
        else:
            raise

    # read the contents from the temporary file
    file_contents = None

    with open(temp_file.name, 'r') as storage_manifest:
        file_contents = storage_manifest.read()

    # remove the file (we asked for this not to be automatically done)
    os.unlink(temp_file.name)

    return file_contents


def event_is_valid(event):
    return 'identifier' in event


def data_items_populated(data_items):
    return len(data_items) > 0


def data_is_valid(data):
    return 's3' in data
