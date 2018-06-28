# -*- encoding: utf-8 -*-
"""
Run a no-op write on DynamoDB (delete a non existent item) to be run in a scheduled lambda to
make DynamoDB scale down when there would otherwise be zero throughput and it otherwise wouldn't.

TABLE_NAMES: comma separated list of dynamoDb tables to call
"""

import boto3
import os

from wellcome_aws_utils.lambda_utils import log_on_error

@log_on_error
def main(event, context, endpoint_url=None):
    dynamodb = boto3.resource('dynamodb', endpoint_url=endpoint_url)
    table_names = os.environ.get('TABLE_NAMES').split(",")
    for table_name in table_names:
        table = dynamodb.Table(table_name.strip())
        table.delete_item(Key={'id' : 'not-there'})
