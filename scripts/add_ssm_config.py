#!/usr/bin/env python
# -*- encoding: utf-8
"""
Store a config variable in SSM under the key structure

    /{project_id}/config/{label}/{config_key}

This script can store a regular config key (unencrypted) or an encrypted key.

"""

import sys

import boto3
import click


ssm_client = boto3.client("ssm")


@click.command()
@click.option("--project_id", prompt="What is the project ID?", required=True)
@click.option("--label", default="prod", required=True)
@click.option("--config_key", prompt="What is the config key?", required=True)
@click.option("--config_value", prompt="What is the config value?", required=True)
def store_config_key(project_id, label, config_key, config_value):
    ssm_name = f"/{project_id}/config/{label}/{config_key}"

    resp = ssm_client.put_parameter(
        Name=ssm_name,
        Description=f"Config value populated by {__file__}",
        Value=config_value,
        Type="String",
        Overwrite=True,
    )

    if resp["ResponseMetadata"]["HTTPStatusCode"] == 200:
        print(
            f"""
{ssm_name} -> {config_value!r}

You can reference this config value in Terraform:

\033[91mdata "aws_ssm_parameter" "{config_key}" {{
  name = "{ssm_name}"
}}

locals {{
  {config_key}_value = "${{data.aws_ssm_parameter.{config_key}.value}}"
}}
""".strip()
        )
    else:
        print(f"Unexpected error: {resp}")
        sys.exit(1)


if __name__ == "__main__":
    store_config_key()
