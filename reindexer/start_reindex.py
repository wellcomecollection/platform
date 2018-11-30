#!/usr/bin/env python
# -*- encoding: utf-8

import math
import sys

import boto3
import click
import hcl
import requests


SOURCES = [
    "miro",
    "sierra",
    "sierra_items"
]

DESTINATIONS = [
    "catalogue",
    "reporting"
]


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
    yield {
        "maxRecords": max_records,
        "type": "PartialReindexParameters"
    }


def post_to_slack(slack_message):
    """
    Posts a message about the reindex in Slack, so we can track them.
    """
    # Get the non-critical Slack token.
    s3 = boto3.client("s3")
    tfvars_obj = s3.get_object(
        Bucket="wellcomecollection-platform-infra", Key="terraform.tfvars"
    )
    tfvars_body = tfvars_obj["Body"].read()
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
        webhook_url,
        json=slack_data,
        headers={"Content-Type": "application/json"},
    )
    resp.raise_for_status()


@click.command()
@click.option(
    "--src", type=click.Choice(SOURCES), required=True,
    prompt="Which source do you want to reindex? (%s)" % ", ".join(SOURCES),
    help="Name of the source to reindex"
)
@click.option(
    "--dst", type=click.Choice(DESTINATIONS), required=True,
    prompt="Which pipeline are you sending this to? (%s)" % ", ".join(DESTINATIONS),
    help="Name of the pipeline to receive the reindexed records"
)
@click.option(
    "--mode", type=click.Choice(["complete", "partial"]), required=True,
    prompt="Every record (complete) or just a few (partial)?",
    help="Should this reindex send every record (complete) or just a few (partial)?"
)
@click.option(
    "--reason", prompt="Why are you running this reindex?",
    help="The reason to run this reindex"
)
def start_reindex(src, dst, mode, reason):
    print(f"Starting a reindex {src!r} ~> {dst!r}")

    if mode == "complete":
        total_segments = how_many_segments(table_name=f"vhs-sourcedata-{src}")
        parameters = complete_reindex_parameters(total_segments)
    elif mode == "partial":
        max_records = click.prompt("How many records do you want to send?", default=10)
        parameters = partial_reindex_parameters(max_records)

    username = boto3.client("iam").get_user()["User"]["UserName"]
    slack_message = (
        f"*{username}* started a {mode} reindex *{src!r}* ~> *{dst!r}*\n"
        f"Reason: *{reason}*"
    )

    post_to_slack(slack_message)

    print(slack_message)

    print(src)
    print(dst)
    print(mode)
    print(reason)


if __name__ == '__main__':
    start_reindex()
