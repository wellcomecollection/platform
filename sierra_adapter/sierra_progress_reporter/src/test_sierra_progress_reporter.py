# -*- encoding: utf-8 -*-

from sierra_progress_reporter import get_matching_s3_keys


def test_get_matching_s3_keys(s3_client, bucket):
    keys = ["key%d" % i for i in range(250)]
    for k in keys:
        s3_client.put_object(Bucket=bucket, Key=k)

    result = get_matching_s3_keys(s3_client=s3_client, bucket=bucket, prefix="")
    assert set(list(result)) == set(keys)


def test_get_matching_s3_keys_filters_on_prefix(s3_client, bucket):
    keys = ["key%d" % i for i in range(250)]
    for k in keys:
        s3_client.put_object(Bucket=bucket, Key=k)

    other_keys = ["_key%d" % i for i in range(100)]
    for k in other_keys:
        s3_client.put_object(Bucket=bucket, Key=k)

    result = get_matching_s3_keys(s3_client=s3_client, bucket=bucket, prefix="key")
    assert set(list(result)) == set(keys)
