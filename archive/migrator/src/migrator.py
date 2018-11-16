#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Migrator lambda

Receives notifications from the bagger service and triggers
archive ingest operations.
"""

import json
import os

from botocore.vendored import requests


def archive_bag_api_messages(bags, bucket, space):
    for bag in bags:
        yield {
            "type": "Ingest",
            "ingestType": {"id": "create", "type": "IngestType"},
            "space": {"id": f"{space}", "type": "Space"},
            "sourceLocation":{
                "type": "Location",
                "provider": {
                    "type": "Provider",
                    "id": "aws-s3-standard"
                },
                "bucket": f"{bucket}",
                "path": f"{bag}"
            }
        }


def print_result(description, result):
    print(description)
    dumped_json = json.dumps(result.json(), indent=2)
    print(dumped_json)


def call_ingest_api(bucket_name, bags, api, space):
    print(api)
    session = requests.Session()

    for message in archive_bag_api_messages(bags, bucket_name, space):
        response = session.post(api, json=message)
        status_code = response.status_code

        if status_code != 201:
            print_result(f"ERROR calling {api}", response)
        else:
            print(f"{message} -> {api} [{status_code}]")
            location = response.headers.get("Location")
            ingest = session.get(location)
            if location:
                print_result(location, ingest)


def lambda_handler(event, _):
    ingest_api_url = os.getenv("INGEST_API_URL")
    space = os.getenv("ARCHIVE_SPACE")

    for record in event["Records"]:
        message = record["Sns"]["Message"]
        decoded_message = json.loads(message)

        print(decoded_message)

        if decoded_message["upload_location"]:
            bucket = decoded_message["upload_location"]["bucket"]
            key = decoded_message["upload_location"]["key"]

            call_ingest_api(bucket, [key], ingest_api_url, space)
