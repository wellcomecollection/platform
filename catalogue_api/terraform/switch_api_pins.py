#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import os

import boto3
import hcl


TFVARS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'terraform_api.tfvars')


def read_config():
    return hcl.load(open(TFVARS))


def write_config(config):
    config_str = hcl.dumps(config, indent=2)
    with open(TFVARS, 'w') as f:
        f.write(config_str)


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
    d = read_config()
    write_config(d)
    print(read_current_api_pins('remus'))
