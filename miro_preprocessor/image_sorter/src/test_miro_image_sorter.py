# -*- encoding: utf-8 -*-

import json

import boto3
from moto import mock_s3
import os

from miro_image_sorter import fetch_s3_data, parse_s3_event, main


def test_parse_s3_event(s3_put_event):
    assert parse_s3_event(s3_put_event) == 'metadata.json'


@mock_s3
def test_fetch_s3_data():
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

    assert fetch_s3_data(bucket='miro-data', key='metadata.json') == metadata


def _get_msg(sqs_client, queue_url):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']

    return json.loads(
        json.loads(message_body)['default']
    )


def collection_image_data(**kwargs):
    image_data = {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "Y",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "Y",
        "image_innopac_id": "12345678",
        "image_cleared": "Y",
        "image_use_restrictions": "CC-BY"
    }
    collection = 'images-M'
    if 'collection' in kwargs.keys():
        collection = kwargs.pop('collection')
    image_data.update(kwargs)

    return {
        "collection": collection,
        "image_data": image_data
    }


def _setup_os_environ(bucket_name, sns_sqs):
    os.environ['S3_MIRODATA_ID'] = bucket_name
    os.environ['TOPIC_COLD_STORE'] = sns_sqs["cold_store"]["topic"]
    os.environ['TOPIC_TANDEM_VAULT'] = sns_sqs["tandem_vault"]["topic"]
    os.environ['TOPIC_CATALOGUE_API'] = sns_sqs["catalogue_api"]["topic"]
    os.environ['TOPIC_NONE'] = sns_sqs["none"]["topic"]
    os.environ['TOPIC_DIGITAL_LIBRARY'] = sns_sqs["digital_library"]["topic"]


@mock_s3
def test_image_sorter_catalogue_api_digital_library(image_sorter_sns_sqs, s3_put_event):
    bucket_name = "miro-data"

    sqs_client = boto3.client('sqs')

    sns_sqs = image_sorter_sns_sqs
    _setup_os_environ(bucket_name, sns_sqs)

    s3_client = boto3.client('s3', region_name='eu-west-1')
    s3_client.create_bucket(Bucket=bucket_name)

    metadata = collection_image_data()

    s3_client.put_object(
        Bucket=bucket_name,
        Key='metadata.json',
        Body=json.dumps(metadata)
    )

    main(s3_put_event, None)

    catalogue_api_msg = _get_msg(sqs_client, sns_sqs["catalogue_api"]["queue"])
    digital_library_msg = _get_msg(sqs_client, sns_sqs["digital_library"]["queue"])

    assert catalogue_api_msg == metadata
    assert digital_library_msg == metadata


@mock_s3
def test_image_sorter_tandem_vault(image_sorter_sns_sqs, s3_put_event):
    bucket_name = "miro-data"

    sqs_client = boto3.client('sqs')

    sns_sqs = image_sorter_sns_sqs
    _setup_os_environ(bucket_name, sns_sqs)

    s3_client = boto3.client('s3', region_name='eu-west-1')
    s3_client.create_bucket(Bucket=bucket_name)

    metadata = collection_image_data(
        collection='images-L', image_library_dept="Public programmes")

    s3_client.put_object(
        Bucket=bucket_name,
        Key='metadata.json',
        Body=json.dumps(metadata)
    )

    main(s3_put_event, None)

    tandem_value_msg = _get_msg(sqs_client, sns_sqs["tandem_vault"]["queue"])

    assert tandem_value_msg == metadata


@mock_s3
def test_image_sorter_cold_store(image_sorter_sns_sqs, s3_put_event):
    bucket_name = "miro-data"

    sqs_client = boto3.client('sqs')

    sns_sqs = image_sorter_sns_sqs
    _setup_os_environ(bucket_name, sns_sqs)

    s3_client = boto3.client('s3', region_name='eu-west-1')
    s3_client.create_bucket(Bucket=bucket_name)

    metadata = collection_image_data(
        collection='images-L', image_library_dept="Archives and Manuscripts")

    s3_client.put_object(
        Bucket=bucket_name,
        Key='metadata.json',
        Body=json.dumps(metadata)
    )

    main(s3_put_event, None)

    cold_store_msg = _get_msg(sqs_client, sns_sqs["cold_store"]["queue"])

    assert cold_store_msg == metadata


@mock_s3
def test_image_sorter_none(image_sorter_sns_sqs, s3_put_event):
    bucket_name = "miro-data"

    sqs_client = boto3.client('sqs')

    sns_sqs = image_sorter_sns_sqs
    _setup_os_environ(bucket_name, sns_sqs)

    s3_client = boto3.client('s3', region_name='eu-west-1')
    s3_client.create_bucket(Bucket=bucket_name)

    metadata = collection_image_data(
        collection='images-M',
        image_access_restrictions="CC-BY-NC-ND",
        image_innopac_id="blahbluh",
        image_cleared="N")

    s3_client.put_object(
        Bucket=bucket_name,
        Key='metadata.json',
        Body=json.dumps(metadata)
    )

    main(s3_put_event, None)

    none_msg = _get_msg(sqs_client, sns_sqs["none"]["queue"])

    assert none_msg == metadata
