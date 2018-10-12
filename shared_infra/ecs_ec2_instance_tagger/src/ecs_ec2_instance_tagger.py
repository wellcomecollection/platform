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

import json
import os

import boto3
from botocore.exceptions import ClientError
from wellcome_aws_utils.lambda_utils import log_on_error


def create_tags(ec2_client, ec2_instance_id, event_detail):
    """
    Given an EC2 Instance ID, and event['detail'] from an ECS Container
    Instance State Change, updates EC2 Instance tags with clusterArn &
    containerInstanceArn

    Returns the SDK response to the create_tags call.
    """

    cluster_arn = event_detail["clusterArn"]
    container_instance_arn = event_detail["containerInstanceArn"]

    print(f"Tag {ec2_instance_id} clusterArn: {cluster_arn}")
    print(f"Tag {ec2_instance_id} containerInstanceArn: {container_instance_arn}")

    return ec2_client.create_tags(
        Resources=[ec2_instance_id],
        Tags=[
            {"Key": "clusterArn", "Value": event_detail["clusterArn"]},
            {
                "Key": "containerInstanceArn",
                "Value": event_detail["containerInstanceArn"],
            },
        ],
    )


@log_on_error
def main(event, _):
    ec2_client = boto3.client("ec2")
    s3_client = boto3.client("s3")

    bucket_name = os.environ["BUCKET_NAME"]
    object_path = os.environ["OBJECT_PATH"]

    ec2_instance_id = event["detail"]["ec2InstanceId"]

    try:
        s3_client.head_object(
            Bucket=bucket_name, Key=f"{object_path}/{ec2_instance_id}"
        )
    except ClientError as ex:
        if ex.response["Error"]["Code"] == "404":
            print(f"{ec2_instance_id} not seen yet, tagging!")

            response = create_tags(ec2_client, ec2_instance_id, event["detail"])

            print(f"response = {response!r}")
        else:
            raise

    s3_client.put_object(
        Bucket=bucket_name,
        Key=f"{object_path}/{ec2_instance_id}",
        Body=json.dumps(event),
    )

    print(f"{ec2_instance_id} tagged.")
