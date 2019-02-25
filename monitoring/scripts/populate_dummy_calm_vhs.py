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

    try:
        with open(file_path) as f:
            print("Loading data from", file_path)
            for record in json.load(f):
                yield record

    except FileNotFoundError:
        print(
            "Can't find calm_records.json locally. "
            "Using data downloaded from source instead. "
        )
        for record in fetch_calm_records():
            yield record


if __name__ == "__main__":
    vhs_records_to_store = []

    dynamodb_client = boto3.resource("dynamodb")
    dynamodb_table = dynamodb_client.Table(CALM_TABLE)
    s3_client = boto3.client("s3")

    print("Loading calm data and pushing to s3 bucket")
    for i, record in enumerate(calm_records()):
        record_id = record["RecordID"][0]
        binary_record = json.dumps(record)
        string_record = str(record).encode("utf-8")
        s3_key = hashlib.sha256(string_record).hexdigest() + ".json"

        s3_client.put_object(
            Body=binary_record,
            Bucket=CALM_BUCKET,
            Key=s3_key
        )

        vhs_record = {
            "id": record_id,
            "location": {
                "key": s3_key,
                "namespace": CALM_BUCKET
            },
            "version": 1
        }

        vhs_records_to_store.append(vhs_record)

        if i % 100 == 0:
            print(f"Processed {i} records")

    print("Done with s3. Adding pointer records to VHS in batches")
    with dynamodb_table.batch_writer() as batch:
        for i, vhs_record in enumerate(vhs_records_to_store):
            batch.put_item(Item=vhs_record)

            if i % 100 == 0:
                print(f"Processed {i} records")

    print("Complete")
