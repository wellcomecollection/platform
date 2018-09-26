# -*- encoding: utf-8
import daiquiri
import json
import sys

from werkzeug.exceptions import NotFound as NotFoundError

logger = daiquiri.getLogger()


def asset_lookup_request(dynamodb_resource, table_name, s3_client, bucket_name, id):
    """
    Fetch the contents of a VHS resource.
    """

    table = dynamodb_resource.Table(table_name)
    item_response = table.get_item(Key={'id': id})

    try:
        manifest_response = s3_client.get_object(
            Bucket=bucket_name,
            Key=item_response['Item']['s3key']
        )
    except KeyError:
        raise NotFoundError(f'No asset found for id={id!r}')

    return json.loads(manifest_response['Body'].read())
