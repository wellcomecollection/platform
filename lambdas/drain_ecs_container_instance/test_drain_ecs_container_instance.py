import json

import boto3
from mock import patch, Mock
from moto import mock_ec2, mock_autoscaling, mock_ecs, mock_sns, mock_sqs
from moto.ec2 import utils as ec2_utils

import drain_ecs_container_instance

mocked_clients = {}


@mock_ec2
@mock_autoscaling
def test_complete_ec2_shutdown_if_no_ecs_cluster():
    fake_asg_client = boto3.client('autoscaling')
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')

    auto_scaling_group_name = 'TestGroup1'
    lifecycle_hook_name = "monitoring-cluster-LifecycleHook-OENP6M5XGYVM"

    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=auto_scaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    instances = fake_ec2_client.describe_instances()
    print(instances)
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']

    message = {
        "LifecycleHookName": lifecycle_hook_name,
        "AccountId": "account_id",
        "RequestId": "f29364ad-8523-4d58-9a70-3537f4edec15",
        "LifecycleTransition": "autoscaling:EC2_INSTANCE_TERMINATING",
        "AutoScalingGroupName": auto_scaling_group_name,
        "Service": "AWS Auto Scaling",
        "Time": "2017-07-10T12:36:05.857Z",
        "EC2InstanceId": instance_id,
        "LifecycleActionToken": "78c16884-6bd4-4296-ac0c-2da9eb6a0d29"
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
            'EventVersion': '1.0',
            'Sns': {
                'Message': json.dumps(message),
                'MessageAttributes': {},
                'MessageId': 'a4416c50-9ec6-5a8e-934a-3d8de60d1428',
                'Signature': 'signature',
                'SignatureVersion': '1',
                'SigningCertUrl': 'https://certificate.pem',
                'Subject': None,
                'Timestamp': '2017-07-10T12:43:55.664Z',
                'TopicArn':
                    'arn:aws:sns:region:account_id:ec2_terminating_topic',
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}

    # Horrible hack be able to mock the autoscaling client:
    # mock_asg_client returns a function that behaves differently
    # based on the service_name parameter passed to a call to
    # boto3.client.
    #
    # I need to mock the autoscaling client only, to be able to assert
    # on what function is called on it (As far as I know moto
    # does not have this functionality).
    # The other clients don't have to be mocked but, as there is no way
    # to mock each client individually (patch needs in importable target
    # string), the only way is to patch the entire boto3.client function.
    def mock_asg_client():
        def patch(*args):
            if args[0] == 'autoscaling':
                client = Mock()
                mocked_clients[args[0]] = client
            elif args[0] == 'ec2':
                client = fake_ec2_client
            elif args[0] == 'ecs':
                client = fake_ecs_client
            else:
                raise Exception(f'Invalid {args[0]}')

            return client

        return patch

    with patch("boto3.client", new_callable=mock_asg_client):
        drain_ecs_container_instance.main(event, None)

        mocked_asg_client = mocked_clients['autoscaling']
        mocked_asg_client \
            .complete_lifecycle_action \
            .assert_called_once_with(
                LifecycleHookName=lifecycle_hook_name,
                AutoScalingGroupName=auto_scaling_group_name,
                LifecycleActionResult='CONTINUE',
                InstanceId=instance_id
            )


@mock_ec2
@mock_ecs
@mock_autoscaling
@mock_sns
@mock_sqs
def test_drain_ecs_instance_if_running_tasks():
    fake_asg_client = boto3.client('autoscaling')
    fake_ec2_client = boto3.client('ec2')
    fake_ecs_client = boto3.client('ecs')
    fake_sns_client = boto3.client('sns')
    fake_sqs_client = boto3.client('sqs')

    fake_sns_client.create_topic(Name="test-topic")

    response = fake_sns_client.list_topics()
    topic_arn = response["Topics"][0]['TopicArn']

    queue_name = "test-queue"
    queue = fake_sqs_client.create_queue(QueueName=queue_name)

    fake_sns_client.subscribe(
        TopicArn=topic_arn,
        Protocol="sqs",
        Endpoint=f"arn:aws:sqs:eu-west-1:123456789012:{queue_name}"
    )
    auto_scaling_group_name = 'TestGroup1'
    lifecycle_hook_name = "monitoring-cluster-LifecycleHook-OENP6M5XGYVM"

    fake_asg_client.create_launch_configuration(
        LaunchConfigurationName='TestLC'
    )

    fake_asg_client.create_auto_scaling_group(
        AutoScalingGroupName=auto_scaling_group_name,
        MinSize=1,
        MaxSize=1,
        LaunchConfigurationName='TestLC'
    )

    instances = fake_ec2_client.describe_instances()

    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']

    cluster_name = 'test_ecs_cluster'
    cluster_response = fake_ecs_client.create_cluster(
        clusterName=cluster_name
    )

    ec2 = boto3.resource('ec2')
    instance = ec2.Instance(instance_id)

    instance_id_document = json.dumps(
        ec2_utils.generate_instance_identity_document(instance)
    )

    container_instance_response = fake_ecs_client.register_container_instance(
        cluster=cluster_name,
        instanceIdentityDocument=instance_id_document
    )

    container_instance_arn = container_instance_response['containerInstance']['containerInstanceArn']
    cluster_arn = cluster_response['cluster']['clusterArn']

    fake_ec2_client.create_tags(
        Resources=[
            instance_id,
        ],
        Tags=[
            {
                'Key': 'clusterArn',
                'Value': cluster_arn
            },
            {
                'Key': 'containerInstanceArn',
                'Value': container_instance_arn
            }
        ]
    )

    task_name = 'test_ecs_task'
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
        cluster=cluster_name,
        overrides={},
        taskDefinition=task_name,
        count=1,
        startedBy='moto'
    )

    tasks = fake_ecs_client.list_tasks(
        cluster=cluster_arn,
        containerInstance=container_instance_arn
    )
    assert len(tasks['taskArns']) == 1

    message = {
        "LifecycleHookName": lifecycle_hook_name,
        "AccountId": "account_id",
        "RequestId": "f29364ad-8523-4d58-9a70-3537f4edec15",
        "LifecycleTransition": "autoscaling:EC2_INSTANCE_TERMINATING",
        "AutoScalingGroupName": auto_scaling_group_name,
        "Service": "AWS Auto Scaling",
        "Time": "2017-07-10T12:36:05.857Z",
        "EC2InstanceId": instance_id,
        "LifecycleActionToken": "78c16884-6bd4-4296-ac0c-2da9eb6a0d29"
    }

    event = {
        'Records': [{
            'EventSource': 'aws:sns',
            'EventSubscriptionArn':
                'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
            'EventVersion': '1.0',
            'Sns': {
                'Message': json.dumps(message),
                'MessageAttributes': {},
                'MessageId': 'a4416c50-9ec6-5a8e-934a-3d8de60d1428',
                'Signature': 'signature',
                'SignatureVersion': '1',
                'SigningCertUrl': 'https://certificate.pem',
                'Subject': None,
                'Timestamp': '2017-07-10T12:43:55.664Z',
                'TopicArn': topic_arn,
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}


    # Horrible hack be able to mock the autoscaling client:
    # mock_asg_client returns a function that behaves differently
    # based on the service_name parameter passed to a call to
    # boto3.client.
    #
    # I need to mock the autoscaling client only, to be able to assert
    # on what function is called on it (As far as I know moto
    # does not have this functionality).
    # The other clients don't have to be mocked but, as there is no way
    # to mock each client individually (patch needs in importable target
    # string), the only way is to patch the entire boto3.client function.
    def mock_asg_client():
        def patch(*args):
            if args[0] == 'autoscaling':
                client = Mock()
                mocked_clients[args[0]] = client
            elif args[0] == 'ec2':
                client = fake_ec2_client
            elif args[0] == 'ecs':
                client = fake_ecs_client
            elif args[0] == 'sns':
                client = fake_sns_client
            else:
                raise Exception(f'Invalid {args[0]}')

            return client

        return patch

    with patch("boto3.client", new_callable=mock_asg_client):
        drain_ecs_container_instance.main(event, None)

        container_instance_info = fake_ecs_client.describe_container_instances(
            cluster=cluster_name,
            containerInstances=[container_instance_arn]
        )

        assert container_instance_info['containerInstances'][0]['status'] == "DRAINING"

        mocked_asg_client = mocked_clients['autoscaling']
        mocked_asg_client \
            .record_lifecycle_action_heartbeat \
            .assert_called_once_with(
                LifecycleHookName=lifecycle_hook_name,
                AutoScalingGroupName=auto_scaling_group_name,
                LifecycleActionResult='CONTINUE',
                InstanceId=instance_id
            )

        messages = fake_sqs_client.receive_message(
            QueueUrl=queue['QueueUrl'],
            MaxNumberOfMessages=1
        )
        message_body = messages['Messages'][0]['Body']

        assert json.loads(message_body)['default'] == json.dumps(message)
