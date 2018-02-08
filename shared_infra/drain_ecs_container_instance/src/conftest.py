# -*- encoding: utf-8 -*-

import json
import os

import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
from moto.ec2 import utils as ec2_utils
import pytest


@pytest.fixture
def autoscaling_group_name():
    return 'TestGroup1'


@pytest.fixture()
def ec2_instance_id(autoscaling_group_name, moto_start):
    print(f'Creating autoscaling group {autoscaling_group_name}')

    fake_asg_client = boto3.client('autoscaling')
    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=autoscaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    fake_ec2_client = boto3.client('ec2')
    instances = fake_ec2_client.describe_instances()
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']
    yield instance_id


@pytest.fixture()
def moto_start():
    mock_autoscaling().start()
    mock_ec2().start()
    mock_ecs().start()
    mock_sns().start()
    mock_sqs().start()
    yield
    mock_autoscaling().stop()
    mock_ec2().stop()
    mock_ecs().stop()
    mock_sns().stop()
    mock_sqs().stop()


@pytest.fixture
def ecs_cluster_name():
    return 'test_ecs_cluster'


@pytest.fixture
def ecs_cluster_arn(moto_start):
    ecs_client = boto3.client('ecs')

    cluster_name = 'test_ecs_cluster'
    print(f'Creating ECS cluster {cluster_name}')
    resp = ecs_client.create_cluster(clusterName=cluster_name)

    yield resp['cluster']['clusterArn']


@pytest.fixture
def ecs_container_instance_arn(
    ec2_instance_id, ecs_cluster_name, ecs_cluster_arn
):
    ecs_client = boto3.client('ecs')
    ec2_client = boto3.client('ec2')
    ec2_resource = boto3.resource('ec2')

    instance = ec2_resource.Instance(ec2_instance_id)

    instance_id_document = json.dumps(
        ec2_utils.generate_instance_identity_document(instance)
    )

    resp = ecs_client.register_container_instance(
        cluster=ecs_cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    instance_arn = resp['containerInstance']['containerInstanceArn']

    ec2_client.create_tags(
        Resources=[
            ec2_instance_id,
        ],
        Tags=[
            {
                'Key': 'clusterArn',
                'Value': ecs_cluster_arn
            },
            {
                'Key': 'containerInstanceArn',
                'Value': instance_arn
            }
        ]
    )

    yield instance_arn


@pytest.fixture()
def ecs_task(ecs_cluster_name, ecs_cluster_arn, ecs_container_instance_arn):
    fake_ecs_client = boto3.client('ecs')
    task_name = 'test_ecs_task'
    print(f"Creating task {task_name}")
    fake_ecs_client.register_task_definition(
        family=task_name,
        containerDefinitions=[
            {
                'name': 'hello_world',
                'image': 'docker/hello-world:latest',
                'cpu': 1024,
                'memory': 400,
                'essential': True,
                'environment': [{
                    'name': 'AWS_ACCESS_KEY_ID',
                    'value': 'SOME_ACCESS_KEY'
                }],
                'logConfiguration': {'logDriver': 'json-file'}
            }
        ]
    )

    fake_ecs_client.run_task(
        cluster=ecs_cluster_name,
        overrides={},
        taskDefinition=task_name,
        count=1,
        startedBy='moto'
    )

    tasks = fake_ecs_client.list_tasks(
        cluster=ecs_cluster_arn,
        containerInstance=ecs_container_instance_arn
    )
    assert len(tasks['taskArns']) == 1
    yield


@pytest.fixture
def lifecycle_hook_name():
    return 'monitoring-cluster-LifecycleHook-OENP6M5XGYVM'


@pytest.fixture
def lifecycle_action_token():
    return '78c16884-6bd4-4296-ac0c-2da9eb6a0d29'


@pytest.fixture
def ec2_terminating_message(
    lifecycle_hook_name,
    autoscaling_group_name,
    ec2_instance_id,
    lifecycle_action_token
):
    return {
        "LifecycleHookName": lifecycle_hook_name,
        "AccountId": "account_id",
        "RequestId": "f29364ad-8523-4d58-9a70-3537f4edec15",
        "LifecycleTransition": "autoscaling:EC2_INSTANCE_TERMINATING",
        "AutoScalingGroupName": autoscaling_group_name,
        "Service": "AWS Auto Scaling",
        "Time": "2017-07-10T12:36:05.857Z",
        "EC2InstanceId": ec2_instance_id,
        "LifecycleActionToken": lifecycle_action_token
    }


@pytest.fixture
def ec2_terminating_event(ec2_terminating_message, moto_topic_arn):
    return {
        'Records': [
            {
                'EventSource': 'aws:sns',
                'EventSubscriptionArn': 'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
                'EventVersion': '1.0',
                'Sns': {
                    'Message': json.dumps(ec2_terminating_message),
                    'MessageAttributes': {},
                    'MessageId': 'a4416c50-9ec6-5a8e-934a-3d8de60d1428',
                    'Signature': 'signature',
                    'SignatureVersion': '1',
                    'SigningCertUrl': 'https://certificate.pem',
                    'Subject': None,
                    'Timestamp': '2017-07-10T12:43:55.664Z',
                    'TopicArn': moto_topic_arn,
                    'Type': 'Notification',
                    'UnsubscribeUrl': 'https://unsubscribe-url'
                }
            }
        ]
    }


@pytest.fixture
def moto_topic_arn():
    """Creates an SNS topic in moto, and yields the new topic ARN."""
    with mock_sns():
        sns_client = boto3.client('sns')
        topic_name = 'test-lambda-topic'

        resp = sns_client.create_topic(Name=topic_name)
        topic_arn = resp['TopicArn']

        # Our Lambdas all read their topic ARN from the environment, so we
        # set it here.
        os.environ.update({'TOPIC_ARN': topic_arn})

        yield topic_arn


@pytest.fixture
def moto_queue_url(moto_topic_arn):
    """
    Creates an SQS queue in moto, subscribes it to an SNS topic, and
    yields the new queue URL.
    """
    with mock_sqs():
        sns_client = boto3.client('sns')
        sqs_client = boto3.client('sqs')
        queue_name = 'test-lambda-queue'

        resp = sqs_client.create_queue(QueueName=queue_name)
        queue_url = resp['QueueUrl']

        sns_client.subscribe(
            TopicArn=moto_topic_arn,
            Protocol='sqs',
            Endpoint=f'arn:aws:sqs:eu-west-1:123456789012:{queue_name}'
        )
        yield queue_url
