import os

import boto3
import json

import schedule_reindexer


def _create_topic_and_queue(sns_client, sqs_client, name):
    queue_name = f"test-{name}"
    topic_name = f"test-{name}"

    print(f"Creating topic {topic_name} and queue {queue_name}")

    sns_client.create_topic(Name=topic_name)
    response = sns_client.list_topics()

    print(response)

    topics = [topic for topic in response["Topics"] if name in topic["TopicArn"]]
    topic_arn = topics[0]['TopicArn']

    queue = sqs_client.create_queue(QueueName=queue_name)

    sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )

    return topic_arn, queue['QueueUrl']


def schedule_reindexer_sns_sqs(set_region, moto_start):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')

    task_scheduler_topic, task_scheduler_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "task_scheduler")

    dynamo_provision_topic, dynamo_provision_queue = _create_topic_and_queue(
        fake_sns_client, fake_sqs_client, "dynamo_provision")

    return {
        "task_scheduler": {
            "topic": task_scheduler_topic,
            "queue": task_scheduler_queue
        },
        "dynamo_provision": {
            "topic": dynamo_provision_topic,
            "queue": dynamo_provision_queue
        }
    }


table_name = "table_name"
reindex_shard = "default"
cluster_name = "cluster_name"
reindexer_name = "reindexer_name"
desired_capacity = 42


def _get_msg(sqs_client, queue_url):
    messages = sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )

    message_body = messages['Messages'][0]['Body']

    return json.loads(
        json.loads(message_body)['default']
    )


def _wrap(image):
    return {
        'Records': [{
            'eventID': '81659528846ddb9826c612c16043c2ea',
            'eventName': 'MODIFY',
            'eventVersion': '1.1',
            'eventSource': 'aws:dynamodb',
            'awsRegion': 'eu-west-1',
            'dynamodb': {
                'NewImage': image,
            },
            'eventSourceARN': 'foo'
        }]
    }


def _config_os_env(sns_sqs):
    os.environ = {
        "DYNAMO_DESIRED_CAPACITY": desired_capacity,
        "SCHEDULER_TOPIC_ARN": sns_sqs["task_scheduler"]["topic"],
        "CLUSTER_NAME": "cluster_name",
        "REINDEXERS": f"{table_name}={reindexer_name}",
        "DYNAMO_TOPIC_ARN": sns_sqs["dynamo_provision"]["topic"],
        "DYNAMO_TABLE_NAME": table_name
    }


def test_schedule_reindexer_scale_up(set_region, moto_start):
    sqs_client = boto3.client('sqs')

    event = _wrap({
        'TableName': {'S': table_name},
        'ReindexShard': {'S': reindex_shard},
        'CurrentVersion': {'N': '0'},
        'RequestedVersion': {'N': '1'}
    })

    sns_sqs = schedule_reindexer_sns_sqs(set_region, moto_start)
    _config_os_env(sns_sqs)

    schedule_reindexer.main(event, None)

    task_scheduler_msg = _get_msg(sqs_client, sns_sqs["task_scheduler"]["queue"])
    dynamo_provision_msg = _get_msg(sqs_client, sns_sqs["dynamo_provision"]["queue"])

    expected_task_scheduler_msg = {
        'cluster': cluster_name,
        'desired_count': 1,
        'service': reindexer_name
    }

    expected_dynamo_provision_msg = {
        'dynamo_table_name': table_name,
        'desired_capacity': desired_capacity
    }

    print(os.environ)

    assert task_scheduler_msg == expected_task_scheduler_msg
    assert dynamo_provision_msg == expected_dynamo_provision_msg


def test_schedule_reindexer_scale_down(set_region, moto_start):
    table_name = "table_name"
    reindex_shard = "default"

    sqs_client = boto3.client('sqs')

    event = _wrap({
        'TableName': {'S': table_name},
        'ReindexShard': {'S': reindex_shard},
        'CurrentVersion': {'N': '1'},
        'RequestedVersion': {'N': '1'}
    })

    sns_sqs = schedule_reindexer_sns_sqs(set_region, moto_start)
    _config_os_env(sns_sqs)

    schedule_reindexer.main(event, None)

    task_scheduler_msg = _get_msg(sqs_client, sns_sqs["task_scheduler"]["queue"])
    dynamo_provision_msg = _get_msg(sqs_client, sns_sqs["dynamo_provision"]["queue"])

    expected_task_scheduler_msg = {
        'cluster': cluster_name,
        'desired_count': 0,
        'service': reindexer_name
    }

    expected_dynamo_provision_msg = {
        'dynamo_table_name': table_name,
        'desired_capacity': 1
    }

    print(os.environ)

    assert task_scheduler_msg == expected_task_scheduler_msg
    assert dynamo_provision_msg == expected_dynamo_provision_msg
