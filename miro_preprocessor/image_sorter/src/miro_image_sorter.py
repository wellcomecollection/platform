# -*- encoding: utf-8 -*-

import csv
from io import StringIO
import json
import os

import boto3

from sorter_logic import Decision, sort_image
from sns_utils import publish_sns_message


def parse_s3_event(event):
    """
    Given an event that comes from an S3 update to a Miro metadata blob,
    return a key for the corresponding S3 object.
    """
    records = event['Records']
    assert len(records) == 1
    return records[0]['s3']['object']['key']


def fetch_s3_data(bucket, key):
    client = boto3.client('s3')
    resp = client.get_object(Bucket=bucket, Key=key)
    return resp['Body'].read()


def fetch_json_s3_data(bucket, key):
    return json.loads(fetch_s3_data(bucket, key))


def main(event, _):
    print(f'Received event: {event!r}')

    # Parse environment config
    topic_cold_store = os.environ['TOPIC_COLD_STORE']
    topic_tandem_vault = os.environ['TOPIC_TANDEM_VAULT']
    topic_catalogue_api = os.environ['TOPIC_CATALOGUE_API']
    topic_none = os.environ['TOPIC_NONE']
    topic_digital_library = os.environ['TOPIC_DIGITAL_LIBRARY']

    s3_bucket = os.environ['S3_MIRODATA_ID']
    s3_exceptions_key = os.environ['S3_EXCEPTIONS_KEY']
    s3_key = parse_s3_event(event)

    # Fetch the metadata from S3
    print(f'Bucket = {s3_bucket}, key = {s3_key}')
    print(f'Bucket = {s3_bucket}, exceptions_key = {s3_exceptions_key}')

    exceptions = fetch_s3_data(s3_bucket, s3_exceptions_key)
    print(exceptions.decode())

    data = fetch_json_s3_data(bucket=s3_bucket, key=s3_key)

    # Decide where to put it, then send the metadata to SNS
    collection = data['collection']
    image_data = data['image_data']

    decisions = sort_image(collection=collection, image_data=image_data, exceptions=csv.DictReader(StringIO(exceptions.decode())))

    topic_arns = {
        Decision.cold_store: topic_cold_store,
        Decision.tandem_vault: topic_tandem_vault,
        Decision.catalogue_api: topic_catalogue_api,
        Decision.none: topic_none,
        Decision.digital_library: topic_digital_library
    }

    for decision in decisions:
        print(f'Sorting this image into {decision}')
        publish_sns_message(topic_arn=topic_arns[decision], message=data)
