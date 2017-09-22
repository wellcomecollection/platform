#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Remove unused Docker containers and images from an ECS cluster.

Normally the ECS agent handles this for us, but the ECS cleaner only runs
on a fixed schedule.  Sometimes we fill up an instance disk very quickly
(e.g. if there are lots of failed deployments in quick succession).  This
allows us to clean up a cluster without waiting for the ECS agent.

Usage:
  ecs_clean_cluster.py --key=<PRIV_KEY> --cluster=<CLUSTER_NAME>
  ecs_clean_cluster.py -h | --help

Options:
  --cluster=<CLUSTER_NAME>  Name of the ECS cluster to clean.
  --key=<PRIV_KEY>          Path to the SSH key for accessing EC2 instances.

"""

import subprocess

import boto3
import docopt


def get_ec2_dns_names(cluster):
    """
    Generates the public DNS names of instances in the cluster.
    """

    ecs = boto3.client('ecs')
    resp = ecs.list_container_instances(cluster='api_cluster')
    arns = resp['containerInstanceArns']

    resp = ecs.describe_container_instances(cluster='api_cluster', containerInstances=arns)
    instance_ids = [e['ec2InstanceId'] for e in resp['containerInstances']]

    ec2 = boto3.client('ec2')
    resp = ec2.describe_instances(InstanceIds=instance_ids)

    for r in resp['Reservations']:
        for i in r['Instances']:
            yield i['PublicDnsName']


def main():
    args = docopt.docopt(__doc__)

    for name in get_ec2_dns_names(args['--cluster']):
        print(f'*** {name}')
        proc = subprocess.Popen([
            'ssh', '-i', args['--key'],
            f'core@{name}', 'docker rm $(docker ps -a -q)'
        ])
        proc.communicate()
        proc = subprocess.Popen([
            'ssh', '-i', args['--key'],
            f'core@{name}', 'docker rmi $(docker images -q)'
        ])
        proc.communicate()


if __name__ == '__main__':
    main()
