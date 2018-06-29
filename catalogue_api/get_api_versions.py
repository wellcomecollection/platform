#!/usr/bin/env python
# -*- encoding: utf-8
"""
Prints information about which version of the API is currently running,
so you can create a new set of pins.
"""

import os

import boto3
import hcl


API_DIR = os.path.dirname(os.path.realpath(__file__))
API_TF = os.path.join(API_DIR, 'terraform')


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
        cluster='api_cluster',
        services=[f'api_{name}_v1']
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

    return dict(name.split(':', 2) for name in image_names)


if __name__ == '__main__':
    with open(os.path.join(API_TF, 'variables.tf')) as var_tf:
        variables = hcl.load(var_tf)['variable']

    prod_api = variables['production_api']['default']
    prod_api_info = get_ecs_api_info(prod_api)

    staging_api = 'remus' if prod_api == 'romulus' else 'romulus'
    staging_api_info = get_ecs_api_info(staging_api)

    print(f'The prod API is {bold(prod_api)}')
    print(f'- api   = {bold(prod_api_info["api"])}')
    print(f'- nginx = {bold(prod_api_info["nginx_api"])}')
    print('')

    print(f'The staging API is {bold(staging_api)}')
    print(f'- api   = {bold(staging_api_info["api"])}')
    print(f'- nginx = {bold(staging_api_info["nginx_api"])}')

    print('')
    print('---')
    print('')

    print('If you want to switch the prod/staging API, copy the following')
    print('Terraform into variables.tf:')
    print('')

    new_prod_api = staging_api
    new_prod_api_info = staging_api_info

    print(f'''
\033[32mvariable "production_api" {{
  description = "Which version of the API is production? (romulus | remus)"
  default     = "{staging_api}"
}}

variable "pinned_romulus_api" {{
  description = "Which version of the API image to pin romulus to, if any"
  default     = "{new_prod_api_info['api'] if new_prod_api == 'romulus' else staging_api_info['api']}"
}}

variable "pinned_romulus_api_nginx" {{
  description = "Which version of the nginx API image to pin romulus to, if any"
  default     = "{new_prod_api_info['nginx_api'] if new_prod_api == 'romulus' else staging_api_info['nginx_api']}"
}}

variable "pinned_remus_api" {{
  description = "Which version of the API image to pin remus to, if any"
  default     = "{new_prod_api_info['api'] if new_prod_api == 'remus' else staging_api_info['api']}"
}}

variable "pinned_remus_api_nginx" {{
  description = "Which version of the nginx API image to pin remus to, if any"
  default     = "{new_prod_api_info['nginx_api'] if new_prod_api == 'remus' else staging_api_info['nginx_api']}"
}}
'''.strip())

    print('\033[0m')
    print('---')
    print('')
    print("When you've deployed the change above successfully, then remove the")
    print('pins for {prod_api}.')
