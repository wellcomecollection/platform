#!/usr/bin/env python
# -*- encoding: utf-8
"""
Store a config variable in SSM under the key structure

    /{project_id}/{config_key}

This script can store a regular config key (unencrypted) or an encrypted key.

"""

import getpass
import sys

import boto3
from botocore.exceptions import ClientError
import click


secrets_client = boto3.client("secretsmanager")


@click.command()
@click.option("--project_id", prompt="What is the project ID?", required=True)
@click.option("--config_key", prompt="What is the config key?", required=True)
def store_config_key(project_id, config_key):
    name = f"{project_id}/{config_key}"
    config_value = getpass.getpass("Config value: ")

    try:
        resp = secrets_client.create_secret(
            Name=name,
            Description=f"Config secret populated by {__file__}",
            SecretString=config_value,
        )
    except ClientError as err:
        if err.response["Error"]["Code"] == "ResourceExistsException":
            resp = secrets_client.put_secret_value(
                SecretId=name, SecretString=config_value
            )

            if resp["ResponseMetadata"]["HTTPStatusCode"] != 200:
                print(f"Unexpected error from PutSecretValue: {resp}")
                sys.exit(1)
        else:
            raise
    else:
        if resp["ResponseMetadata"]["HTTPStatusCode"] != 200:
            print(f"Unexpected error from CreateSecret: {resp}")
            sys.exit(1)

    print(
        f"""
{name} -> [secret]

You can reference this secret in an ECS task definition in Terraform:

\033[91msecret_app_env_vars = {{
  {config_key} = "{name}"
}}

secret_app_env_vars_length = 1
""".strip()
    )


if __name__ == "__main__":
    store_config_key()
