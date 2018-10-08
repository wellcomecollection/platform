# -*- encoding: utf-8

import json


class VHSNotFound(Exception):
    """
    Raised if an item isn't found in VHS.
    """

    pass


class VHSError(Exception):
    """
    Raised if there was an unexpected error while reading from the VHS.
    """

    pass


def read_from_vhs(dynamodb_resource, table_name, s3_client, bucket_name, id):
    """
    Fetch the JSON-decoded contents of a resource from VHS.

    This function assumes that the DynamoDB index table contains a "location"
    field with a HybridRecord(namespace, key) instance.

    """
    table = dynamodb_resource.Table(table_name)

    try:
        item_response = table.get_item(Key={"id": id})
    except Exception as err:
        raise VHSError(f"Error reading from DynamoDB: {err!r}")

    try:
        item = item_response["Item"]
    except KeyError:
        raise VHSNotFound(id)

    try:
        bucket = item["location"]["namespace"]
        key = item["location"]["key"]
    except KeyError:
        raise VHSError(f"Malformed item in DynamoDB: {item!r}")

    try:
        body = s3_client.get_object(Bucket=bucket, Key=key)["Body"].read()
    except Exception as err:
        raise VHSError(f"Error retrieving from S3: {err!r}")

    try:
        return json.loads(body)
    except ValueError as err:
        raise VHSError(f"Error decoding S3 contents as JSON: {err!r}")
