# -*- encoding: utf-8 -*-
"""
Returns a manifest file requested by id in VHS where dynamodb stores the id to S3 location mapping.
"""

import daiquiri
import json
import os
import boto3
from wellcome_aws_utils.lambda_utils import log_on_error

TABLE_NAME = 'vhs-archive-manifests'
BUCKET_NAME = 'wellcomecollection-vhs-archive-manifests'

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


@log_on_error
def main(event, _, dynamodb_client=None, s3_client=None):
    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    s3_client = s3_client or boto3.client('s3')

    vhs_table_name = os.environ['VHS_TABLE_NAME']
    vhs_bucket_name = os.environ['VHS_BUCKET_NAME']

    id = event["id"]

    dynamo_response = dynamodb_client.get_item(
        TableName=vhs_table_name,
        Key={'id': {'S': id}}
    )

    manifest_response = s3_client.get_object(
        Bucket=vhs_bucket_name,
        Key=dynamo_response['Item']['s3key']['S']
    )

    return json.loads(manifest_response['Body'].read())
