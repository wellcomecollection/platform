#!/usr/bin/env python
# -*- encoding: utf-8
"""
Prints information about which version of the API is currently running,
so you can create a new set of pins.
"""

import difflib
import json
import os
import subprocess
import sys

import attr
import boto3
import hcl
import requests


ROOT = subprocess.check_output([
        'git', 'rev-parse', '--show-toplevel']).strip().decode('utf8')

API_TF = os.path.join(ROOT, 'catalogue_api', 'terraform')


@attr.s
class ApiConfiguration(object):
    name = attr.ib()
    api = attr.ib()
    nginx = attr.ib()


def bold(message):
    # This uses ANSI escape codes to print a message in a bright color
    # to make it stand out more in a console.
    return f'\033[91m{message}\033[0m'


def get_ecs_api_info(name):
    """
    Given the name of an API (remus or romulus), return the container
    versions which are currently running in ECS.
    """
    assert name in ('remus', 'romulus')

    ecs = boto3.client('ecs')
    resp = ecs.describe_services(
        cluster='catalogue-api',
        services=[f'catalogue-api-{name}']
    )
    assert len(resp['services']) == 1, resp
    task_definition = resp['services'][0]['taskDefinition']

    resp = ecs.describe_task_definition(
        taskDefinition=task_definition
    )
    assert len(resp['taskDefinition']['containerDefinitions']) == 2, resp
    containers = resp['taskDefinition']['containerDefinitions']
    images = [c['image'] for c in containers]

    # The names of the images are in the form:
    #
    #   {ecr_repo}/uk.ac.wellcome/{api|nginx_api}:{tag}
    #
    image_names = [name.split('/')[-1] for name in images]

    data = dict(name.split(':', 2) for name in image_names)

    return ApiConfiguration(
        name=name,
        api=data['api'],
        nginx=data['nginx_api-delta']
    )


def print_current_state(prod_api, staging_api):
    """
    Prints a summary of the current API state.
    """
    print('')
    print(f'The prod API is {bold(prod_api.name)}')
    print(f'- api   = {bold(prod_api.api)}')
    print(f'- nginx = {bold(prod_api.nginx)}')
    print('')

    print(f'The staging API is {bold(staging_api.name)}')
    print(f'- api   = {bold(staging_api.api)}')
    print(f'- nginx = {bold(staging_api.nginx)}')


def check_staging_api():
    """
    Check the responses in the staging and the prod API for a given Miro
    work are the same.
    """
    id = 'a22au6yn'
    includes = 'identifiers,items,thumbnail'

    print(f'Checking that responses for work {id} match...')

    prod_resp = requests.get(
        f'https://api.wellcomecollection.org/catalogue/v1/works/{id}',
        params={'includes': includes}
    )
    stage_resp = requests.get(
        f'https://api-stage.wellcomecollection.org/catalogue/v1/works/{id}',
        params={'includes': includes}
    )

    prod_json = prod_resp.json()
    stage_json = stage_resp.json()

    if prod_json == stage_json:
        print('OK!')
    else:
        print(bold('Responses do not match!'))

        prod_lines = json.dumps(prod_json, indent=2, sort_keys=True)
        stage_lines = json.dumps(stage_json, indent=2, sort_keys=True)

        for line in difflib.context_diff(
            prod_lines.splitlines(),
            stage_lines.splitlines(),
            fromfile='api.wellcomecollection.org',
            tofile='api-stage.wellcomecollection.org'
        ):
            print(line)

        sys.exit(1)


def read_local_terraform():
    """
    Returns a dict of API configuration data from the local Terraform config.
    """
    with open(os.path.join(API_TF, 'api_pins.tf')) as tf_file:
        return hcl.load(tf_file)['locals']


def write_new_terraform(new_prod_api, romulus_api, remus_api):
    """
    Write the new API configuration to Terraform.
    """
    with open(os.path.join(API_TF, 'api_pins.tf'), 'w') as tf_file:
        tf_file.write(f'''
locals {{
  production_api       = "{new_prod_api}"
  pinned_remus_api     = "{remus_api.api}"
  pinned_remus_nginx   = "{remus_api.nginx}"
  pinned_romulus_api   = "{romulus_api.api}"
  pinned_romulus_nginx = "{romulus_api.nginx}"
}}
'''.lstrip())


if __name__ == '__main__':
    existing_tf = read_local_terraform()

    prod_api = existing_tf['production_api']
    prod_api_info = get_ecs_api_info(prod_api)

    staging_api = 'remus' if prod_api == 'romulus' else 'romulus'
    staging_api_info = get_ecs_api_info(staging_api)

    print_current_state(prod_api=prod_api_info, staging_api=staging_api_info)

    print('\n---\n')

    if '--force' in sys.argv:
        print('Skipping check of staging/prod API...')
    else:
        check_staging_api()

    print('\n---\n')

    if prod_api == 'romulus':
        romulus_api = prod_api_info
        remus_api = staging_api_info
        new_prod_api = 'remus'
    else:
        romulus_api = staging_api_info
        remus_api = prod_api_info
        new_prod_api = 'romulus'

    write_new_terraform(
        new_prod_api=new_prod_api,
        romulus_api=romulus_api,
        remus_api=remus_api
    )

    print('Triggering terraform update...')
    subprocess.check_call(['make', 'catalogue_api-terraform-plan'], cwd=ROOT)

    print('')
    print('Once the change has successfully deployed, you can remove the')
    print('pins for the staging API.')
