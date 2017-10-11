#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Update a task based on a change in configuration.

Our applications load config from an S3 bucket at startup.  When the S3 config
changes, this Lambda is triggered.  We need to create a new task definition,
then tell the service to run with the new task definition, so the ECS
scheduler will bring up new instances with the updated config.

"""

import re

import boto3

from utils.ecs_utils import (
    clone_task_definition,
    get_latest_task_definition,
    identify_cluster_by_app_name
)


def clone_latest_task_definition(client, cluster, service):
    """
    Given a cluster and a service, clone the current task definition for
    the service and change it to use the new task definition.

    This triggers an update to the running application.
    """
    task_definition = get_latest_task_definition(client, cluster, service)
    print(f'The current task definition is {task_definition}, cloning')

    cloned_task = clone_task_definition(
        client=client, task_definition=task_definition
    )
    print(f'The cloned task definition is {cloned_task}, updating {service}')

    resp = client.update_service(
        cluster=cluster,
        service=service,
        taskDefinition=cloned_task
    )
    print(f'ECS response = {resp!r}')
    assert resp['ResponseMetadata']['HTTPStatusCode'] == 200


def trigger_config_update(app_name):
    """
    Trigger a config update for a given app.
    """
    print(f'Triggering config update for {app_name}...')
    client = boto3.client('ecs')
    cluster = identify_cluster_by_app_name(client=client, app_name=app_name)
    clone_latest_task_definition(
        client=client,
        cluster=cluster,
        service=app_name
    )


def parse_s3_event(event):
    """
    Given an event that comes from an S3 update to a config file, return
    the name of the app whose config has been updated.
    """
    records = event['Records']
    assert len(records) == 1
    changed_object_key = records[0]['s3']['object']['key']
    match = re.match(r'^config/prod/(?P<app>[a-z_]+)\.ini', changed_object_key)
    assert match is not None, changed_object_key
    return match.group('app')


def main(event, _):
    print(f'event = {event!r}')
    app_name = parse_s3_event(event=event)
    trigger_config_update(app_name=app_name)


if __name__ == '__main__':
    import os
    import sys

    if len(sys.argv) != 2:
        sys.exit(f'Usage: {os.path.basename(__file__)} <app_name>')

    trigger_config_update(sys.argv[1])
