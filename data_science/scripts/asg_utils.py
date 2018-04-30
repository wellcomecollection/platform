# -*- encoding: utf-8


DATA_SCIENCE_ASG_TAGS = {
    'data_science': 'true',
}


def discover_asg_name(asg_client):
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

        if actual_tags == DATA_SCIENCE_ASG_TAGS:
            return asg_data['AutoScalingGroupName']

    else:
        raise RuntimeError(
            "Can't find an ASG with tags %r!" % DATA_SCIENCE_ASG_TAGS
        )
