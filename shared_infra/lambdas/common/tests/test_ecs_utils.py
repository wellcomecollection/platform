import json

import boto3
from botocore.exceptions import ClientError
from mock import Mock
from moto import mock_ecs, mock_ec2
from moto.ec2 import utils as moto_ec2_utils
import pytest

from utils import ecs_utils


cluster_name = 'my_cluster'
service_name = 'my_service'


def ecs_cluster(ecs_client):
    cluster_response = ecs_client.create_cluster(clusterName=cluster_name)

    cluster_arn = cluster_response['cluster']['clusterArn']

    task_definition_response = ecs_client.register_task_definition(
        family='my_family',
        containerDefinitions=[]
    )

    task_definition_arn = (
        task_definition_response['taskDefinition']['taskDefinitionArn']
    )

    service_response = ecs_client.create_service(
        cluster='my_cluster',
        serviceName=service_name,
        taskDefinition=task_definition_arn,
        desiredCount=0
    )

    service_arn = service_response['service']['serviceArn']

    return task_definition_arn, service_arn, cluster_arn


@mock_ecs
def test_get_cluster_arns():
    ecs_client = boto3.client('ecs')
    _, _, cluster_arn = ecs_cluster(ecs_client)

    actual_cluster_list = ecs_utils.get_cluster_arns(ecs_client)

    assert actual_cluster_list == [cluster_arn]


def test_get_cluster_arns_throws_EcsThrottleException():
    mock_ecs_client = Mock()
    mock_ecs_client.list_clusters.side_effect = ClientError(
        error_response={
            'Error': {
                'Code': 'ThrottlingException'
            }
        },
        operation_name="foo"
    )

    with pytest.raises(ecs_utils.EcsThrottleException):
        ecs_utils.get_cluster_arns(mock_ecs_client)


@mock_ecs
def test_get_service_arns():
    ecs_client = boto3.client('ecs')
    _, service_arn, cluster_arn = ecs_cluster(ecs_client)

    actual_service_list = (
        ecs_utils.get_service_arns(ecs_client, cluster_arn)
    )

    assert actual_service_list == [service_arn]


def test_get_service_arns_throws_EcsThrottleException():
    mock_ecs_client = Mock()
    mock_ecs_client.list_services.side_effect = ClientError(
        error_response={'Error': {
            'Code': 'ThrottlingException'
        }},
        operation_name="foo"
    )

    with pytest.raises(ecs_utils.EcsThrottleException):
        ecs_utils.get_service_arns(mock_ecs_client, 'foo/bar')


@mock_ecs
def test_describe_cluster():
    ecs_client = boto3.client('ecs')
    _, service_arn, cluster_arn = ecs_cluster(ecs_client)

    actual_cluster_description = ecs_utils.describe_cluster(
        ecs_client,
        cluster_arn
    )

    actual_cluster_arn = (
        actual_cluster_description['clusterArn']
    )

    assert actual_cluster_arn == cluster_arn


def test_describe_cluster_throws_EcsThrottleException():
    mock_ecs_client = Mock()
    mock_ecs_client.describe_clusters.side_effect = ClientError(
        error_response={'Error': {
            'Code': 'ThrottlingException'
        }},
        operation_name="foo"
    )

    with pytest.raises(ecs_utils.EcsThrottleException):
        ecs_utils.describe_cluster(mock_ecs_client, 'foo/bar')


@mock_ecs
def test_describe_service():
    ecs_client = boto3.client('ecs')
    _, service_arn, cluster_arn = ecs_cluster(ecs_client)

    actual_service_description = ecs_utils.describe_service(
        ecs_client,
        cluster_arn,
        service_arn
    )

    actual_service_arn = (
        actual_service_description['serviceArn']
    )

    assert actual_service_arn == service_arn


def test_describe_service_throws_EcsThrottleException():
    mock_ecs_client = Mock()
    mock_ecs_client.describe_services.side_effect = ClientError(
        error_response={'Error': {
            'Code': 'ThrottlingException'
        }},
        operation_name="foo"
    )

    with pytest.raises(ecs_utils.EcsThrottleException):
        ecs_utils.describe_service(mock_ecs_client, 'foo/bar', 'bat/baz')


@mock_ec2
@mock_ecs
def test_run_task():
    ecs_client = boto3.client('ecs')
    ec2 = boto3.resource('ec2', region_name='us-east-1')

    cluster_response = ecs_client.create_cluster(clusterName=cluster_name)
    cluster_arn = cluster_response['cluster']['clusterArn']

    test_instance = ec2.create_instances(
        ImageId="ami-1234abcd",
        MinCount=1,
        MaxCount=1,
    )[0]

    instance_id_document = json.dumps(
        moto_ec2_utils.generate_instance_identity_document(test_instance)
    )

    ecs_client.register_container_instance(
        cluster=cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    task_definition_response = ecs_client.register_task_definition(
        family='my_family',
        containerDefinitions=[]
    )
    task_definition_arn = (
        task_definition_response['taskDefinition']['taskDefinitionArn']
    )

    started_by = "started_by"

    response = ecs_utils.run_task(
        ecs_client,
        cluster_name,
        task_definition_arn,
        started_by)

    assert len(response["failures"]) == 0
    assert len(response["tasks"]) == 1

    assert response["tasks"][0]["taskDefinitionArn"] == task_definition_arn
    assert response["tasks"][0]["clusterArn"] == cluster_arn
    assert response["tasks"][0]["startedBy"] == started_by
