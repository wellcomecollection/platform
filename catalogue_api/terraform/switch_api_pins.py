#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import boto3


def read_current_api_pins(name):
    """
    For an API (remus/romulus), return the _current_ versions of the API
    and nginx that are running in ECS.
    """
    ecs = boto3.client('ecs')

    resp = ecs.describe_services(
        cluster='api_cluster',
        services=[f'api_{name}_v1']
    )
    services = resp['services']
    assert len(services) == 1

    task_definition = ecs.describe_task_definition(
        taskDefinition = services[0]['taskDefinition']
    )
    containers = task_definition['taskDefinition']['containerDefinitions']
    assert len(containers) == 2

    app_container = [c for c in containers if c['name'] == 'app'][0]
    nginx_container = [c for c in containers if c['name'] == 'nginx'][0]

    app_image = app_container['image'].split(':')[-1]
    nginx_image = nginx_container['image'].split(':')[-1]

    return {'app': app_image, 'nginx': nginx_image}


if __name__ == '__main__':
    print(read_current_api_pins('remus'))
