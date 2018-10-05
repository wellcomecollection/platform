# -*- encoding: utf-8

import json


def fetch_bag(dynamodb_resource, table_name, s3_client, bucket_name, id):
    """
    Fetch the contents of a Bag.
    """

    table = dynamodb_resource.Table(table_name)
    item_response = table.get_item(Key={"id": id})

    try:
        item = item_response["Item"]
    except KeyError:
        raise ValueError(f"No bag found for id={id!r}")

    bucket = item["location"]["namespace"]
    key = item["location"]["key"]

    body = s3_client.get_object(Bucket=bucket, Key=key)["Body"].read()

    return json.loads(body)
