#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

Usage: send_reindex_messages.py complete --table=<TABLE_NAME> --topic=<TOPIC_NAME> --reason=<REASON> --total_segments=<COUNT> [--skip-pipeline-checks]
       send_reindex_messages.py partial --table=<TABLE_NAME> --topic=<TOPIC_NAME> --reason=<REASON> --max_records=<MAX>
       send_reindex_messages.py -h | --help

Actions:
  complete                  Trigger a complete reindex -- send every record in the
                            VHS to the downstream applications.
  partial                   Trigger a partial reindex -- only send a small number of
                            records to the downstream application.

Options:
  --source=<SOURCE_NAME>    Name of the source you want to reindex.
  --reason=<REASON>         An explanation of why you're running this reindex.
                            This will be printed in the Slack alert.
  --total_segments=<COUNT>  How many segments to divide the VHS table into.
  --max_records=<MAX>       What's the most number of records that should be sent?
  --skip-pipeline-checks    Don't check if the pipeline tables are clear
                            before running.
  -h --help                 Print this help message

"""

import json
import sys

import boto3
import docopt
import hcl
import requests
import tqdm


dynamodb = boto3.client("dynamodb")
iam = boto3.client("iam")
sns = boto3.client("sns")
sts = boto3.client("sts")


def run_reindex(
    *,
    reason,
    table_name,
    topic_name,
    skip_pipeline_checks,
    is_complete,
    is_partial,
    total_segments=None,
    max_records=None
):
    username = iam.get_user()["User"]["UserName"]

    if not skip_pipeline_checks:
        print("Checking pipeline is clear...")
        check_tables_are_clear()

    print(f"Triggering a reindex {table_name} ~> {topic_name}")

    if is_complete:
        assert total_segments is not None
        parameters = all_complete_parameters(total_segments=total_segments)
        slack_message = (
            f"*{username}* started a complete reindex *{table_name} ~> {topic_name}*\n"
            f"Reason: *{reason}* "
            f'({total_segments} segment{"s" if total_segments != 1 else ""})'
        )
    else:
        assert max_records is not None
        parameters = all_partial_parameters(max_records=max_records)
        slack_message = (
            f"*{username}* started a partial reindex *DDB: {table_name} ~> SNS: {topic_name}*\n"
            f"Reason: *{reason}* ({max_records})"
        )

    post_to_slack(slack_message=slack_message)

    for p in parameters:
        message = {
            "dynamoConfig": {"table": table_name},
            "snsConfig": {"topicArn": build_topic_arn(topic_name)},
            "parameters": p
        }

        print(message)


def main():
    args = docopt.docopt(__doc__)

    def int_or_none(value):
        if value is None:
            return None
        else:
            return int(value)

    run_reindex(
        reason=args["--reason"],
        table_name=args["--table"],
        topic_name=args["--topic"],
        skip_pipeline_checks=args["--skip-pipeline-checks"] or args["partial"],
        is_complete=args["complete"],
        total_segments=int_or_none(args["--total_segments"]),
        is_partial=args["partial"],
        max_records=int_or_none(args["--max_records"])
    )


def all_partial_parameters(max_records):
    yield {
        "maxRecords": max_records,
        "type": "PartialReindexParameters"
    }


def all_complete_parameters(total_segments):
    """
    Generates the parameters to be sent to SNS.
    """
    for i in range(total_segments):
        yield {
            "segment": i,
            "totalSegments": total_segments,
            "type": "CompleteReindexParameters",
        }
#
#
# def publish_messages(topic_arn, messages):
#     """Publish a sequence of messages to an SNS topic."""
#     for m in tqdm.tqdm(list(messages)):
#         resp = sns.publish(
#             TopicArn=topic_arn,
#             MessageStructure="json",
#             Message=json.dumps({"default": json.dumps(m)}),
#             Subject=f"Source: {__file__}",
#         )
#         assert resp["ResponseMetadata"]["HTTPStatusCode"] == 200, resp
#
#
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
        data=json.dumps(slack_data),
        headers={"Content-Type": "application/json"},
    )
    resp.raise_for_status()


def build_topic_arn(topic_name):
    """Given a topic name, return the topic ARN."""
    # https://stackoverflow.com/a/37723278/1558022
    account_id = sts.get_caller_identity().get("Account")

    return f"arn:aws:sns:eu-west-1:{account_id}:{topic_name}"


def check_tables_are_clear():
    """
    Check that all the tables in the pipeline are clear before starting
    a reindex.
    """
    table_names = dynamodb.list_tables()["TableNames"]

    for table_suffix in ["matcher-lock-table", "works-graph", "recorder"]:
        matching_tables = [t for t in table_names if t.endswith(table_suffix)]
        if len(matching_tables) == 0:
            print(f"Unable to find a pipeline table for {table_suffix!r}?")
            sys.exit(1)
        elif len(matching_tables) > 1:
            print(
                f"More than one pipeline table for {table_suffix!r}: {matching_tables}"
            )
            sys.exit(1)

        matching_table = matching_tables[0]

        resp = dynamodb.scan(TableName=matching_table, Limit=1)
        if resp["Items"]:
            print(
                f"Table {matching_table!r} is already populated!  Clear tables before reindexing."
            )
            sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
