#!/usr/bin/env python
# -*- encoding: utf-8
"""
Connect to the first instance in an autoscaling group.

Usage: create_tunnel_to_asg.py [--key=<KEY>] [--port=<PORT>] [--type=(p2 | t2)] [--namespace=<NAMESPACE>]

Actions:
  --key=<KEY>             Path to an SSH key with access to the instances in the ASG.
  --port=<PORT>           Local port to use for the remote Jupyter notebook
                          (default: 8888).
  --type=(p2 | t2)        AWS Instance type (valid values: p2,t2) defaults to t2

"""

import os
import subprocess
import sys
import time

from os.path import expanduser

import boto3
import docopt

from asg_utils import discover_asg


def _default_ssh_key_path():
    return "%s/.ssh/wellcomedigitalplatform" % expanduser("~")


def _wait(message, wait=5):
    print("%s; waiting for %d seconds..." % (message, wait))
    time.sleep(wait)


def main():
    args = docopt.docopt(__doc__)

    key_path = args["--key"] or _default_ssh_key_path()
    namespace = args["--namespace"] or "datascience-281118-notebook"

    print("Using SSH key at path %r" % key_path)

    assert os.path.exists(key_path)

    port = args["--port"] or "8888"

    instance_type = args["--type"] or "t2"
    tag_name = "jupyter-%s-%s" % (instance_type, namespace)

    asg_client = boto3.client("autoscaling")

    while True:
        asg_data = discover_asg(asg_client=asg_client, tag_name=tag_name)

        if not asg_data["Instances"]:
            _wait("No instances in ASG group %r" % asg_data["AutoScalingGroupName"])
            continue

        in_service_instances = [
            inst
            for inst in asg_data["Instances"]
            if inst["LifecycleState"] == "InService"
        ]

        if not in_service_instances:
            _wait(
                'No instances in ASG group %r are "InService"'
                % asg_data["AutoScalingGroupName"]
            )
            continue

        assert len(in_service_instances) > 0
        break

    instance_data = in_service_instances[0]
    instance_id = instance_data["InstanceId"]

    print("Looking up EC2 instance ID %r" % instance_id)

    ec2_client = boto3.client("ec2")
    resp = ec2_client.describe_instances(InstanceIds=[instance_id])

    try:
        instances = resp["Reservations"][0]["Instances"]
        ec2_data = instances[0]
        assert ec2_data["InstanceId"] == instance_id

        public_dns = ec2_data["PublicDnsName"]
    except (IndexError, KeyError) as err:
        print("Unexpected error parsing the EC2 response: %r" % err)
        sys.exit("resp=%r" % resp)

    print("Connecting to instance %r" % public_dns)

    try:
        subprocess.check_call(
            [
                "ssh",
                # Use the provided SSH key to connect.
                "-i",
                key_path,
                # Create a tunnel to port 8888 (Jupyter) on the remote host
                "-L",
                "%s:%s:8888" % (port, public_dns),
                # Our data science AMI is based on Ubuntu
                "ubuntu@%s" % public_dns,
            ]
        )
    except subprocess.CalledProcessError as err:
        sys.exit(err.returncode)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
