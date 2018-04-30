#!/usr/bin/env python
# -*- encoding: utf-8
"""
Connect to the first instance in an autoscaling group.

Usage: create_tunnel_to_asg.py --key=<KEY>

Actions:
  --key=<KEY>   Path to an SSH key with access to the instances in the ASG.

"""

import os
import subprocess
import sys

import boto3
import docopt

from asg_utils import discover_data_science_asg


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    key_path = os.path.abspath(args['--key'])
    assert os.path.exists(key_path)

    asg_client = boto3.client('autoscaling')
    asg_data = discover_data_science_asg(asg_client=asg_client)

    if len(asg_data['Instances']) == 0:
        sys.exit(
            'No instances running in ASG group %r; is it started?' %
            asg_data['AutoScalingGroupName']
        )

    in_service_instances = [
        inst
        for inst in asg_data['Instances']
        if inst['LifecycleState'] == 'InService'
    ]

    if len(in_service_instances) == 0:
        sys.exit(
            'No instances in ASG group %r are "InService"; wait a few seconds and try again.' %
            asg_data['AutoScalingGroupName']
        )

    instance_data = in_service_instances[0]
    instance_id = instance_data['InstanceId']

    print('Looking up EC2 instance ID %r' % instance_id)

    ec2_client = boto3.client('ec2')
    resp = ec2_client.describe_instances(InstanceIds=[instance_id])

    try:
        instances = resp['Reservations'][0]['Instances']
        ec2_data = instances[0]
        assert ec2_data['InstanceId'] == instance_id

        public_dns = ec2_data['PublicDnsName']
    except (IndexError, KeyError) as err:
        print('Unexpected error parsing the EC2 response: %r' % err)
        sys.exit('resp=%r' % resp)

    print('Connecting to instance %r' % public_dns)

    try:
        subprocess.check_call(['ssh', '-i', key_path, 'core@%s' % public_dns])
    except subprocess.CalledProcessError as err:
        sys.exit(err.returncode)
