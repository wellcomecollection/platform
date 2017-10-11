# -*- encoding: utf-8 -*-

import json

import boto3
from moto import mock_s3
import pytest
import os

from miro_image_sorter import fetch_json_s3_data, parse_s3_event, main


def test_parse_s3_event(s3_put_event):
    assert parse_s3_event(s3_put_event) == 'metadata.json'


bucket_name = "miro-data"


def _create_bucket(s3_client):
    s3_client.create_bucket(Bucket=bucket_name)


def _put_object(s3_client, object_body, key):
    s3_client.put_object(
        Bucket=bucket_name,
        Key=key,
        Body=object_body
    )


def _put_metadata_json(s3_client, metadata):
    s3_client.put_object(
        Bucket=bucket_name,
        Key='metadata.json',
        Body=json.dumps(metadata)
    )


def _setup_os_environ(bucket_name, sns_sqs):
    os.environ['S3_MIRODATA_ID'] = bucket_name
    os.environ['S3_ID_EXCEPTIONS_KEY'] = "exceptions.csv"
    os.environ['S3_CONTRIB_EXCEPTIONS_KEY'] = "contrib.csv"
    os.environ['TOPIC_COLD_STORE'] = sns_sqs["cold_store"]["topic"]
    os.environ['TOPIC_TANDEM_VAULT'] = sns_sqs["tandem_vault"]["topic"]
    os.environ['TOPIC_CATALOGUE_API'] = sns_sqs["catalogue_api"]["topic"]
    os.environ['TOPIC_NONE'] = sns_sqs["none"]["topic"]


def _setup(
        s3_client,
        metadata,
        id_exceptions_csv_body=None,
        contrib_exceptions_csv_body=None,
        sns_sqs=None):

    if id_exceptions_csv_body is None:
        id_exceptions_csv_body = "miro_id,cold_store,tandem_vault,catalogue_api"

    if contrib_exceptions_csv_body is None:
        contrib_exceptions_csv_body = "A,B,C,N,W"

    if sns_sqs is not None:
        _setup_os_environ(bucket_name, sns_sqs)

    _create_bucket(s3_client)
    _put_object(s3_client, id_exceptions_csv_body, "exceptions.csv")
    _put_object(s3_client, contrib_exceptions_csv_body, "contrib.csv")
    _put_metadata_json(s3_client, metadata)


def _get_msg(sqs_client, queue_url):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']

    return json.loads(
        json.loads(inner_message)['default']
    )


def collection_image_data(**kwargs):
    collection = 'images-M'
    image_data = {
        "image_no_calc": "V1234567",
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "Y",
        "image_use_restrictions": "CC-BY",
        "image_general_use": "Y",
        "image_innopac_id": "12345678",
        "image_cleared": "Y",
        "image_source_code": "XXX"
    }

    collection = kwargs.pop('collection', 'images-M')

    return {
        "collection": collection,
        "image_data": image_data
    }


@mock_s3
def test_fetch_json_s3_data():
    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = {
        'image_calc_no': 'V0000001',
        'image_title': 'A morsel of metadata about mussels',
    }

    _setup(
        s3_client=s3_client,
        metadata=metadata
    )

    assert fetch_json_s3_data(bucket=bucket_name, key='metadata.json') == metadata


@mock_s3
def test_image_sorter_catalogue_api(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data()

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    catalogue_api_msg = _get_msg(sqs_client, sns_sqs["catalogue_api"]["queue"])

    assert catalogue_api_msg == metadata


@mock_s3
def test_image_sorter_tandem_vault(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data(
        collection='images-L', image_library_dept="Public programmes")

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    tandem_value_msg = _get_msg(sqs_client, sns_sqs["tandem_vault"]["queue"])

    assert tandem_value_msg == metadata


@mock_s3
def test_image_sorter_cold_store(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    _setup_os_environ(bucket_name, sns_sqs)

    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data(
        collection='images-L', image_library_dept="Archives and Manuscripts")

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    cold_store_msg = _get_msg(sqs_client, sns_sqs["cold_store"]["queue"])

    assert cold_store_msg == metadata


@mock_s3
def test_image_sorter_none(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')

    sns_sqs = image_sorter_sns_sqs

    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data(
        collection='images-M',
        image_access_restrictions="CC-BY-NC-ND",
        image_innopac_id="blahbluh",
        image_cleared="N")

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    none_msg = _get_msg(sqs_client, sns_sqs["none"]["queue"])

    assert none_msg == metadata


@mock_s3
def test_image_sorter_id_exceptions(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    s3_client = boto3.client('s3', region_name='eu-west-1')

    miro_id = "V1234567"
    metadata = collection_image_data(
        image_no_calc=miro_id)

    id_exceptions_csv_body = f"""miro_id,cold_store,tandem_vault,catalogue_api\n{miro_id},true,,"""

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        id_exceptions_csv_body=id_exceptions_csv_body,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    cold_store_msg = _get_msg(sqs_client, sns_sqs["cold_store"]["queue"])

    assert cold_store_msg == metadata


@mock_s3
def test_image_sorter_contrib_exceptions_match(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data(
        image_source_code="FOO",
        collection="images-A"
    )

    contrib_exceptions_csv_body = f"""A,B,C,N,W\nFOO,,,,,"""

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        contrib_exceptions_csv_body=contrib_exceptions_csv_body,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    catalogue_api_msg = _get_msg(sqs_client, sns_sqs["catalogue_api"]["queue"])

    assert catalogue_api_msg == metadata


@mock_s3
def test_image_sorter_contrib_exceptions_no_match(image_sorter_sns_sqs, s3_put_event):
    sqs_client = boto3.client('sqs')
    sns_sqs = image_sorter_sns_sqs
    s3_client = boto3.client('s3', region_name='eu-west-1')

    metadata = collection_image_data(
        image_source_code="BAR",
        collection="images-A"
    )

    contrib_exceptions_csv_body = f"""A,B,C,N,W\nFOO,,,,,"""

    _setup(
        s3_client=s3_client,
        metadata=metadata,
        contrib_exceptions_csv_body=contrib_exceptions_csv_body,
        sns_sqs=sns_sqs
    )

    main(s3_put_event, None)

    cold_store_msg = _get_msg(sqs_client, sns_sqs["cold_store"]["queue"])
    assert cold_store_msg == metadata

    with pytest.raises(KeyError):
        _get_msg(sqs_client, sns_sqs["catalogue_api"]["queue"])

    with pytest.raises(KeyError):
        _get_msg(sqs_client, sns_sqs["tandem_vault"]["queue"])
