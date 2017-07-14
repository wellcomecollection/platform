import json

import boto3
from mock import Mock
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
from moto.ec2 import utils as ec2_utils

import drain_ecs_container_instance

import pytest


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


@pytest.fixture()
def autoscaling_group_info(moto_start):
    auto_scaling_group_name = 'TestGroup1'
    print(f"creating autoscaling group {auto_scaling_group_name}")
    fake_asg_client = boto3.client('autoscaling')
    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=auto_scaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    fake_ec2_client = boto3.client('ec2')
    instances = fake_ec2_client.describe_instances()
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']
    yield auto_scaling_group_name, instance_id


@pytest.fixture()
def sns_sqs_info(moto_start):
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')
    queue_name = "test-queue"
    topic_name = "test-topic"
    print(f"Creating topic {topic_name} and queue {queue_name}")

    fake_sns_client.create_topic(Name=topic_name)
    response = fake_sns_client.list_topics()
    topic_arn = response["Topics"][0]['TopicArn']

    queue = fake_sqs_client.create_queue(QueueName=queue_name)

    fake_sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )
    yield topic_arn, queue['QueueUrl']


@pytest.fixture()
def ecs_cluster(autoscaling_group_info):
    fake_ecs_client = boto3.client('ecs')
    fake_ec2_client = boto3.client('ec2')
    cluster_name = 'test_ecs_cluster'
    print(f"Creating ecs cluster {cluster_name}")
    instance_id = autoscaling_group_info[1]
    cluster_response = fake_ecs_client.create_cluster(
        clusterName=cluster_name
    )

    ec2 = boto3.resource('ec2')
    instance = ec2.Instance(instance_id)

    instance_id_document = json.dumps(
        ec2_utils.generate_instance_identity_document(instance)
    )

    register_container_response = fake_ecs_client.register_container_instance(
        cluster=cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    container_instance_arn = \
        register_container_response['containerInstance']['containerInstanceArn']
    cluster_arn = cluster_response['cluster']['clusterArn']

    fake_ec2_client.create_tags(
        Resources=[
            instance_id,
        ],
        Tags=[
            {
                'Key': 'clusterArn',
                'Value': cluster_arn
            },
            {
                'Key': 'containerInstanceArn',
                'Value': container_instance_arn
            }
        ]
    )
    yield cluster_arn, cluster_arn, container_instance_arn


@pytest.fixture()
def ecs_task(ecs_cluster):
    fake_ecs_client = boto3.client('ecs')
    task_name = 'test_ecs_task'

    cluster_name = ecs_cluster[0]
    cluster_arn = ecs_cluster[1]
    container_instance_arn = ecs_cluster[2]
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
        cluster=cluster_name,
        overrides={},
        taskDefinition=task_name,
        count=1,
        startedBy='moto'
    )

    tasks = fake_ecs_client.list_tasks(
        cluster=cluster_arn,
        containerInstance=container_instance_arn
    )
    assert len(tasks['taskArns']) == 1
    yield


def test_complete_ec2_shutdown_if_no_ecs_cluster(autoscaling_group_info):
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    autoscaling_group = autoscaling_group_info[0]
    instance_id = autoscaling_group_info[1]
    lifecycle_hook_name = "monitoring-cluster-LifecycleHook-OENP6M5XGYVM"

    message = {
        "LifecycleHookName": lifecycle_hook_name,
        "AccountId": "account_id",
        "RequestId": "f29364ad-8523-4d58-9a70-3537f4edec15",
        "LifecycleTransition": "autoscaling:EC2_INSTANCE_TERMINATING",
        "AutoScalingGroupName": autoscaling_group,
        "Service": "AWS Auto Scaling",
        "Time": "2017-07-10T12:36:05.857Z",
        "EC2InstanceId": instance_id,
        "LifecycleActionToken": "78c16884-6bd4-4296-ac0c-2da9eb6a0d29"
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
            'EventVersion': '1.0',
            'Sns': {
                'Message': json.dumps(message),
                'MessageAttributes': {},
                'MessageId': 'a4416c50-9ec6-5a8e-934a-3d8de60d1428',
                'Signature': 'signature',
                'SignatureVersion': '1',
                'SigningCertUrl': 'https://certificate.pem',
                'Subject': None,
                'Timestamp': '2017-07-10T12:43:55.664Z',
                'TopicArn':
                    'arn:aws:sns:region:account_id:ec2_terminating_topic',
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}

    mocked_asg_client = Mock()

    drain_ecs_container_instance.drain_ecs_container_instance(
        mocked_asg_client,
        fake_ec2_client,
        fake_ecs_client,
        event
    )

    mocked_asg_client \
        .complete_lifecycle_action \
        .assert_called_once_with(
            LifecycleHookName=lifecycle_hook_name,
            AutoScalingGroupName=autoscaling_group,
            LifecycleActionResult='CONTINUE',
            InstanceId=instance_id
        )


def test_drain_ecs_instance_if_running_tasks(sns_sqs_info, autoscaling_group_info, ecs_task):
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    fake_sqs_client = boto3.client('sqs')
    topic_arn = sns_sqs_info[0]
    queue_url = sns_sqs_info[1]
    auto_scaling_group_name = autoscaling_group_info[0]
    instance_id = autoscaling_group_info[1]

    lifecycle_action_token = "78c16884-6bd4-4296-ac0c-2da9eb6a0d29"

    lifecycle_hook_name = "monitoring-cluster-LifecycleHook-OENP6M5XGYVM"
    message = {
        "LifecycleHookName": lifecycle_hook_name,
        "AccountId": "account_id",
        "RequestId": "f29364ad-8523-4d58-9a70-3537f4edec15",
        "LifecycleTransition": "autoscaling:EC2_INSTANCE_TERMINATING",
        "AutoScalingGroupName": auto_scaling_group_name,
        "Service": "AWS Auto Scaling",
        "Time": "2017-07-10T12:36:05.857Z",
        "EC2InstanceId": instance_id,
        "LifecycleActionToken": lifecycle_action_token
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
            'EventVersion': '1.0',
            'Sns': {
                'Message': json.dumps(message),
                'MessageAttributes': {},
                'MessageId': 'a4416c50-9ec6-5a8e-934a-3d8de60d1428',
                'Signature': 'signature',
                'SignatureVersion': '1',
                'SigningCertUrl': 'https://certificate.pem',
                'Subject': None,
                'Timestamp': '2017-07-10T12:43:55.664Z',
                'TopicArn': topic_arn,
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}

    mocked_asg_client = Mock()

    drain_ecs_container_instance.drain_ecs_container_instance(
        mocked_asg_client,
        fake_ec2_client,
        fake_ecs_client,
        event
    )

    # Commented out as there is a bug in moto:
    # https://github.com/spulec/moto/issues/1009
    # TODO figure out another way to assert on this while
    # the bug is fixed (mocks?)
    # container_instance_info = fake_ecs_client.describe_container_instances(
    #     cluster=cluster_name,
    #     containerInstances=[container_instance_arn]
    # )
    #
    # assert
    # container_instance_info['containerInstances'][0]['status'] == "DRAINING"

    mocked_asg_client \
        .record_lifecycle_action_heartbeat \
        .assert_called_once_with(
            LifecycleHookName=lifecycle_hook_name,
            AutoScalingGroupName=auto_scaling_group_name,
            LifecycleActionToken=lifecycle_action_token,
            InstanceId=instance_id,
        )

    mocked_asg_client\
        .complete_lifecycle_action\
        .assert_not_called()

    messages = fake_sqs_client.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']

    assert json.loads(message_body)['default'] == json.dumps(message)
