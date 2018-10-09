#!/usr/bin/env python
# -*- encoding: utf-8

import os

from botocore.exceptions import ClientError
import boto3
import tqdm


OLD_TABLE = "SourceData"
OLD_BUCKET = "wellcomecollection-vhs-sourcedata"

NEW_TABLE = "vhs-sourcedata-miro"
NEW_BUCKET = "wellcomecollection-vhs-sourcedata-miro"


def all_records(dynamodb_client):
    paginator = dynamodb_client.get_paginator("scan")
    for page in paginator.paginate(TableName=OLD_TABLE):
        for item in page["Items"]:
            yield item


def get_existing_records(dynamodb_client):
    """
    Generates existing Miro records from the SourceData table.
    """
    for item in tqdm.tqdm(all_records(dynamodb_client), total=2039416):
        if "reindexShard" not in item:
            print(item)

        if item["sourceName"] != {"S": "miro"}:
            continue
        yield item


os.makedirs("_miro", exist_ok=True)

if __name__ == "__main__":
    dynamodb_client = boto3.client("dynamodb")
    s3_client = boto3.client("s3")

    for item in get_existing_records(dynamodb_client):

        item["id"]["S"] = item["id"]["S"].replace("miro/", "")
        marker = os.path.join("_miro", item["id"]["S"])

        if os.path.exists(marker):
            continue

        del item["sourceName"]
        del item["sourceId"]
        del item["reindexVersion"]
        item["version"]["N"] = "1"

        old_key = item["s3key"]["S"]
        new_key = old_key.replace("miro/", "")
        del item["s3key"]

        try:
            s3_client.head_object(Bucket=NEW_BUCKET, Key=new_key)
        except ClientError as err:
            if err.response["Error"]["Code"] == "404":
                s3_client.copy_object(
                    Bucket=NEW_BUCKET,
                    Key=new_key,
                    CopySource={"Bucket": OLD_BUCKET, "Key": old_key},
                )
            else:
                raise

        item["location"] = {
            "M": {"namespace": {"S": NEW_BUCKET}, "key": {"S": new_key}}
        }

        resp = dynamodb_client.get_item(TableName=NEW_TABLE, Key={"id": item["id"]})
        if "Item" not in resp:
            dynamodb_client.put_item(TableName=NEW_TABLE, Item=item)

            resp = dynamodb_client.get_item(TableName=NEW_TABLE, Key={"id": item["id"]})
            if "Item" not in resp:
                import time

                time.sleep(1)
                resp = dynamodb_client.get_item(
                    TableName=NEW_TABLE, Key={"id": item["id"]}
                )
                assert "Item" in resp

        open(marker, "wb").write(b"")
