import boto3
import botocore
import json
import os
import settings
import tempfile
from boto3.dynamodb.conditions import Key


def main(event, context):
    dynamodb = boto3.resource('dynamodb', region_name=settings.REGION)
    s3 = boto3.resource('s3', region_name=settings.REGION)
    lookup(dynamodb, s3, event)


def wrapResult(message, success, status=500):
    statusCode = 200
    if success and status != 500:
        statusCode = status

    if not success:
        statusCode = status

    body = None
    if success and statusCode == 200:
        body = json.dumps(message)
    else:
        print(message)
        body = json.dumps({
            'message': message
        })

    return {
        'statusCode': statusCode,
        'body': body
    }


def lookup(dynamodb, s3, event):
    print('got event{}'.format(event))

    # validate event - check if we have an id
    if not event_is_valid(event):
        return wrapResult("invalid request", False, 400)

    identifier = event["pathParameters"]["id"]

    if identifier == "":
        return wrapResult("invalid request", False, 400)

    # query our asset lookup table and get a response

    bucket_name = settings.BUCKET_NAME

    table_name = settings.TABLE_NAME

    table = dynamodb.Table(table_name)

    response = table.query(
        KeyConditionExpression=Key('id').eq(identifier)
    )

    data_items = response['Items']

    # validate if we have any items in our response
    if not data_items_populated(data_items):
        return wrapResult("no data for id", True, 404)

    # we are just expecting to use a single row
    if len(data_items) > 1:
        return wrapResult("multiple results for id", False)

    # should just have a single item
    data = data_items[0]

    # validate if the item has the fields we are expecting to use
    if not data_is_valid(data):
        return wrapResult("invalid data", False)

    # get a temporary file name to use (usage is safe for windows too)
    # the file is created and requested not to be deleted, then we close it
    # so it can be written to without lock interference.

    temp_file = tempfile.NamedTemporaryFile(delete=False)
    temp_file.close()

    storage_manifest_key = data['s3key']

    # attempt to download the content pointed to by the table into the file
    try:
        s3.Bucket(bucket_name) \
            .download_file(storage_manifest_key, temp_file.name)
    except botocore.exceptions.ClientError as e:
        # check for 404 or something worse
        if e.response['Error']['Code'] == "404":
            return wrapResult("could not load storage manifest from s3", False)
        else:
            raise

    # read the contents from the temporary file
    file_contents = None

    with open(temp_file.name, 'r') as storage_manifest:
        file_contents = storage_manifest.read()

    # remove the file (we asked for this not to be automatically done)
    os.unlink(temp_file.name)

    return wrapResult(file_contents, True)


def event_is_valid(event):
    return 'pathParameters' in event \
           and event['pathParameters'] is not None \
           and 'id' in event['pathParameters']


def data_items_populated(data_items):
    return len(data_items) > 0


def data_is_valid(data):
    return 's3key' in data
