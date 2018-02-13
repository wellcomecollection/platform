# -*- encoding: utf-8

import os

import boto3
import botocore
from wellcome_aws_utils.sns_utils import extract_sns_messages_from_lambda_event

from shard_manager import create_reindex_shard


SOURCE_SIZES = {
    'sierra': 5e6,
    'miro': 2.5e5,
}

SHARD_SIZE = 1500


def main(event, _ctxt=None, dynamodb_client=None):
    print(f'event={event!r}')

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    table_name = os.environ['TABLE_NAME']

    for sns_event in extract_sns_messages_from_lambda_event(event):
        row = sns_event.message

        source_id = row['sourceId']
        new_reindex_shard = create_reindex_shard(
            source_id=source_id,
            source_name=row['sourceName'],
            source_size=SOURCE_SIZES[row['sourceName']],
            shard_size=SHARD_SIZE
        )

        # If the reindex shard doesn't match the current schema, we'll
        # create a new one.
        if row.get('reindexShard') == new_reindex_shard:
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
                Key={'id': {'S': row['id']}},
                UpdateExpression='SET version = :newVersion, reindexShard=:reindexShard',
                ConditionExpression='version < :newVersion',
                ExpressionAttributeValues={
                    ':newVersion': {'N': str(version + 1)},
                    ':reindexShard': {'S': new_reindex_shard},
                }
            )
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] != 'ConditionalCheckFailedException':
                raise
            else:
                print(f'Adding shard for source_id: {source_id} failed with ConditionalCheckFailedException')
