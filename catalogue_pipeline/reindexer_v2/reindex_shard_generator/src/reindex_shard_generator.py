# -*- encoding: utf-8

import os

import boto3

from shard_manager import create_reindex_shard

from wellcome_aws_utils.sns_utils import extract_sns_messages_from_lambda_event


SOURCE_SIZES = {
    'sierra': 5e7,
    'miro': 2.5e5,
}


def main(event, _ctxt=None, dynamodb_client=None):
    print(f'event={event!r}')

    dynamodb_client = dynamodb_client or boto3.client('dynamodb')
    table_name = os.environ['TABLE_NAME']

    for sns_event in extract_sns_messages_from_lambda_event(event):
        row = sns_event.message

        new_reindex_shard = create_reindex_shard(
            source_id=row['sourceId'],
            source_name=row['sourceName'],
            source_size=SOURCE_SIZES[row['sourceName']]
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

        # In that case, we call GetItem to get the current version of the
        # row.  This means we can do a Conditional Update to avoid overriding
        # another edit.
        current_row = dynamodb_client.get_item(
            TableName=table_name,
            Key={'id': {'S': row['id']}},
            AttributesToGet=['version']
        )
        current_version = int(current_row['Item']['version']['N'])

        # Then we call UpdateItem.  We only need to change the version and
        # the reindexShard fields, and we condition it on the version
        # incrementing by 1.
        dynamodb_client.update_item(
            TableName=table_name,
            Key={'id': {'S': row['id']}},
            UpdateExpression='SET version = :newVersion, reindexShard=:reindexShard',
            ConditionExpression='version < :newVersion',
            ExpressionAttributeValues={
                ':newVersion': {'N': str(current_version + 1)},
                ':reindexShard': {'S': new_reindex_shard},
            }
        )
