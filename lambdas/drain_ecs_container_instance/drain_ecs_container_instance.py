#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This lambda is triggered by messages in the ec2_terminating_topic.
Messages are sent to this topic by the auto scaling group when it's
terminating one instance.

This lambda checks if the EC2 instance being terminated is a
container instance of an ECS cluster.

If it is, it checks if it has tasks running and puts it in draining
state if it has.

It then resends the same message to the topic so that it triggers itself
until there are no more tasks, in which case it continues the lifecycle
hook allowing the EC2 instance to terminate.

See below for example event.
"""

EXAMPLE_EVENT = """
{
  "Records": [
    {
      "EventVersion": "1.0",
      "EventSubscriptionArn": "event_subscription_arn",
      "EventSource": "aws:sns",
      "Sns": {
        "SignatureVersion": "1",
        "Timestamp": "1970-01-01T00:00:00.000Z",
        "Signature": "EXAMPLE",
        "SigningCertUrl": "EXAMPLE",
        "MessageId": "95df01b4-ee98-5cb9-9903-4c221d41eb5e",
        "Message":" """ + """
            { 
                "LifecycleActionToken":"87654321-4321-4321-4321-210987654321", 
                "AutoScalingGroupName":"my-asg", 
                "LifecycleHookName":"my-lifecycle-hook", 
                "EC2InstanceId":"i-02dff6affe226139f", 
                "LifecycleTransition":"autoscaling:EC2_INSTANCE_TERMINATING" 
            }
        """.replace('"', '\\"').replace('\n', '\\n') +  """ ",
        "MessageAttributes": {},
        "Type": "Notification",
        "UnsubscribeUrl": "EXAMPLE",
        "TopicArn": "topic_arn",
        "Subject": "TestInvoke"
      }
    }
  ]
}
"""

import collections
import json
import pprint
import time

import boto3

from sns_utils import publish_sns_message


def set_container_instance_to_draining(
        cluster_arn,
        ecs_container_instance_arn,
        ecs_client):

    return ecs_client.update_container_instances_state(
        cluster=cluster_arn,
        containerInstances=[
            ecs_container_instance_arn,
        ],
        status='DRAINING'
    )


def continue_lifecycle_action(
        asg_group_name,
        ec2_instance_id,
        lifecycle_hook_name,
        asg_client):

    return asg_client.complete_lifecycle_action(
        LifecycleHookName=lifecycle_hook_name,
        AutoScalingGroupName=asg_group_name,
        LifecycleActionResult='CONTINUE',
        InstanceId=ec2_instance_id)


class ECSInfo(collections.namedtuple(
    'ECSInfo',
    'cluster_arn ecs_container_instance_arn running_tasks status')):

    @classmethod
    def create(cls, ecs_client, ec2_client, ec2_instance_id):
        ec2_instance_info = ec2_client.describe_instances(
            InstanceIds=[ec2_instance_id])

        print(f'Received ec2_instance_info:\n{pprint.pformat(ec2_instance_info)}')

        tags = ec2_instance_info['Reservations'][0]['Instances'][0]['Tags']

        ecs_container_instance_arns = \
            [x['Value'] for x in tags if x['Key'] == 'containerInstanceArn']

        cluster_arns = \
            [x['Value'] for x in tags if x['Key'] == 'clusterArn']

        running_tasks = []
        cluster_arn = None
        ecs_container_instance_arn = None

        if cluster_arns and ecs_container_instance_arns:
            cluster_arn = cluster_arns[0]
            ecs_container_instance_arn = ecs_container_instance_arns[0]

            running_tasks = ecs_client.list_tasks(
                cluster = cluster_arn,
                containerInstance = ecs_container_instance_arn)

            container_instance_info = ecs_client.describe_container_instances(
                cluster=cluster_arn,
                containerInstances=[ecs_container_instance_arn],
            )

            status = container_instance_info['containerInstances'][0]['status']

        return cls(cluster_arn, ecs_container_instance_arn, running_tasks, status)

    def has_no_tags():
        no_cluster_arn_tags = not self.cluster_arn
        no_container_instance_arn_tags = not self.ecs_container_instance_arns

        return no_cluster_arn_tags and no_container_instance_arn_tags

    def has_no_tasks():
        return not self.running_tasks

    def is_draining():
        return self.status != 'DRAINING'



EC2Info = collections.namedtuple(
    'EC2Info',
    'ec2_instance_id asg_group_name')

LifeCycleInfo = collections.namedtuple(
    'LifecycleInfo',
    'hook_name transition action_token')

class LifecycleEvent(collections.namedtuple(
    'LifecycleEvent',
    'ec2_info lifecycle_info topic_arn')):

    @classmethod
    def create(cls, event):
        topic_arn = event['Records'][0]['Sns']['TopicArn']

        message = event['Records'][0]['Sns']['Message']
        message_data = json.loads(message)

        ec2_instance_id = message_data['EC2InstanceId']
        asg_group_name = message_data['AutoScalingGroupName']

        lifecycle_hook_name = message_data['LifecycleHookName']
        lifecycle_transition = message_data['LifecycleTransition']
        lifecycle_action_token = message_data['LifecycleActionToken']

        return cls(
            EC2Info(ec2_instance_id, asg_group_name),
            LifeCycleInfo(
                lifecycle_hook_name,
                lifecycle_transition,
                lifecycle_action_token
            ),
            topic_arn
        )

    def is_terminating():
        self.lifecycle_info.transition == \
        'autoscaling:EC2_INSTANCE_TERMINATING'


def main(event, _):
    asg_client = boto3.client("autoscaling")
    ec2_client = boto3.client("ec2")
    ecs_client = boto3.client('ecs')

    print(f'Received event:\n{pprint.pformat(event)}')

    lifecycle_event = LifecycleEvent.create(event)

    if (not lifecycle_event.is_terminating):
        return

    ecs_info = ECSInfo.create(
        ecs_client,
        ec2_client,
        lifecycle_event.ec2_info.ec2_instance_id
    )

    def _continue_lifecycle_action():
        return asg_client.complete_lifecycle_action(
            LifecycleHookName=lifecycle_event.lifecycle_info.hook_name,
            AutoScalingGroupName=lifecycle_event.ec2_info.asg_group_name,
            LifecycleActionResult='CONTINUE',
            InstanceId=lifecycle_event.ec2_info.ec2_instance_id)

    print(f"running tasks: {ecs_info.running_tasks}")

    if ecs_info.has_no_tags or ecs_info.has_no_tasks():
        response = _continue_lifecycle_action()
        print(f'continue_lifecycle_action received:\n{pprint.pformat(response)}')

    asg_client.record_lifecycle_action_heartbeat(
        LifecycleHookName=lifecycle_event.lifecycle_info.hook_name,
        AutoScalingGroupName=lifecycle_event.ec2_info.asg_group_name,
        LifecycleActionToken=lifecycle_event.lifecycle_info.action_token,
        InstanceId=lifecycle_event.ec2_info.ec2_instance_id,
    )

    if not ecs_info.is_draining:
        response = set_container_instance_to_draining(
            ecs_info.cluster_arn,
            ecs_info.ecs_container_instance_arn,
            ecs_client)

        print(f'set_container_instance_to_draining received:\n{pprint.pformat(response)}')

        time.sleep(30)
        publish_sns_message(topic_arn, message_data)
