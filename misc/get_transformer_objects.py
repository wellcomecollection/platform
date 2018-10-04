# -*- encoding: utf-8
"""
Snippet.

AWLC: When I'm trying to diagnose failures on the transformer queue,
I save the queue contents with sqs_freezeray [1], and then I want to
go through them to analyse the failures.

Our transformer messages contain pointers to S3, not the records themselves.
This snippet gets messages from the frozen SQS output, extracts the
S3 pointers, and yields the contents of the resulting objects.

I kept rewriting this snippet from scratch, so I figured checking it in
would save me a bit of time later.

Copy + paste this into a Jupyter notebook/script to use.

Usage:

    >>> gto = GetTransformerObjects()
    >>> for obj in gto
    ...    print(obj)
    '{"sourceId": "123", "sourceName": "sierra", ...}'
    '{"sourceId": "456", "sourceName": "sierra", ...}'
    '{"sourceId": "789", "sourceName": "sierra", ...}'

[1]: https://github.com/wellcometrust/dockerfiles/tree/master/sqs_freezeray

"""

import json

import boto3


s3 = boto3.client("s3")
jl = json.loads


class GetTransformerObjects(object):
    def __init__(self, key=None):
        if key is None:
            resp = s3.list_objects_v2(
                Bucket="wellcomecollection-platform-infra", Prefix="sqs"
            )
            possible_keys = [r["Key"] for r in resp["Contents"]]
            key = max(possible_keys)

        if not key.startswith("sqs/"):
            key = f"sqs/{key}"

        self.data = s3.get_object(Bucket="wellcomecollection-platform-infra", Key=key)[
            "Body"
        ].read()

        self.cached_s3_records = {}

    def __iter__(self):
        for line in self.data.splitlines():
            s3key = jl(jl(jl(line)["Body"])["Message"])["s3key"]

            try:
                self.cached_s3_records[s3key]
            except KeyError:
                self.cached_s3_records[s3key] = s3.get_object(
                    Bucket="wellcomecollection-vhs-sourcedata", Key=s3key
                )["Body"].read()

            yield self.cached_s3_records[s3key]
