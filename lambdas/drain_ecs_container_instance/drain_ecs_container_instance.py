#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This lambda is triggered by messages in the ec2_terminating_topic.
Messages are sent to this topic by the auto scaling group when it's terminating one instance.
This lambda checks if the EC2 instance being terminated is a container instance of an ECS cluster.
If it is, it checks if it has tasks running and puts it in draining state if it has.
It then resends the same message to the topic so that it triggers itself until there are no more tasks,
 in which case it continues the lifecycle hook allowing the EC2 instance to terminate
"""
import json
import pprint
import time

import boto3

from sns_utils import publish_sns_message


def set_container_instance_to_draining(cluster_arn, ecs_container_instance_arn, ecs_client):
    ecs_client.update_container_instances_state(
        cluster=cluster_arn,
        containerInstances=[
            ecs_container_instance_arn,
        ],
        status='DRAINING'
    )


def continue_lifecycle_action(asg_group_name, ec2_instance_id, lifecycle_hook_name, asg_client):
    response = asg_client.complete_lifecycle_action(
        LifecycleHookName=lifecycle_hook_name,
        AutoScalingGroupName=asg_group_name,
        LifecycleActionResult='CONTINUE',
        InstanceId=ec2_instance_id)
    pprint.pprint(response)


def get_ecs_info_from_tags(ec2_client, ec2_instance_id):
    ec2_instance_info = ec2_client.describe_instances(InstanceIds=[
        ec2_instance_id,
    ], )
    tags = ec2_instance_info['Reservations'][0]['Instances'][0]['Tags']
    ecs_container_instance_arns = [x['Value'] for x in tags if x['Key'] == 'containerInstanceArn']
    cluster_arns = [x['Value'] for x in tags if x['Key'] == 'clusterArn']
    print(f"containerInstanceArns: {ecs_container_instance_arns}, clusterArns: {cluster_arns}")
    return {
        'cluster_arns': cluster_arns,
        'ecs_container_instance_arns': ecs_container_instance_arns
    }


def main(event, _):
    asg_client = boto3.client("autoscaling")
    ec2_client = boto3.client("ec2")
    ecs_client = boto3.client('ecs')

    print(f'Received event:\n{pprint.pformat(event)}')

    topic_arn = event['Records'][0]['Sns']['TopicArn']
    message = event['Records'][0]['Sns']['Message']
    message_data = json.loads(message)

    ec2_instance_id = message_data['EC2InstanceId']
    asg_group_name = message_data['AutoScalingGroupName']
    lifecycle_hook_name = message_data['LifecycleHookName']
    lifecycle_transition = message_data['LifecycleTransition']
    lifecycle_action_token = message_data['LifecycleActionToken']

    if lifecycle_transition == 'autoscaling:EC2_INSTANCE_TERMINATING':
        ecs_info = get_ecs_info_from_tags(ec2_client, ec2_instance_id)

        if not ecs_info['cluster_arns'] and not ecs_info['ecs_container_instance_arns']:
            continue_lifecycle_action(asg_group_name, ec2_instance_id, lifecycle_hook_name, asg_client)
            return

        cluster_arn=ecs_info['cluster_arns'][0]
        ecs_container_instance_arn=ecs_info['ecs_container_instance_arns'][0]
        running_tasks = ecs_client.list_tasks(
            cluster=ecs_info['cluster_arns'][0],
            containerInstance=ecs_info['ecs_container_instance_arns'][0]
        )
        print(f"running tasks: {running_tasks['taskArns']}")

        if not running_tasks['taskArns']:
            continue_lifecycle_action(asg_group_name, ec2_instance_id, lifecycle_hook_name, asg_client)
        else:
            asg_client.record_lifecycle_action_heartbeat(
                LifecycleHookName=lifecycle_hook_name,
                AutoScalingGroupName=asg_group_name,
                LifecycleActionToken=lifecycle_action_token,
                InstanceId=ec2_instance_id,
            )

            container_instance_info = ecs_client.describe_container_instances(
                cluster=cluster_arn,
                containerInstances=[ecs_container_instance_arn],
            )
            if container_instance_info['containerInstances'][0]['status'] != 'DRAINING':
                set_container_instance_to_draining(cluster_arn, ecs_container_instance_arn, ecs_client)

            time.sleep(30)
            publish_sns_message(topic_arn, message_data)
