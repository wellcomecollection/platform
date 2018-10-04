import os
import json

import boto3
from moto import mock_s3, mock_ec2

import ecs_ec2_instance_tagger


def create_ecs_container_instance_state_change(
    instance_id, cluster_arn, container_instance_arn
):

    return f"""
    {{
      "version": "0",
      "id": "8952ba83-7be2-4ab5-9c32-6687532d15a2",
      "detail-type": "ECS Container Instance State Change",
      "source": "aws.ecs",
      "account": "111122223333",
      "time": "2016-12-06T16:41:06Z",
      "region": "us-east-1",
      "resources": [
        "{container_instance_arn}"
      ],
      "detail": {{
        "agentConnected": true,
        "attributes": [
          {{
            "name": "foo"
          }}
        ],
        "clusterArn": "{cluster_arn}",
        "containerInstanceArn": "{container_instance_arn}",
        "ec2InstanceId": "{instance_id}",
        "registeredResources": [
          {{
            "name": "CPU",
            "type": "INTEGER",
            "integerValue": 2048
          }}
        ],
        "remainingResources": [
          {{
            "name": "CPU",
            "type": "INTEGER",
            "integerValue": 1988
          }}
        ],
        "status": "ACTIVE",
        "version": 14801,
        "versionInfo": {{
          "agentHash": "aebcbca",
          "agentVersion": "1.13.0",
          "dockerVersion": "DockerVersion: 1.11.2"
        }},
        "updatedAt": "2016-12-06T16:41:06.991Z"
      }}
    }}
    """


bucket_name = "bukkit"

object_path = "tmp/ecs_ec2_instance_tagger"

base_arn = "arn:aws:ecs:us-east-1:111122223333"

cluster_arn = f"{base_arn}:cluster/default"

container_instance_arn = f"{base_arn}:container-instance/foo"


@mock_ec2
@mock_s3
def test_ecs_ec2_instance_tagger_creates_s3_cache():
    instance_id = "i-0209cc3b3d10166bf"

    s3_client = boto3.client("s3")
    s3_client.create_bucket(Bucket=bucket_name)

    event = create_ecs_container_instance_state_change(
        instance_id, cluster_arn, container_instance_arn
    )

    os.environ["BUCKET_NAME"] = bucket_name
    os.environ["OBJECT_PATH"] = object_path

    ecs_ec2_instance_tagger.main(json.loads(event), {})

    body = s3_client.get_object(Bucket=bucket_name, Key=f"{object_path}/{instance_id}")[
        "Body"
    ].read()

    assert json.loads(body) == json.loads(event)


@mock_ec2
@mock_s3
def test_ecs_ec2_instance_tagger_creates_ec2_tags():
    ec2_client = boto3.client("ec2")

    s3_client = boto3.client("s3")
    s3_client.create_bucket(Bucket=bucket_name)

    response = ec2_client.run_instances(ImageId="ami-image-id", MaxCount=1, MinCount=1)

    instance_id = response["Instances"][0]["InstanceId"]

    event = create_ecs_container_instance_state_change(
        instance_id, cluster_arn, container_instance_arn
    )

    os.environ["BUCKET_NAME"] = bucket_name
    os.environ["OBJECT_PATH"] = object_path

    ecs_ec2_instance_tagger.main(json.loads(event), {})

    response = ec2_client.describe_instances(InstanceIds=[instance_id])

    actual_tags = response["Reservations"][0]["Instances"][0]["Tags"]

    expected_tags = [
        {"Key": "clusterArn", "Value": cluster_arn},
        {"Key": "containerInstanceArn", "Value": container_instance_arn},
    ]

    assert expected_tags == actual_tags


@mock_ec2
@mock_s3
def test_ecs_ec2_instance_tagger_creates_ec2_tags_only_once():
    ec2_client = boto3.client("ec2")

    s3_client = boto3.client("s3")
    s3_client.create_bucket(Bucket=bucket_name)

    response = ec2_client.run_instances(ImageId="ami-image-id", MaxCount=1, MinCount=1)

    instance_id = response["Instances"][0]["InstanceId"]

    event = create_ecs_container_instance_state_change(
        instance_id, cluster_arn, container_instance_arn
    )

    os.environ["BUCKET_NAME"] = bucket_name
    os.environ["OBJECT_PATH"] = object_path

    ecs_ec2_instance_tagger.main(json.loads(event), {})

    new_event = create_ecs_container_instance_state_change(instance_id, "foo", "bar")

    ecs_ec2_instance_tagger.main(json.loads(new_event), {})

    response = ec2_client.describe_instances(InstanceIds=[instance_id])

    actual_tags = response["Reservations"][0]["Instances"][0]["Tags"]

    expected_tags = [
        {"Key": "clusterArn", "Value": cluster_arn},
        {"Key": "containerInstanceArn", "Value": container_instance_arn},
    ]

    assert expected_tags == actual_tags
