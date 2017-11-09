# -*- encoding: utf-8 -*-

import json
from operator import itemgetter

import boto3
from moto import mock_s3
import pytest

import sierra_objects_to_s3

test_bucket_name = "bukkit"


@pytest.yield_fixture(scope="function")
def mock_s3_client():
    mock_s3().start()

    s3_client = boto3.client("s3")
    s3_client.create_bucket(Bucket=test_bucket_name, ACL="private")

    yield s3_client

    mock_s3().stop()


def test_build_from_to_params():
    assert sierra_objects_to_s3.build_from_to_params(
        '3rd of january 1982',
        '4rd of january 1982'
    ) == {'updatedDate': f'[1982-01-03T00:00:00Z,1982-01-04T00:00:00Z]'}

    assert sierra_objects_to_s3.build_from_to_params(
        '3rd of january 1982',
        False
    ) == {'updatedDate': f'[1982-01-03T00:00:00Z,]'}

    assert sierra_objects_to_s3.build_from_to_params(
        False,
        '4rd of january 1982'
    ) == {'updatedDate': f'[,1982-01-04T00:00:00Z]'}


def test_write_objects_to_s3(mock_s3_client):
    objects = sorted([
        {
            'id': '1',
            'value': 'foo'
        },
        {
            'id': '2',
            'value': 'bar'
        }
    ], key=itemgetter('id'))

    sierra_objects_to_s3.write_objects_to_s3(
        mock_s3_client,
        test_bucket_name,
        'example',
        objects
    )

    response = mock_s3_client.list_objects(
        Bucket=test_bucket_name
    )

    assert len(response['Contents']) == len(objects)

    received_objects = []

    for object in response['Contents']:
        print(object['Key'])
        response = mock_s3_client.get_object(
            Bucket=test_bucket_name,
            Key=object['Key']
        )

        parsed_body = json.loads(response['Body'].read())

        received_objects.append(parsed_body)

    assert objects == sorted(received_objects, key=itemgetter('id'))


def test_main(mock_s3_client, recorder):
    args = {
        '--url': recorder['api_url'],
        '--key': recorder['oauthkey'],
        '--sec': recorder['oauthsec'],

        '--type': '/bibs',

        '--bucket': test_bucket_name,
        '--path': 'example',

        '--from': '2013-12-10T17:16:35Z',
        '--to': '2013-12-13T21:34:35Z'
    }

    with recorder['betamax'].use_cassette('sierra_api'):
        sierra_objects_to_s3.main(args, mock_s3_client, recorder['session'])

        response = mock_s3_client.list_objects(
            Bucket=test_bucket_name
        )

        assert len(response['Contents']) == 29
