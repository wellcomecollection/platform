import json
import os

import boto3

from moto import mock_sns, mock_sqs
import dynamo_to_sns


@mock_sns
@mock_sqs
def test_dynamo_to_sns():
    # Need this otherwise boto complains about missing region in sns_utils.pblish_sns_message
    # when trying to create client with sns = boto3.client('sns')
    # (despite region being set with the environment variable AWS_DEFAULT_REGION,
    # which should be read by default by boto)
    # Weirdly enough it doesn't complain in this file when it tries to do the same thing.
    # After investigation this is not related to moto
    boto3.setup_default_session(region_name=os.environ['AWS_DEFAULT_REGION'])
    client = boto3.client('sns')
    client.create_topic(Name="test-topic")
    response = client.list_topics()
    topic_arn = response["Topics"][0]['TopicArn']

    sqs_client = boto3.client('sqs')
    queue_name = "test-queue"
    queue = sqs_client.create_queue(QueueName=("%s" % queue_name))


    client.subscribe(TopicArn=topic_arn,
                     Protocol="sqs",
                     Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}")

    stream_arn = 'arn:aws:dynamodb:eu-west-1:123456789012:table/MiroData/stream/2017-06-01T12:51:55.680'
    new_image = {'ReindexVersion': {'N': '0'}, 'ReindexShard': {'S': 'default'}, 'data': {
        'S': 'test-json-data'},
          'MiroID': {'S': 'V0010033'}, 'MiroCollection': {'S': 'Images-V'}}

    event = {'Records': [{'eventID': '81659528846ddb9826c612c16043c2ea', 'eventName': 'MODIFY', 'eventVersion': '1.1',
                          'eventSource': 'aws:dynamodb', 'awsRegion': 'eu-west-1',
                          'dynamodb': {'ApproximateCreationDateTime': 1499243940.0,
                                       'Keys': {'MiroID': {'S': 'V0010033'}, 'MiroCollection': {'S': 'Images-V'}},
                                       'NewImage': new_image,
                                       'SequenceNumber': '167031600000000009949839133', 'SizeBytes': 6422,
                                       'StreamViewType': 'NEW_IMAGE'},
                          'eventSourceARN': stream_arn}]}

    stream_arn_map = {
        stream_arn: topic_arn
    }
    os.environ = {
        "STREAM_TOPIC_MAP": json.dumps(stream_arn_map)
    }

    dynamo_to_sns.main(event, None)

    messages = sqs_client.receive_message(QueueUrl=queue['QueueUrl'],MaxNumberOfMessages=1)
    assert json.loads(messages['Messages'][0]['Body'])['default'] == json.dumps(new_image)