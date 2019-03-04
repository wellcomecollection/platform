#!/usr/bin/env python
# -*- encoding: utf-8
"""
Start/stop all the instances in an autoscaling group.

Usage: toggle_asg.py (--start | --stop | --enable-overnight | --disable-overnight) [--type=(p2 | t2)] [--namespace=<NAMESPACE>]
       toggle_asg.py [--type=(p2 | t2)] [--namespace=<NAMESPACE>]


Actions:
  --start                 Start the autoscaling group (set the desired count to 1).
  --stop                  Stop the autoscaling group (set the desired count to 0).
  --enable-overnight      Keeps the autoscaling group desired count at 1 (disables auto scale down)
  --disable-overnight     Allows the automatic scaling action to be set desired count to 0 (enables auto scale down)
  --type=(p2 | t2)        AWS Instance type (valid values: p2,t2) defaults to t2

"""

import boto3
import docopt

from asg_utils import discover_asg, set_asg_size


def update_desired_capacity_for_scheduled_action(
    client, scaling_group_name, scheduled_action_name, desired_capacity
):
    """Updates the desired capacity for a scheduled action.

    Keyword arguments:
    client -- boto3 autoscaling client
    scaling_group_name -- name of the autoscaling group to modify
    scheduled_action_name -- name of the scheduled action to modify
    desired_capacity -- desired capacity of scheduled action
    """

    action_str = (
        "Setting desired capacity of ScheduledAction {scheduled_action_name} to {desired_capacity}."
    ).format(
        scheduled_action_name=scheduled_action_name, desired_capacity=desired_capacity
    )

    print(action_str)

    client.put_scheduled_update_group_action(
        AutoScalingGroupName=scaling_group_name,
        ScheduledActionName=scheduled_action_name,
        Recurrence="0 20 * * *",
        DesiredCapacity=desired_capacity,
    )


def get_status(client, tag_name, scheduled_action_name):
    """Gets the status of an autoscaling group.

    Keyword arguments:
    client -- boto3 autoscaling client
    tag_name -- tags used to identify autoscaling group
    scheduled_action_name -- name of the scheduled action to get status for
    """
    scaling_group = discover_asg(asg_client=client, tag_name=tag_name)

    desired_capacity = scaling_group["DesiredCapacity"]
    instance_count = len(scaling_group["Instances"])
    scaling_group_name = scaling_group["AutoScalingGroupName"]

    response = client.describe_scheduled_actions(
        AutoScalingGroupName=scaling_group_name,
        ScheduledActionNames=[scheduled_action_name],
    )

    scheduled_desired_capacity = response["ScheduledUpdateGroupActions"][0][
        "DesiredCapacity"
    ]

    return {
        "scaling_group_name": scaling_group_name,
        "desired_capacity": desired_capacity,
        "instance_count": instance_count,
        "scheduled_desired_capacity": scheduled_desired_capacity,
    }


def print_status(status):
    """Prints the status of an autoscaling group.

    Keyword arguments:
    status -- status as returned from the get_status function
    """
    instance_str = (
        'The desired size of autoscaling group "{name}" is {desired}.\n\nThere are currently {count} running.\n'
    ).format(
        name=status["scaling_group_name"],
        desired=status["desired_capacity"],
        count=status["instance_count"],
    )

    if status["scheduled_desired_capacity"] == 1:
        scheduled_action_str = "The instances will continue running overnight.\n"
    else:
        scheduled_action_str = "The instances will be turned off at 8pm this evening.\n"

    print(instance_str)
    print(scheduled_action_str)


if __name__ == "__main__":
    args = docopt.docopt(__doc__)

    instance_type = args["--type"] or "t2"
    namespace = args["--namespace"] or "datascience-281118-notebook"
    tag_name = "jupyter-%s-%s" % (instance_type, namespace)
    scheduled_action_name = "ensure_down"

    client = boto3.client("autoscaling")
    scaling_group_name = discover_asg(asg_client=client, tag_name=tag_name)[
        "AutoScalingGroupName"
    ]

    if args["--start"]:
        set_asg_size(asg_client=client, asg_name=scaling_group_name, desired_size=1)

    if args["--stop"]:
        set_asg_size(asg_client=client, asg_name=scaling_group_name, desired_size=0)

    if args["--enable-overnight"]:
        update_desired_capacity_for_scheduled_action(
            client=client,
            scaling_group_name=scaling_group_name,
            scheduled_action_name=scheduled_action_name,
            desired_capacity=1,
        )

    if args["--disable-overnight"]:
        update_desired_capacity_for_scheduled_action(
            client=client,
            scaling_group_name=scaling_group_name,
            scheduled_action_name=scheduled_action_name,
            desired_capacity=0,
        )

    print("---\n")

    status = get_status(client, tag_name, scheduled_action_name)
    print_status(status)
