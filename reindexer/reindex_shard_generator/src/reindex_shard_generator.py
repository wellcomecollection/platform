# -*- encoding: utf-8

import os

import boto3
import botocore
from wellcome_aws_utils.dynamo_event import DynamoEvent
from wellcome_aws_utils.lambda_utils import log_on_error

from reindex_shard_config import create_reindex_shard


@log_on_error
def main(event, _ctxt=None, dynamodb_client=None):
    dynamodb_client = dynamodb_client or boto3.client('dynamodb')

    # TODO: Isn't the table name passed in the record?
    table_name = os.environ['TABLE_NAME']

    for record in event['Records']:
        dynamo_event = DynamoEvent(record)
        row = dynamo_event.new_image(deserialize_values=True)

        if not row:
            print("no NewImage key in dynamo update event, skipping")
            continue

        source_name = row['sourceName']
        source_id = row['id']
        new_reindex_shard = create_reindex_shard(
            source_name=row['sourceName'],
            source_id=source_id
        )

        # If the reindex shard doesn't match the current schema, we'll
        # create a new one.
        if row.get('reindexShard') == new_reindex_shard and 'reindexVersion' in row:
            print(
                f'{row["id"]} already has an up-to-date reindexShard; skipping'
            )
            continue
        else:
            print(f'Adding new reindex shard {new_reindex_shard}')

        version = row['version']

        try:
            # Then we call UpdateItem.  We only need to change the version and
            # the reindexShard fields, and we condition it on the version
            # incrementing by 1.
            dynamodb_client.update_item(
                TableName=table_name,
                Key={'id': {'S': source_id}},
                UpdateExpression='SET version = :newVersion, reindexShard=:reindexShard, reindexVersion=:reindexVersion',
                ConditionExpression='version < :newVersion',
                ExpressionAttributeValues={
                    ':newVersion': {'N': str(version + 1)},
                    ':reindexShard': {'S': new_reindex_shard},
                    ':reindexVersion': {'N': str(0)},
                }
            )
        except botocore.exceptions.ClientError as e:

            # If we get a ConditionalCheckFailedException, it means somebody
            # else wrote a record with a newer version before we could do it.
            # That's okay -- the newer record will appear later in the event
            # stream, so we can add the shard then.
            if e.response['Error']['Code'] != 'ConditionalCheckFailedException':
                raise
            else:
                print(f'Adding shard for id: {id} failed with ConditionalCheckFailedException')
