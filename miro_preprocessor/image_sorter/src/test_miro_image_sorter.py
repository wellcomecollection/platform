# -*- encoding: utf-8 -*-

import json

import boto3
from moto import mock_s3

from miro_image_sorter import fetch_s3_metadata, parse_s3_event


def test_parse_s3_event(s3_put_event):
    assert parse_s3_event(s3_put_event) == 'HappyFace.jpg'


@mock_s3
def test_fetch_s3_metadata():
    client = boto3.client('s3', region_name='eu-west-1')
    client.create_bucket(Bucket='miro-data')

    metadata = {
        'image_calc_no': 'V0000001',
        'image_title': 'A morsel of metadata about mussels',
    }

    client.put_object(
        Bucket='miro-data',
        Key='metadata.json',
        Body=json.dumps(metadata)
    )

    assert fetch_s3_metadata(bucket='miro-data', key='metadata.json') == metadata
