import json
from mock import patch

import boto3
from moto import mock_ec2, mock_ecs, mock_autoscaling

import drain_ecs_container_instance



@mock_ec2
@mock_autoscaling
@patch('drain_ecs_container_instance.asg_client.complete_lifecycle_action')
def test_complete_ec2_shutdown_if_no_ecs_cluster(mock_complete_call):
    mock_asg_client = boto3.client('autoscaling')
    mock_ec2_client = boto3.client('ec2')

    auto_scaling_group_name = 'TestGroup1'

    mock_asg_client.create_launch_configuration(LaunchConfigurationName='TestLC')

    mock_asg_client.create_auto_scaling_group(AutoScalingGroupName=auto_scaling_group_name,
                                              MinSize=1,
                                              MaxSize=1,
                                              LaunchConfigurationName='TestLC')

    instances = mock_ec2_client.describe_instances()
    print(instances)
    instance_id = instances['Reservations'][0]['Instances'][0]['InstanceId']

    message = {
        "LifecycleHookName": "monitoring-cluster-LifecycleHook-OENP6M5XGYVM",
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

    drain_ecs_container_instance.main(event, None)

    calls = mock_complete_call.call_list
    assert len(calls) == 1
