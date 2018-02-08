# -*- encoding: utf-8 -*-

import json

import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
from moto.ec2 import utils as ec2_utils
import pytest


@pytest.fixture
def autoscaling_group_name():
    return 'TestGroup1'


@pytest.fixture()
def ec2_instance_id(autoscaling_group_name, moto_start):
    print(f'Creating autoscaling group {autoscaling_group_name}')

    fake_asg_client = boto3.client('autoscaling')
    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=autoscaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    fake_ec2_client = boto3.client('ec2')
    instances = fake_ec2_client.describe_instances()
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']
    yield instance_id


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


@pytest.fixture
def ecs_cluster_name():
    return 'test_ecs_cluster'


@pytest.fixture
def ecs_cluster_arn(moto_start):
    ecs_client = boto3.client('ecs')

    cluster_name = 'test_ecs_cluster'
    print(f'Creating ECS cluster {cluster_name}')
    resp = ecs_client.create_cluster(clusterName=cluster_name)

    yield resp['cluster']['clusterArn']


@pytest.fixture
def ecs_container_instance_arn(
    ec2_instance_id, ecs_cluster_name, ecs_cluster_arn
):
    ecs_client = boto3.client('ecs')
    ec2_client = boto3.client('ec2')
    ec2_resource = boto3.resource('ec2')

    instance = ec2_resource.Instance(ec2_instance_id)

    instance_id_document = json.dumps(
        ec2_utils.generate_instance_identity_document(instance)
    )

    resp = ecs_client.register_container_instance(
        cluster=ecs_cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    instance_arn = resp['containerInstance']['containerInstanceArn']

    ec2_client.create_tags(
        Resources=[
            ec2_instance_id,
        ],
        Tags=[
            {
                'Key': 'clusterArn',
                'Value': ecs_cluster_arn
            },
            {
                'Key': 'containerInstanceArn',
                'Value': instance_arn
            }
        ]
    )

    yield instance_arn


@pytest.fixture()
def ecs_task(ecs_cluster_name, ecs_cluster_arn, ecs_container_instance_arn):
    fake_ecs_client = boto3.client('ecs')
    task_name = 'test_ecs_task'
    print(f"Creating task {task_name}")
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
        cluster=ecs_cluster_name,
        overrides={},
        taskDefinition=task_name,
        count=1,
        startedBy='moto'
    )

    tasks = fake_ecs_client.list_tasks(
        cluster=ecs_cluster_arn,
        containerInstance=ecs_container_instance_arn
    )
    assert len(tasks['taskArns']) == 1
    yield
