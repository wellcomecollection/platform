#!/usr/bin/env python
# -*- encoding: utf-8
"""
Start/stop all the instances in an autoscaling group.

Usage: toggle_asg.py (--start | --stop)

Actions:
  --start           Start the autoscaling group (set the desired count to 1).
  --stop            Stop the autoscaling group (set the desired count to 0).

"""

import sys

import boto3
import docopt


DATA_SCIENCE_ASG_TAGS = {
    'data_science': 'true',
}


def discover_asg_name(asg_client, desired_tags):
    """
    Returns the name of the first autoscaling group whose tags exactly match
    the supplied input.
    """
    # This API is paginated, but for now we assume we have less than 100 ASGs
    # to check!
    resp = asg_client.describe_auto_scaling_groups(MaxRecords=100)

    for asg_data in resp['AutoScalingGroups']:

        # The structure of the response is a little awkward.  It's a list of
        # entries of the form:
        #
        #   {'Key': '<KEY>',
        #    'PropagateAtLaunch': (True|False),
        #    'ResourceId': '<RESOURCE_ID>',
        #    'ResourceType': 'auto-scaling-group',
        #    'Value': '<VALUE>'}]
        #
        # We only care about the tag values, so we extract them into a
        # Python dict.
        actual_tags = {t['Key']: t['Value'] for t in asg_data['Tags']}

        if desired_tags == actual_tags:
            return asg_data['AutoScalingGroupName']

    else:
        raise RuntimeError("Can't find an ASG with tags %r!" % desired_tags)


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    if args.get('--start'):
        desired_size = 1
    elif args.get('--stop'):
        desired_size = 0
    else:
        print('Neither --start nor --stop flags supplied?  args=%r' % args)
        sys.exit(1)

    asg_client = boto3.client('autoscaling')

    asg_name = discover_asg_name(
        asg_client=asg_client,
        desired_tags=DATA_SCIENCE_ASG_TAGS
    )

    print('Setting size of ASG group %r to %r' % (asg_name, desired_size))

    asg_client.update_auto_scaling_group(
        AutoScalingGroupName=asg_name,
        MinSize=desired_size,
        MaxSize=desired_size,
        DesiredCapacity=desired_size,
    )
