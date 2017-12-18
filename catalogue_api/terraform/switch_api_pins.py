#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import os

import boto3
import hcl


TFVARS = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'terraform_api.tfvars')


def read_config():
    return hcl.load(open(TFVARS))


def write_config(config):
    config_str = f'''
production_api   = "{config["production_api"]}"
pinned_api       = "{config["pinned_api"]}"
pinned_api_nginx = "{config["pinned_api_nginx"]}"
'''.lstrip()
    with open(TFVARS, 'w') as f:
        f.write(config_str)


def read_current_api_version(name):
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

    if d['production_api'] == 'remus':
        new_api = 'romulus'
    elif d['production_api'] == 'romulus':
        new_api = 'remus'
    else:
        raise RuntimeError(
            "Unrecognised value in production_api: %r" % d['production_api']
        )

    print('Switching the production API variables to %s' % new_api)
    d['production_api'] = new_api

    print('Reading current ECS versions...')
    existing_state = read_current_api_version(new_api)

    d['pinned_api'] = existing_state['app']
    print('Current API version   = %s' % d['pinned_api'])

    d['pinned_api_nginx'] = existing_state['nginx']
    print('Current nginx version = %s' % d['pinned_api_nginx'])

    write_config(d)

    print("")
    print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
    print("@                                                                      @")
    print("@ Please review these changes before you plan/apply.                   @")
    print("@                                                                      @")
    print("@ In particular, make sure the Elasticsearch config variables for      @")
    print("@ the new versions of the API are up-to-date.                          @")
    print("@                                                                      @")
    print("@ If you're happy with the changes, don't forget to commit them!       @")
    print("@                                                                      @")
    print("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
