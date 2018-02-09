# -*- encoding: utf-8

from wellcome_aws_utils.sns_utils import extract_sns_messages_from_lambda_event


def filter_rows_that_need_shard(rows):
    """
    Given a series of DynamoDB rows, generate just the rows that need
    new shards.
    """
    for r in rows:
        if r['reindexShard'] == 'default':
            yield r
        else:
            print(f'{r["id"]} already has a non-default reindex shard')


def main(event, _ctxt=None):
    print(f'event={event!r}')

    for sns_event in extract_sns_messages_from_lambda_event(event):
        image = sns_event.message

        # if image['reindexShard'] != 'default':
        #     print(f'{image["id"]} already has a reindex shard; skipping')
        #     continue
