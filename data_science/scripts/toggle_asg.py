#!/usr/bin/env python
# -*- encoding: utf-8
"""
Start/stop all the instances in an autoscaling group.

Usage: toggle_asg.py (--start | --stop | --status | --keep-on | --allow-scale-down) [--type=(p2 | t2)]

Actions:
  --start                 Start the autoscaling group (set the desired count to 1).
  --stop                  Stop the autoscaling group (set the desired count to 0).
  --status                Report the current side of autoscaling group.
  --keep-on               Keeps the autoscaling group desired count at 1 (disables auto scale down)
  --allow-scale-down      Allows the automatic scaling action to be set desired count to 0 (enables auto scale down)
  --type=(p2 | t2)        AWS Instance type (valid values: p2,t2) defaults to t2

"""

import sys

import boto3
import docopt

from asg_utils import discover_asg, set_asg_size


def will_stay_up(client, scaling_group_name):
    response = client.describe_scheduled_actions(
        AutoScalingGroupName=scaling_group_name,
        ScheduledActionNames=['ensure_down']
    )

    if(response['ScheduledUpdateGroupActions']):
        desired_capacity = response['ScheduledUpdateGroupActions'][0]['DesiredCapacity']
        will_stay_up = bool(desired_capacity)
    else:
        raise ValueError('No ScheduledUpdateGroupActions matching name "ensure_down"')

    return will_stay_up


def keep_on(client, scaling_group_name):
    client.put_scheduled_update_group_action(
        AutoScalingGroupName=scaling_group_name,
        ScheduledActionName='ensure_down',
        Recurrence='0 20 * * *',
        DesiredCapacity=1
    )
    print("No scaling action will take place.")


def allow_scale_down(client, scaling_group_name):
    client.put_scheduled_update_group_action(
        AutoScalingGroupName=scaling_group_name,
        ScheduledActionName='ensure_down',
        Recurrence='0 20 * * *',
        DesiredCapacity=0
    )
    print("Scaling to 0 instances will take place this evening.")


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    instance_type = args['--type'] or 't2'
    tag_name = 'jupyter-%s' % instance_type

    asg_client = boto3.client('autoscaling')
    asg = discover_asg(asg_client=asg_client, tag_name=tag_name)
    asg_name = asg['AutoScalingGroupName']

    # Starting or stopping
    if args['--start'] or args['--stop']:
        if args['--start']:
            desired_size = 1
        else:
            desired_size = 0

        set_asg_size(
            asg_client=asg_client,
            asg_name=asg_name,
            desired_size=desired_size
        )

    # Scaling rules
    elif args['--keep-on'] or args['--allow-scale-down']:
        if args['--keep-on']:
            keep_on(asg_client, asg_name)
        else:
            allow_scale_down(asg_client, asg_name)

    # Report status
    elif args['--status']:
        actual_size = asg['DesiredCapacity']
        instance_count = len(asg['Instances'])

        if instance_count == 1:
            instance_str = '1 instance'
        elif instance_count == 0:
            instance_str = 'no instances'
        else:
            instance_str = '%d instances' % instance_count

        if(will_stay_up(asg_client, asg_name)):
            scaling_status = 'OFF'
        else:
            scaling_status = 'ON'

        print(
            'The desired size of ASG group %r is %r, with %s running' %
            (asg_name, actual_size, instance_str)
        )

        print(
            'The scaling rule to turn the ASG off overnight is %r' %
            (scaling_status)
        )

    else:
        print(
            'Neither --start, --stop nor --status flags supplied?  args=%r' %
            args)
        sys.exit(1)
