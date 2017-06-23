#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Tag EC2 instances with their cluster ARN and container instance ARN.
This allows us to easily follow an instance back to its ECS cluster.

This script is triggered by an ECS Container Instance State Change event.

Requires a Cloudwatch Event with pattern:

{
  "source": [
    "aws.ecs"
  ],
  "detail-type": [
    "ECS Container Instance State Change"
  ]
}

"""

import pprint

import boto3


def create_tags(ec2_client, ec2_instance_id, event_detail):
    """
    Given an EC2 Instance ID, and event['detail'] from an ECS Container
    Instance State Change, updates EC2 Instance tags with clusterArn &
    containerInstanceArn

    Returns the SDK response to the create_tags call.
    """
    return ec2_client.create_tags(
        Resources=[ec2_instance_id],
        Tags=[{
            "Key": "clusterArn",
            "Value": event_detail['clusterArn']
        },{
            "Key": "containerInstanceArn",
            "Value": event_detail['containerInstanceArn']
        }]
    )


def main(event, _):
    pprint.pprint(event)
    ec2_client = boto3.client('ec2')

    ec2_instance_id = event['detail']['ec2InstanceId']

    response = create_tags(ec2_client, ec2_instance_id, event['detail'])

    pprint.pprint(response)
