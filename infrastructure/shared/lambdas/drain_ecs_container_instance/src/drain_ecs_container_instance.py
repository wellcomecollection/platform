#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This task tries to ensure graceful termination of ECS container instances.

The SNS topic "ec2_terminating" receives messages telling us about terminating
EC2 instances. If the terminating instance is part of an ECS cluster, it
drains the ECS tasks on the instance.
"""

import json
import time

import boto3

from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_aws_utils.sns_utils import publish_sns_message


def set_container_instance_to_draining(
    ecs_client, cluster_arn, ecs_container_instance_arn
):
    resp = ecs_client.update_container_instances_state(
        cluster=cluster_arn,
        containerInstances=[ecs_container_instance_arn],
        status="DRAINING",
    )

    print(f"Updating container instance response:\n{resp}")


def continue_lifecycle_action(
    asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name
):
    response = asg_client.complete_lifecycle_action(
        LifecycleHookName=lifecycle_hook_name,
        AutoScalingGroupName=asg_group_name,
        LifecycleActionResult="CONTINUE",
        InstanceId=ec2_instance_id,
    )

    print(f"response = {response!r}")


def get_ec2_tags(ec2_client, ec2_instance_id):
    ec2_instance_info = ec2_client.describe_instances(InstanceIds=[ec2_instance_id])

    tags = ec2_instance_info["Reservations"][0]["Instances"][0]["Tags"]
    tag_dict = {t["Key"]: t["Value"] for t in tags}

    print(f"tag_dict = {tag_dict!r}")
    return tag_dict


def drain_ecs_container_instance(asg_client, ec2_client, ecs_client, sns_client, event):
    topic_arn = event["Records"][0]["Sns"]["TopicArn"]
    message = event["Records"][0]["Sns"]["Message"]
    message_data = json.loads(message)

    # Check this is an interesting message
    if "AutoScalingGroupName" not in message_data:
        return
    if "LifecycleHookName" not in message_data:
        return

    ec2_instance_id = message_data["EC2InstanceId"]
    asg_group_name = message_data["AutoScalingGroupName"]

    lifecycle_hook_name = message_data["LifecycleHookName"]
    lifecycle_transition = message_data["LifecycleTransition"]
    lifecycle_action_token = message_data["LifecycleActionToken"]

    if lifecycle_transition == "autoscaling:EC2_INSTANCE_TERMINATING":
        tags_dict = get_ec2_tags(ec2_client, ec2_instance_id)

        try:
            cluster_arn = tags_dict["clusterArn"]
            ecs_container_instance_arn = tags_dict["containerInstanceArn"]
        except KeyError:
            continue_lifecycle_action(
                asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name
            )
            return

        running_tasks = ecs_client.list_tasks(
            cluster=cluster_arn, containerInstance=ecs_container_instance_arn
        )

        print(f"running tasks: {running_tasks['taskArns']}")

        if not running_tasks["taskArns"]:
            continue_lifecycle_action(
                asg_client, asg_group_name, ec2_instance_id, lifecycle_hook_name
            )
        else:
            asg_client.record_lifecycle_action_heartbeat(
                LifecycleHookName=lifecycle_hook_name,
                AutoScalingGroupName=asg_group_name,
                LifecycleActionToken=lifecycle_action_token,
                InstanceId=ec2_instance_id,
            )

            container_instance_info = ecs_client.describe_container_instances(
                cluster=cluster_arn, containerInstances=[ecs_container_instance_arn]
            )

            status = container_instance_info["containerInstances"][0]["status"]

            if status != "DRAINING":
                set_container_instance_to_draining(
                    ecs_client, cluster_arn, ecs_container_instance_arn
                )

            time.sleep(30)

            publish_sns_message(
                sns_client=sns_client, topic_arn=topic_arn, message=message_data
            )


@log_on_error
def main(event, _):
    asg_client = boto3.client("autoscaling")
    ec2_client = boto3.client("ec2")
    ecs_client = boto3.client("ecs")
    sns_client = boto3.client("sns")

    drain_ecs_container_instance(asg_client, ec2_client, ecs_client, sns_client, event)
