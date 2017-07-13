import json

import boto3
from mock import patch, Mock
from moto import mock_ec2, mock_autoscaling

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

    fake_asg_client.create_launch_configuration(LaunchConfigurationName='TestLC')

    fake_asg_client.create_auto_scaling_group(AutoScalingGroupName=auto_scaling_group_name,
                                              MinSize=1,
                                              MaxSize=1,
                                              LaunchConfigurationName='TestLC')

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
            'EventSubscriptionArn': 'arn:aws:sns:region:account_id:ec2_terminating_topic:stuff',
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
                'TopicArn': 'arn:aws:sns:region:account_id:ec2_terminating_topic',
                'Type': 'Notification',
                'UnsubscribeUrl': 'https://unsubscribe-url'
            }}]}

    # Horrible hack be able to mock the autoscaling client:
    # client_patcher returns a function that behaves differently
    # based on the service_name parameter passed to a call to
    # boto3.client.
    #
    # I need to mock the autoscaling client only, to be able to assert
    # on what function are called on it (As far as I know moto
    # does not have this functionality).
    # The other clients don't have to be mocked but, as there is no way
    # to mock each client individually (patch needs in importable target
    # string), the only way is to patch the entire boto3.client function.
    def client_patcher():
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

    with patch("boto3.client", new_callable=client_patcher):
        drain_ecs_container_instance.main(event, None)

        mocked_clients['autoscaling'].complete_lifecycle_action.assert_called_once_with(
            LifecycleHookName=lifecycle_hook_name,
            AutoScalingGroupName=auto_scaling_group_name,
            LifecycleActionResult='CONTINUE',
            InstanceId=instance_id
        )
