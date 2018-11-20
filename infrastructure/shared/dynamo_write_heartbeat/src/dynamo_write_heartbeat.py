# -*- encoding: utf-8 -*-
"""
Run a no-op write on DynamoDB (delete a non existent item) to be run in a scheduled lambda to
make DynamoDB scale down when there would otherwise be zero throughput and it otherwise wouldn't.

TABLE_NAMES: comma separated list of dynamoDb tables to call
"""

import os

import boto3
from botocore.exceptions import ClientError

from wellcome_aws_utils.lambda_utils import log_on_error


@log_on_error
def main(event=None, context=None, dynamodb_client=None):
    try:
        table_names = [t.strip() for t in os.environ["TABLE_NAMES"].split(",")]
    except KeyError:
        raise RuntimeError("TABLE_NAMES not found")

    dynamodb_client = dynamodb_client or boto3.client("dynamodb")
    for table_name in table_names:
        try:
            response = dynamodb_client.delete_item(
                TableName=table_name.strip(), Key={"id": {"S": "dummy-id-not-there"}}
            )

        # The purpose of this heartbeat is to bring the provisioned throughput
        # of a table down to a lower level when it's idle.
        #
        # In certain cases, a failure of the DeleteItem call is okay:
        #
        # - A ProvisionedThroughputExceededException -- if the table capacity
        #   is maxed out, then another process is using the table.  We can
        #   miss a heartbeat here.
        #
        # - A ResourceNotFoundException, which means the table has been
        #   deleted.  In that case, it's unexpected, but we aren't wasting
        #   capacity, so a failure is fine.
        #
        except ClientError as err:
            if err.response["Error"]["Code"] in (
                "ProvisionedThroughputExceededException",
                "ResourceNotFoundException",
            ):
                print(f'Benign error from DynamoDB: {err.response["Error"]["Code"]}')
            else:
                raise

        else:
            print(
                f"Heartbeat, delete_item on dynamoDb table: {table_name}, "
                f'returned-status: {response["ResponseMetadata"]["HTTPStatusCode"]}'
            )
