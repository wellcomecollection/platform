import json
import os

import boto3
from moto import mock_s3

import miro_move_s3_asset


@mock_s3
def test_miro_move_s3_asset_should_move_an_image_into_a_different_bucket():
    s3_client = boto3.client("s3")
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
        "S3_DESTINATION_BUCKET": destination_bucket_name
    }

    miro_move_s3_asset.main(event, None)
    s3_response = s3_client.get_object(Bucket=destination_bucket_name, Key=destination_key)
    assert s3_response['Body'].read() == image_body
