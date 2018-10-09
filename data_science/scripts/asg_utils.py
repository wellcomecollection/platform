# -*- encoding: utf-8

import sys


def discover_asg(asg_client, tag_name):
    """
    Return data about the first autoscaling group whose tag "name" matches
    the supplied tag_name.
    """
    # This API is paginated, but for now we assume we have less than 100 ASGs
    # to check!
    resp = asg_client.describe_auto_scaling_groups(MaxRecords=100)

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
    for asg_data in resp["AutoScalingGroups"]:
        actual_tags = {t["Key"]: t["Value"] for t in asg_data["Tags"]}

        if actual_tags.get("Name") == tag_name:
            return asg_data

    sys.exit("Can't find an ASG with name %r!" % tag_name)


def set_asg_size(asg_client, asg_name, desired_size):
    """
    Set the size of an ASG to ``desired_size``.
    """
    print("Setting size of ASG group %r to %r" % (asg_name, desired_size))

    asg_client.update_auto_scaling_group(
        AutoScalingGroupName=asg_name,
        MinSize=desired_size,
        MaxSize=desired_size,
        DesiredCapacity=desired_size,
    )
