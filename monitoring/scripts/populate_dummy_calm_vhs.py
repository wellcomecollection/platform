#!/usr/bin/env python
# -*- encoding: utf-8

import os
import json
import boto3
import hashlib
from download_oai_harvest import fetch_calm_records

CALM_TABLE = "vhs-calm"
CALM_BUCKET = "wellcomecollection-vhs-calm"


def calm_records():
    """
    Yields calm records line by line, either from a local file downloaded
    previously, or by downloading the records individually from the source.
    """
    dir_path = os.path.dirname(os.path.realpath(__file__))
    file_path = os.path.join(dir_path, "calm_records.json")
    print(file_path)

    if os.path.exists(file_path):
        with open(file_path) as f:
            for record in json.load(f):
                yield record
    else:
        print(
            "Can't find calm_records.json locally. "
            "Using data downloaded from source instead. "
        )
        for record in fetch_calm_records():
            yield record


if __name__ == "__main__":
    dynamodb_client = boto3.client("dynamodb")
    s3_client = boto3.client("s3")

    for i, record in enumerate(calm_records()):
        record_id = record["RecordID"][0]
        binary_record = json.dumps(record)
        string_record = str(record).encode('utf-8')
        s3_key = hashlib.sha256(string_record).hexdigest() + '.json'

        s3_client.put_object(
            Body=binary_record,
            Bucket=CALM_BUCKET,
            Key=s3_key
        )

        vhs_record = {
            "id": {"S": record_id},
            "location": {
                "M": {
                    "key": {"S": s3_key},
                    "namespace": {"S": CALM_BUCKET}
                }
            },
            "version": {"N": "1"}
        }

        dynamodb_client.put_item(
            TableName=CALM_TABLE,
            Item=vhs_record
        )

        if i % 100 == 0:
            print(i)
