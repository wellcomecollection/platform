#!/usr/bin/env python
# -*- encoding: utf-8

import boto3


def describe_all_log_groups():
    """
    Generates describes of CloudWatch log groups, as returned by the
    DescribeLogGroups API.

    https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_DescribeLogGroups.html

    """
    client = boto3.client('logs')
    paginator = client.get_paginator('describe_log_groups')

    for page in paginator.paginate():
        yield from page['logGroups']


if __name__ == '__main__':
    stored_sizes = {
        group['logGroupName']: group['storedBytes']
        for group in describe_all_log_groups()
    }

    import collections
    from pprint import pprint

    pprint(collections.Counter(stored_sizes).most_common(10))
