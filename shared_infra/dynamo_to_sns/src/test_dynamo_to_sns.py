# -*- encoding: utf-8 -*-

import os

import pytest

import dynamo_to_sns


TEST_STREAM_ARN = 'arn:aws:dynamodb:eu-west-1:123456789012:table/table-stream'

OLD_IMAGE = {
    'MiroID': {'S': 'V0000001'},
    'MiroCollection': {'S': 'Images-V'},
}

OLD_IMAGE_DATA = {
    'MiroID': 'V0000001',
    'MiroCollection': 'Images-V'
}

NEW_IMAGE = {
    'ReindexVersion': {'N': '0'},
    'ReindexShard': {'S': 'default'},
    'data': {'S': 'test-json-data'},
    'MiroID': {'S': 'V0010033'},
    'MiroCollection': {'S': 'Images-V'}
}

NEW_IMAGE_DATA = {
    'ReindexVersion': 0,
    'ReindexShard': 'default',
    'data': 'test-json-data',
    'MiroID': 'V0010033',
    'MiroCollection': 'Images-V'
}


def _dynamo_event(event_name, old_image=None, new_image=None):
    event_data = {
        'eventID': '87cf2ca0f689908d573fb3698a487bb1',
        'eventName': event_name,
        'eventVersion': '1.1',
        'eventSource': 'aws:dynamodb',
        'awsRegion': 'eu-west-1',
        'dynamodb': {
            'ApproximateCreationDateTime': 1505815200.0,
            'Keys': {
                'MiroID': {
                    'S': 'V0000001'
                },
                'MiroCollection': {
                    'S': 'Images-V'
                }
            },
            'OldImage': old_image,
            'SequenceNumber': '545308300000000005226392296',
            'SizeBytes': 36,
            'StreamViewType': 'OLD_IMAGE'
        },
        'eventSourceARN': TEST_STREAM_ARN
    }

    if old_image is not None:
        event_data['dynamodb']['OldImage'] = old_image
    if new_image is not None:
        event_data['dynamodb']['NewImage'] = new_image

    return event_data


@pytest.mark.parametrize('input_event, expected_message, stream_view_type', [

    # These two tests simulate the old behaviour, when we didn't have a
    # STREAM_VIEW_TYPE variable.
    (
        _dynamo_event(event_name='MODIFY', new_image=NEW_IMAGE),
        {
            'event_type': 'MODIFY',
            'old_image': None,
            'new_image': NEW_IMAGE_DATA
        },
        None
    ),
    (
        _dynamo_event(event_name='REMOVE', old_image=OLD_IMAGE),
        {
            'event_type': 'REMOVE',
            'old_image': OLD_IMAGE_DATA,
            'new_image': None
        },
        None
    ),

    # Then we replay exactly the same test events, but this time setting the
    # view type explicitly.
    (
        _dynamo_event(event_name='MODIFY', new_image=NEW_IMAGE),
        {
            'event_type': 'MODIFY',
            'old_image': None,
            'new_image': NEW_IMAGE_DATA
        },
        'FULL_EVENT'
    ),
    (
        _dynamo_event(event_name='REMOVE', old_image=OLD_IMAGE),
        {
            'event_type': 'REMOVE',
            'old_image': OLD_IMAGE_DATA,
            'new_image': None
        },
        'FULL_EVENT'
    ),

    # And now we play a couple of tests with different view types, to check
    # they behave correctly.
    (
        _dynamo_event(event_name='REMOVE', old_image=OLD_IMAGE),
        OLD_IMAGE_DATA,
        'OLD_IMAGE_ONLY'
    ),
    (
        _dynamo_event(event_name='REMOVE', new_image=NEW_IMAGE),
        NEW_IMAGE_DATA,
        'NEW_IMAGE_ONLY'
    ),
])
def test_end_to_end_feature_test(
    sns_client, topic_arn, input_event, expected_message, stream_view_type
):
    if stream_view_type is not None:
        os.environ.update({'STREAM_VIEW_TYPE': stream_view_type})

    event = {'Records': [input_event]}
    dynamo_to_sns.main(event=event, sns_client=sns_client)

    messages = sns_client.list_messages()
    assert len(messages) == 1
    assert messages[0][':message'] == expected_message


def test_invalid_stream_view_type_is_error(topic_arn):
    input_event = _dynamo_event(event_name='REMOVE', old_image=OLD_IMAGE)
    os.environ.update({'STREAM_VIEW_TYPE': 'NOTAREALSTREAMVIEWTYPE'})

    event = {'Records': [input_event]}

    with pytest.raises(ValueError):
        dynamo_to_sns.main(event=event)
