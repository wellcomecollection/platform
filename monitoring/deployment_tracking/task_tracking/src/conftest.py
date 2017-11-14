import json

import boto3
from moto import mock_ec2, mock_autoscaling, mock_ecs
from moto.ec2 import utils as ec2_utils
import pytest


def pytest_runtest_setup(item):
    set_region()


@pytest.fixture()
def set_region():
    # Without this, boto3 is complaining about not having a region defined
    # in tests (despite one being set in the Travis env variables and passed
    # into the image).
    # TODO: Investigate this properly.
    boto3.setup_default_session(region_name='eu-west-1')


@pytest.fixture()
def moto_start(set_region):
    mock_autoscaling().start()
    mock_ec2().start()
    mock_ecs().start()
    yield
    mock_autoscaling().stop()
    mock_ec2().stop()
    mock_ecs().stop()


@pytest.fixture()
def autoscaling_group(moto_start):
    auto_scaling_group_name = 'TestGroup1'
    print(f"creating autoscaling group {auto_scaling_group_name}")
    fake_asg_client = boto3.client('autoscaling')
    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=auto_scaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    fake_ec2_client = boto3.client('ec2')
    instances = fake_ec2_client.describe_instances()
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']
    yield auto_scaling_group_name, instance_id


@pytest.fixture()
def ecs_cluster(autoscaling_group):
    fake_ecs_client = boto3.client('ecs')
    fake_ec2_client = boto3.client('ec2')

    cluster_name = 'test_ecs_cluster'
    print(f"Creating ecs cluster {cluster_name}")
    _, instance_id = autoscaling_group
    cluster_response = fake_ecs_client.create_cluster(
        clusterName=cluster_name
    )

    ec2 = boto3.resource('ec2')
    instance = ec2.Instance(instance_id)

    instance_id_document = json.dumps(
        ec2_utils.generate_instance_identity_document(instance)
    )

    container_instance_resp = fake_ecs_client.register_container_instance(
        cluster=cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    container_instance_arn = \
        container_instance_resp['containerInstance']['containerInstanceArn']
    cluster_arn = cluster_response['cluster']['clusterArn']

    yield cluster_name, cluster_arn, container_instance_arn


@pytest.fixture()
def ecs_task(ecs_cluster):
    fake_ecs_client = boto3.client('ecs')
    task_name = 'test_ecs_task'
    print(f"Creating task {task_name}")
    cluster_name, cluster_arn, container_instance_arn = ecs_cluster

    task_definition_response = fake_ecs_client.register_task_definition(
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

    task_definition = task_definition_response['taskDefinition']

    run_task_response = fake_ecs_client.run_task(
        cluster=cluster_name,
        overrides={},
        taskDefinition=task_name,
        count=1,
        startedBy='moto'
    )

    assert len(run_task_response['tasks']) == 1

    task = run_task_response['tasks'][0]

    tasks = fake_ecs_client.list_tasks(
        cluster=cluster_arn,
        containerInstance=container_instance_arn
    )

    assert len(tasks['taskArns']) == 1

    yield task, task_definition, cluster_name, cluster_arn, container_instance_arn
