# -*- encoding: utf-8
"""
Snippet.

AWLC: When I'm trying to diagnose failures on the transformer queue,
I save the queue contents with sqs_freezeray [1], and then I want to
go through them to analyse the failures.

Our transformer messages contain pointers to S3, not the records themselves.
This snippet gets messages from the frozen SQS output, extracts the
S3 pointers, and yields the contents of the resulting objects.

Copy + paste this into a Jupyter notebook/script to use.

Usage:

    >>> for obj in get_transformer_objects():
    ...    print(obj)
    {"sourceId": "123", "sourceName": "sierra", ...}
    {"sourceId": "456", "sourceName": "sierra", ...}
    {"sourceId": "789", "sourceName": "sierra", ...}

[1]: https://github.com/wellcometrust/dockerfiles/tree/master/sqs_freezeray

"""


def get_transformer_objects(key=None):
    import json
    import boto3

    s3 = boto3.client('s3')

    if key is None:
        resp = s3.list_objects_v2(
            Bucket='wellcomecollection-platform-infra',
            Prefix='sqs'
        )
        possible_keys = [r['Key'] for r in resp['Contents']]
        key = max(possible_keys)

    if not key.startswith('sqs/'):
        key = f'sqs/{key}'

    data = s3.get_object(
        Bucket='wellcomecollection-platform-infra',
        Key=key
    )['Body'].read()

    jl = json.loads

    for line in data.splitlines():
        s3key = jl(jl(jl(line)['Body'])['Message'])['s3key']
        s3obj = s3.get_object(
            Bucket='wellcomecollection-vhs-sourcedata',
            Key=s3key
        )
        yield s3obj['Body'].read()
