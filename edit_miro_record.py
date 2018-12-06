#!/usr/bin/env python
# -*- encoding: utf-8

# TODO: Conditional updates in DynamoDB; right now it's not safe for concurrent edits
# TODO: Support toggling the 'isClearedForCatalogueAPI' parameter

import contextlib
import copy
import datetime as dt
import hashlib
import json
import os
import subprocess
import sys
import tempfile

import boto3
from boto3.dynamodb.types import TypeDeserializer
import click


def sha256(s):
    h = hashlib.sha256()
    h.update(s)
    return h.hexdigest()


@contextlib.contextmanager
def edit_miro_vhs_record(miro_id, reason):
    dynamodb = boto3.client("dynamodb")
    s3 = boto3.client("s3")

    resp = dynamodb.get_item(
        TableName="vhs-sourcedata-miro", Key={"id": {"S": miro_id}}
    )

    try:
        item = resp["Item"]
    except KeyError:
        raise ValueError(f"Unable to find Miro ID {miro_id!r}")

    existing_record = json.loads(
        s3.get_object(
            Bucket=item["location"]["M"]["namespace"]["S"],
            Key=item["location"]["M"]["key"]["S"],
        )["Body"].read()
    )

    record = copy.deepcopy(existing_record)
    yield record

    if record == existing_record:
        print("No changes; not uploading a new version")
    else:
        username = boto3.client("iam").get_user()["User"]["UserName"]

        record["all_amendment_by"].append(f"aws:{username}")
        record["all_amendment_date"].append(dt.datetime.now().strftime("%d/%m/%Y"))
        record["all_amendment_note"].append(reason)

        json_string = json.dumps(record, separators=(",", ":")).encode("utf8")
        new_key = f"{miro_id}/{sha256(json_string)}.json"
        print(f"Uploading new object as {new_key}")

        item["location"]["M"]["key"]["S"] = new_key
        item["version"]["N"] = str(int(item["version"]["N"]) + 1)

        s3.put_object(
            Bucket=item["location"]["M"]["namespace"]["S"],
            Key=item["location"]["M"]["key"]["S"],
            Body=json_string,
        )

        print(f"Updating DynamoDB item {miro_id}")
        dynamodb.put_item(TableName="vhs-sourcedata-miro", Item=item)

    print(f"Sending message to SNS topic")
    sns = boto3.client("sns")
    deserialized_item = {k: TypeDeserializer().deserialize(v) for k, v in item.items()}
    deserialized_item["version"] = int(deserialized_item["version"])
    sns.publish(
        TopicArn="arn:aws:sns:eu-west-1:760097843905:vhs_sourcedata_miro_updates",
        Subject=f"Sent by {__file__}",
        Message=json.dumps(deserialized_item),
    )


@click.command()
@click.option(
    "--miro-id", required=True, prompt="Which Miro record do you want to edit?"
)
@click.option("--reason", required=True, prompt="Why are you editing this Miro record?")
def edit_miro_record(miro_id, reason):
    with edit_miro_vhs_record(miro_id=miro_id, reason=reason) as r:
        tmp_path = os.path.join(tempfile.mkdtemp(), f"{miro_id}.json")

        with open(tmp_path, "w") as outfile:
            outfile.write(json.dumps(r, indent=2, sort_keys=True))

        subprocess.check_call(["nano", tmp_path])

        r.clear()
        r.update(json.load(open(tmp_path)))


if __name__ == "__main__":
    # Allows us to omit the '--miro-id' argument and click is still happy.
    if len(sys.argv) == 2:
        sys.argv = [sys.argv[0], "--miro-id", sys.argv[1]]

    edit_miro_record()
