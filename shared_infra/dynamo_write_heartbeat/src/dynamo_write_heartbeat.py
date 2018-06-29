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
def main(event, context, dynamodb_client=None):
    try:
        table_names = [t.strip(' ') for t in os.environ['TABLE_NAMES'].split(',')]
    except KeyError:
        raise RuntimeError('TABLE_NAMES not found')

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    for table_name in table_names:
        dynamodb_client.delete_item(TableName=table_name.strip(),
                                    Key={'id': {'S': 'not-there'}})
