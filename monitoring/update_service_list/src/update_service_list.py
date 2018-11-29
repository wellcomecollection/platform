# -*- encoding: utf-8 -*-
"""
Publish a file containing a summary of ECS service status to S3.

This script runs when triggered from an ECS task state change stream.
It does not use the event data from the event.
"""

import datetime
import json
import os

import boto3

from wellcome_aws_utils.ecs_utils import (
    get_cluster_arns,
    get_service_arns,
    describe_service,
    describe_cluster,
    EcsThrottleException,
)


class DateTimeAwareEncoder(json.JSONEncoder):
    def default(self, o):
        if isinstance(o, datetime.date):
            return o.isoformat()

        return json.JSONEncoder.default(self, o)


def _create_event_dict(event):
    return {"timestamp": event["createdAt"].timestamp(), "message": event["message"]}


def _describe_task_definition(client, arn):
    return client.describe_task_definition(taskDefinition=arn)["taskDefinition"]


def _enrich_deployment(deployment, task_definition):
    task_definition_arn = deployment["taskDefinition"]

    task_definition["taskDefinitionArn"] = task_definition_arn
    deployment["taskDefinition"] = task_definition

    return deployment


def _create_container_definition_dict(container_definition):
    image_tag = container_definition["image"].split(":")[-1]

    return {
        "name": container_definition["name"],
        "image": container_definition["image"],
        "image_tag": image_tag,
        "cpu": container_definition["cpu"],
        "memory": container_definition["memory"],
        "essential": container_definition["essential"],
    }


def _create_task_definition_dict(task_definition):
    raw_container_definitions = task_definition["containerDefinitions"]

    container_definitions = [
        _create_container_definition_dict(raw_container_definition)
        for raw_container_definition in raw_container_definitions
    ]

    task_definition["containerDefinitions"] = container_definitions

    return task_definition


def _enrich_service(client, cluster_arn, service_arn):
    service = describe_service(client, cluster_arn, service_arn)

    raw_task_definitions = [
        _describe_task_definition(client, deployment["taskDefinition"])
        for deployment in service["deployments"]
    ]

    task_definitions = [
        _create_task_definition_dict(raw_task_definition)
        for raw_task_definition in raw_task_definitions
    ]

    zipped = zip(service["deployments"], task_definitions)

    enriched_deployments = [
        _enrich_deployment(deployment, task_definition)
        for deployment, task_definition in zipped
    ]

    # Only get the last 5 events as out of date events
    # are not useful
    enriched_events = [_create_event_dict(e) for e in service["events"][:5]]

    service["events"] = enriched_events
    service["deployments"] = enriched_deployments

    return service


def get_service_list(ecs_client, cluster_arn):
    return [
        _enrich_service(ecs_client, cluster_arn, service_arn)
        for service_arn in get_service_arns(ecs_client, cluster_arn)
    ]


def _enrich_cluster_list(ecs_client, cluster_arn):
    cluster = describe_cluster(ecs_client, cluster_arn)

    service_list = get_service_list(ecs_client, cluster_arn)

    return {
        "clusterName": cluster["clusterName"],
        "status": cluster["status"],
        "instanceCount": cluster["registeredContainerInstancesCount"],
        "serviceList": service_list,
    }


def get_cluster_list(ecs_client):
    return [
        _enrich_cluster_list(ecs_client, cluster_arn)
        for cluster_arn in get_cluster_arns(ecs_client)
    ]


def send_ecs_status_to_s3(service_snapshot, s3_client, bucket_name, object_key):
    return s3_client.put_object(
        ACL="public-read",
        Bucket=bucket_name,
        Key=object_key,
        Body=json.dumps(service_snapshot, cls=DateTimeAwareEncoder),
        CacheControl="max-age=0",
        ContentType="application/json",
    )


def create_boto_client(service, role_arn):
    sts_client = boto3.client("sts")

    assumed_role_object = sts_client.assume_role(
        RoleArn=role_arn, RoleSessionName="AssumeRoleSession"
    )

    credentials = assumed_role_object["Credentials"]

    return boto3.client(
        service_name="ecs",
        aws_access_key_id=credentials["AccessKeyId"],
        aws_secret_access_key=credentials["SecretAccessKey"],
        aws_session_token=credentials["SessionToken"],
    )


def main(event, _):
    assumable_roles = [s for s in os.environ["ASSUMABLE_ROLES"].split(",") if s]

    bucket_name = os.environ["BUCKET_NAME"]
    object_key = os.environ["OBJECT_KEY"]

    ecs_clients = (
        [create_boto_client("ecs", role_arn) for role_arn in assumable_roles]
    ) + [boto3.client("ecs")]

    try:
        cluster_lists = [get_cluster_list(ecs_client) for ecs_client in ecs_clients]
    except EcsThrottleException:
        # Fail gracefully
        return

    cluster_list = [item for sublist in cluster_lists for item in sublist]

    service_snapshot = {
        "clusterList": cluster_list,
        "lastUpdated": str(datetime.datetime.utcnow()),
    }

    s3_client = boto3.client("s3")

    send_ecs_status_to_s3(service_snapshot, s3_client, bucket_name, object_key)
