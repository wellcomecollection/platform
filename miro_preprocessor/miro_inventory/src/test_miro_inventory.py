import json
import os
import pytest

from botocore.vendored import requests
from botocore.vendored.requests.exceptions import HTTPError
from unittest import mock

import miro_inventory


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

os.environ = {
    "ES_CLUSTER_URL": 'https://example.com',
    "ES_INDEX": 'index',
    "ES_TYPE": 'type',
    "ES_USERNAME": 'foo',
    "ES_PASSWORD": 'foo',
    "ID_FIELD": 'image_data.image_no_calc',
}

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
            'Subject': 'catalogue_api',
            'Message': image_json,
            'Timestamp': '2017-07-10T15:42:24.307Z',
            'SignatureVersion': '1',
            'Signature': 'signature',
            'SigningCertUrl': 'https://certificate.pem',
            'UnsubscribeUrl': 'https://unsubscribe-url',
            'MessageAttributes': {}}
    }]
}


# This method will be used by the mock to replace requests.post
def mocked_requests_post(*args, **kwargs):
    class MockResponse(requests.Response):
        def __init__(self, status_code):
            self.status_code = status_code
            self.reason = "I am a mock object, don't ask me"

    if args[0] == 'https://example.com/index/type':
        return MockResponse(200)

    return MockResponse(500)


@mock.patch('miro_inventory.requests.post', side_effect=mocked_requests_post)
def test_miro_inventory(mock_get):
    miro_inventory.main(event, None)

    mock_get.assert_called_with(
        'https://example.com/index/type',
        data='{"id": "A0000002", "subject": "catalogue_api", "message": {"collection": "Images-C", "image_data": {"image_no_calc": "A0000002", "image_int_default": null, "image_artwork_date_from": "01/02/2000", "image_artwork_date_to": "13/12/2000", "image_barcode": "10000000", "image_creator": ["Caspar Bauhin"]}}}',
        headers={'Authorization': "Basic Zm9vOmZvbw=="}
    )


@mock.patch('miro_inventory.requests.post', side_effect=mocked_requests_post)
def test_miro_inventory_raises_error_on_500(mock_get):
    with pytest.raises(HTTPError):
        os.environ["ES_INDEX"] = "nope"

        miro_inventory.main(event, None)