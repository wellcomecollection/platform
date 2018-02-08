# -*- encoding: utf-8 -*-

import json

import boto3
from mock import Mock
import pytest

import drain_ecs_container_instance


@pytest.fixture()
def ec2_terminating_message(lifecycle_hook_name, lifecycle_action_token, moto_topic_arn, autoscaling_group_name, ec2_instance_id):
    message = {
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
                'TopicArn': moto_topic_arn,
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}
    yield event, message


def test_complete_ec2_shutdown_if_no_ecs_cluster(
        lifecycle_hook_name,
        autoscaling_group_name,
        ec2_instance_id,
        ec2_terminating_message):
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    fake_sns_client = boto3.client('sns')

    event, _ = ec2_terminating_message

    mocked_asg_client = Mock()

    drain_ecs_container_instance.drain_ecs_container_instance(
        mocked_asg_client,
        fake_ec2_client,
        fake_ecs_client,
        fake_sns_client,
        event
    )

    mocked_asg_client \
        .complete_lifecycle_action \
        .assert_called_once_with(
            LifecycleHookName=lifecycle_hook_name,
            AutoScalingGroupName=autoscaling_group_name,
            LifecycleActionResult='CONTINUE',
            InstanceId=ec2_instance_id
        )


def test_complete_ec2_shutdown_ecs_cluster_no_tasks(
        lifecycle_hook_name,
        autoscaling_group_name,
        ec2_instance_id,
        ec2_terminating_message):
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    fake_sns_client = boto3.client('sns')

    event, _ = ec2_terminating_message

    mocked_asg_client = Mock()

    drain_ecs_container_instance.drain_ecs_container_instance(
        mocked_asg_client,
        fake_ec2_client,
        fake_ecs_client,
        fake_sns_client,
        event
    )

    mocked_asg_client \
        .complete_lifecycle_action \
        .assert_called_once_with(
            LifecycleHookName=lifecycle_hook_name,
            AutoScalingGroupName=autoscaling_group_name,
            LifecycleActionResult='CONTINUE',
            InstanceId=ec2_instance_id
        )


def test_drain_ecs_instance_if_running_tasks(
        lifecycle_hook_name,
        lifecycle_action_token,
        autoscaling_group_name,
        ec2_instance_id,
        ecs_task,
        ec2_terminating_message,
        moto_queue_url):
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    fake_sqs_client = boto3.client('sqs')
    fake_sns_client = boto3.client('sns')

    event, \
        message = ec2_terminating_message

    mocked_asg_client = Mock()

    drain_ecs_container_instance.drain_ecs_container_instance(
        mocked_asg_client,
        fake_ec2_client,
        fake_ecs_client,
        fake_sns_client,
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
            AutoScalingGroupName=autoscaling_group_name,
            LifecycleActionToken=lifecycle_action_token,
            InstanceId=ec2_instance_id,
        )

    mocked_asg_client \
        .complete_lifecycle_action \
        .assert_not_called()

    messages = fake_sqs_client.receive_message(
        QueueUrl=moto_queue_url,
        MaxNumberOfMessages=1
    )
    message_body = messages['Messages'][0]['Body']
    inner_message = json.loads(message_body)['Message']

    assert json.loads(inner_message)['default'] == json.dumps(message)
