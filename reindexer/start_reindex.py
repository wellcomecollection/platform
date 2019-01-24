#!/usr/bin/env python
# -*- encoding: utf-8

import json
import math
import sys

import boto3
import click
import hcl
import requests
import tqdm


SOURCES = ["miro", "sierra", "sierra_items"]

DESTINATIONS = ["catalogue", "reporting"]


def how_many_segments(table_name):
    """
    When we do a complete reindex, we need to tell the reindexer how many segments
    to use.  Each segment should contain ~1000 records, so we don't exhaust the
    memory in the reindexer.

    (The reindexer loads the contents of each segment into memory, so choosing overly
    large segment sizes causes it to fall over.)

    """
    dynamodb = boto3.client("dynamodb")
    resp = dynamodb.describe_table(TableName=table_name)

    # The item count isn't real-time; it gets updated every six hours or so.
    # In practice it's the right order of magnitude: if the table has lots of churn,
    # it's probably a bad time to reindex!
    try:
        item_count = resp["Table"]["ItemCount"]
    except KeyError:
        sys.exit("No such table {table_name!r}?")

    return int(math.ceil(item_count / 900))


def complete_reindex_parameters(total_segments):
    for segment in range(total_segments):
        yield {
            "segment": segment,
            "totalSegments": total_segments,
            "type": "CompleteReindexParameters",
        }


def partial_reindex_parameters(max_records):
    yield {"maxRecords": max_records, "type": "PartialReindexParameters"}


def read_from_s3(bucket, key):
    s3 = boto3.client("s3")
    obj = s3.get_object(Bucket=bucket, Key=key)
    return obj["Body"].read()


def post_to_slack(slack_message):
    """
    Posts a message about the reindex in Slack, so we can track them.
    """
    # Get the non-critical Slack token.
    tfvars_body = read_from_s3(
        bucket="wellcomecollection-platform-infra", key="terraform.tfvars"
    )
    tfvars = hcl.loads(tfvars_body)
    webhook_url = tfvars["non_critical_slack_webhook"]

    slack_data = {
        "username": "reindex-tracker",
        "icon_emoji": ":dynamodb:",
        "color": "#2E72B8",
        "title": "reindexer",
        "fields": [{"value": slack_message}],
    }

    resp = requests.post(
        webhook_url, json=slack_data, headers={"Content-Type": "application/json"}
    )
    resp.raise_for_status()


def get_reindexer_topic_arn():
    statefile_body = read_from_s3(
        bucket="wellcomecollection-platform-infra", key="terraform/reindexer.tfstate"
    )

    # The structure of the interesting bits of the statefile is:
    #
    #   {
    #       ...
    #       "modules": [
    #           {
    #               "path": ["root"],
    #               "outputs": {
    #                   "name_of_output": {
    #                       "value": "1234567890x",
    #                       ...
    #                   },
    #                   ...
    #               }
    #           },
    #           ...
    #       ]
    #   }
    #
    statefile_data = json.loads(statefile_body)
    modules = statefile_data["modules"]
    root_module = [m for m in modules if m["path"] == ["root"]][0]
    root_outputs = root_module["outputs"]
    return root_outputs["topic_arn"]["value"]


def publish_messages(job_config_id, topic_arn, parameters):
    """Publish a sequence of messages to an SNS topic."""
    sns = boto3.client("sns")
    for params in tqdm.tqdm(list(parameters)):
        to_publish = {"jobConfigId": job_config_id, "parameters": params}
        resp = sns.publish(
            TopicArn=topic_arn,
            MessageStructure="json",
            Message=json.dumps({"default": json.dumps(to_publish)}),
            Subject=f"Source: {__file__}",
        )
        assert resp["ResponseMetadata"]["HTTPStatusCode"] == 200, resp


@click.command()
@click.option(
    "--src",
    type=click.Choice(SOURCES),
    required=True,
    prompt="Which source do you want to reindex?",
    help="Name of the source to reindex",
)
@click.option(
    "--dst",
    type=click.Choice(DESTINATIONS),
    required=True,
    prompt="Which pipeline are you sending this to?",
    help="Name of the pipeline to receive the reindexed records",
)
@click.option(
    "--mode",
    type=click.Choice(["complete", "partial"]),
    required=True,
    prompt="Every record (complete) or just a few (partial)?",
    help="Should this reindex send every record (complete) or just a few (partial)?",
)
@click.option(
    "--reason",
    prompt="Why are you running this reindex?",
    help="The reason to run this reindex",
)
def start_reindex(src, dst, mode, reason):
    print(f"Starting a reindex {src!r} ~> {dst!r}")

    if mode == "complete":
        total_segments = how_many_segments(table_name=f"vhs-sourcedata-{src}")
        parameters = complete_reindex_parameters(total_segments)
    elif mode == "partial":
        max_records = click.prompt("How many records do you want to send?", default=10)
        parameters = partial_reindex_parameters(max_records)

    # TODO: This was broken by the move to AssumeRole, because the GetUser call
    # doesn't work in an IAM role.  When we agree a replacement, we should apply
    # it globally, including here.

    # username = boto3.client("iam").get_user()["User"]["UserName"]
    # slack_message = (
    #     f"*{username}* started a {mode} reindex *{src!r}* ~> *{dst!r}*\n"
    #     f"Reason: *{reason}*"
    # )

    # post_to_slack(slack_message)

    topic_arn = get_reindexer_topic_arn()

    publish_messages(
        job_config_id=f"{src}--{dst}", topic_arn=topic_arn, parameters=parameters
    )


if __name__ == "__main__":
    start_reindex()
