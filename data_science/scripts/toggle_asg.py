#!/usr/bin/env python
# -*- encoding: utf-8
"""
Start/stop all the instances in an autoscaling group.

Usage: toggle_asg.py (--start | --stop | --status) [--type=(p2 | t2)]

Actions:
  --start                 Start the autoscaling group (set the desired count to 1).
  --stop                  Stop the autoscaling group (set the desired count to 0).
  --status                Report the current side of autoscaling group.
  --type=(p2 | t2)        AWS Instance type (valid values: p2,t2) defaults to t2

"""

import sys

import boto3
import docopt

from asg_utils import discover_asg, set_asg_size

if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    instance_type = args['--type'] or 't2'
    tag_name = 'jupyter-%s' % instance_type

    asg_client = boto3.client('autoscaling')
    asg = discover_asg(asg_client=asg_client, tag_name=tag_name)
    asg_name = asg['AutoScalingGroupName']

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

    elif args['--status']:
        actual_size = asg['DesiredCapacity']
        print('The current size of ASG group %r is %r' % (asg_name, actual_size))

    else:
        print(
            'Neither --start, --stop nor --status flags supplied?  args=%r' %
            args)
        sys.exit(1)
