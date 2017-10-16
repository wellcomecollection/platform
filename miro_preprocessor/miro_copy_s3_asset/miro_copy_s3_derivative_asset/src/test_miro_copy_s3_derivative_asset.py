import json
import os

import boto3
from moto import mock_s3
import pytest

import miro_copy_s3_derivative_asset


def assert_sns_message_forwarded(image_json, queue_url, sqs_client):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']

    actual_message = json.loads(inner_message)['default']
    assert actual_message == image_json


def assert_no_messages_sent(queue_url, sqs_client):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    assert 'Messages' not in messages


def assert_bucket_is_empty(destination_bucket_name, s3_client):
    objects = s3_client.list_objects(Bucket=destination_bucket_name)
    assert 'Content' not in objects.keys()


@pytest.fixture
def create_source_and_destination_buckets():
    mock_s3().start()
    s3_client = boto3.client("s3")
    source_bucket_name = "test-miro-images-sync"
    s3_client.create_bucket(Bucket=source_bucket_name, ACL="private")
    destination_bucket_name = "test-miro-images-public"
    s3_client.create_bucket(Bucket=destination_bucket_name, ACL="public-read")
    yield source_bucket_name, destination_bucket_name
    mock_s3().stop()


@pytest.fixture
def sns_image_json_event():
    miro_id = "A0000002"
    collection = "Images-C"
    image_data = {
        'image_no_calc': miro_id,
        'image_int_default': None,
        'image_artwork_date_from': "01/02/2000",
        'image_artwork_date_to': "13/12/2000",
        'image_barcode': "10000000",
        'image_creator': ["Caspar Bauhin"]
    }

    image_json = json.dumps({
        'collection': collection,
        'image_data': image_data
    })

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventVersion': '1.0',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:sns:stuff',
            'Sns': {
                'Type': 'Notification',
                'MessageId': 'b20eb72b-ffc7-5d09-9636-e6f65d67d10f',
                'TopicArn':
                    'arn:aws:sns:region:account_id:sns',
                'Subject': None,
                'Message': image_json,
                'Timestamp': '2017-07-10T15:42:24.307Z',
                'SignatureVersion': '1',
                'Signature': 'signature',
                'SigningCertUrl': 'https://certificate.pem',
                'UnsubscribeUrl': 'https://unsubscribe-url',
                'MessageAttributes': {}}
        }]
    }
    return miro_id, image_json, event


def test_should_copy_an_asset_into_a_different_bucket_and_forward_the_message(
        create_source_and_destination_buckets,
        sns_sqs,
        sns_image_json_event):
    sqs_client = boto3.client("sqs")
    s3_client = boto3.client("s3")
    source_bucket_name, destination_bucket_name = create_source_and_destination_buckets
    topic_arn, queue_url = sns_sqs
    miro_id, image_json, event = sns_image_json_event
    image_body = b'baba'
    s3_client.put_object(
        Bucket=source_bucket_name,
        ACL='private',
        Body=image_body, Key=f"fullsize/A0000000/{miro_id}.jpg")

    destination_key = f"A0000000/{miro_id}.jpg"

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "TOPIC_ARN": topic_arn
    }

    miro_copy_s3_derivative_asset.main(event, None)

    s3_response = s3_client.get_object(Bucket=destination_bucket_name, Key=destination_key)
    assert s3_response['Body'].read() == image_body

    assert_sns_message_forwarded(image_json, queue_url, sqs_client)


def test_should_not_forward_the_message_if_the_asset_does_not_exist(
        create_source_and_destination_buckets,
        sns_sqs,
        sns_image_json_event):
    sqs_client = boto3.client("sqs")
    s3_client = boto3.client("s3")
    source_bucket_name, destination_bucket_name = create_source_and_destination_buckets
    topic_arn, queue_url = sns_sqs
    miro_id, image_json, event = sns_image_json_event

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "TOPIC_ARN": topic_arn
    }

    miro_copy_s3_derivative_asset.main(event, None)

    assert_bucket_is_empty(destination_bucket_name, s3_client)

    assert_no_messages_sent(queue_url, sqs_client)


def test_should_replace_asset_if_already_exists_with_different_content(
        create_source_and_destination_buckets,
        sns_sqs,
        sns_image_json_event):
    sqs_client = boto3.client("sqs")
    s3_client = boto3.client("s3")
    source_bucket_name, destination_bucket_name = create_source_and_destination_buckets
    topic_arn, queue_url = sns_sqs
    miro_id, image_json, event = sns_image_json_event
    image_body = b'baba'
    s3_client.put_bucket_versioning(Bucket=destination_bucket_name,
                                    VersioningConfiguration={'Status': 'Enabled'})
    s3_client.put_object(
        Bucket=source_bucket_name,
        ACL='private',
        Body=image_body, Key=f"fullsize/A0000000/{miro_id}.jpg")

    destination_key = f"A0000000/{miro_id}.jpg"
    s3_client.put_object(
        Bucket=destination_bucket_name,
        ACL='public-read',
        Body=b"adjhgkjae", Key=destination_key)

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "TOPIC_ARN": topic_arn
    }

    miro_copy_s3_derivative_asset.main(event, None)

    s3_response = s3_client.get_object(Bucket=destination_bucket_name, Key=destination_key)
    assert s3_response['Body'].read() == image_body

    assert_sns_message_forwarded(image_json, queue_url, sqs_client)
