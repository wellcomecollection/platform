import json
import os

import boto3
from moto import mock_s3

import miro_copy_s3_asset


def assert_sns_message_forwarded(image_json, queue_url, sqs_client):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    actual_message = json.loads(message_body)['default']
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


@mock_s3
def test_should_copy_an_asset_into_a_different_bucket_and_forward_the_message(sns_sqs):
    sqs_client = boto3.client("sqs")
    s3_client = boto3.client("s3")
    topic_arn, queue_url = sns_sqs
    source_bucket_name = "test-miro-images-sync"
    s3_client.create_bucket(Bucket=source_bucket_name, ACL="private")
    miro_id = "A0000002"
    image_body = b'baba'
    s3_client.put_object(
        Bucket=source_bucket_name,
        ACL='private',
        Body=image_body, Key=f"fullsize/A0000000/{miro_id}.jpg")

    destination_key = f"A0000000/{miro_id}.jpg"

    destination_bucket_name = "test-miro-images-public"
    s3_client.create_bucket(Bucket=destination_bucket_name, ACL="private")

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

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "TOPIC_ARN": topic_arn
    }

    miro_copy_s3_asset.main(event, None)

    s3_response = s3_client.get_object(Bucket=destination_bucket_name, Key=destination_key)
    assert s3_response['Body'].read() == image_body

    assert_sns_message_forwarded(image_json, queue_url, sqs_client)


@mock_s3
def test_should_not_forward_the_message_if_the_asset_does_not_exist(sns_sqs):
    sqs_client = boto3.client("sqs")
    s3_client = boto3.client("s3")
    topic_arn, queue_url = sns_sqs
    source_bucket_name = "test-miro-images-sync"
    s3_client.create_bucket(Bucket=source_bucket_name, ACL="private")
    miro_id = "A0000002"

    destination_bucket_name = "test-miro-images-public"
    s3_client.create_bucket(Bucket=destination_bucket_name, ACL="private")

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

    os.environ = {
        "S3_SOURCE_BUCKET": source_bucket_name,
        "S3_DESTINATION_BUCKET": destination_bucket_name,
        "TOPIC_ARN": topic_arn
    }

    miro_copy_s3_asset.main(event, None)

    assert_bucket_is_empty(destination_bucket_name, s3_client)

    assert_no_messages_sent(queue_url, sqs_client)
