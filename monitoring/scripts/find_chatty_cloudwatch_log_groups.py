#!/usr/bin/env python
# -*- encoding: utf-8
"""
Find chatty CloudWatch log groups.

This script looks at all the log groups in your account, and prints a
bar chart showing the groups with the most stored logs.  This can be helpful
whe trying to find unusually chatty applications.

"""

import boto3


def describe_all_log_groups():
    """
    Generates describes of CloudWatch log groups, as returned by the
    DescribeLogGroups API.

    https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_DescribeLogGroups.html

    """
    client = boto3.client("logs")
    paginator = client.get_paginator("describe_log_groups")

    for page in paginator.paginate():
        yield from page["logGroups"]


def print_bar_chart(data):
    """
    Given a list of two-tuples (label, size), print a bar chart to the
    console representing this data.

    See: https://alexwlchan.net/2018/05/ascii-bar-charts/

    """
    max_value = max(count for _, count in data)
    increment = max_value / 25

    longest_label_length = max(len(label) for label, _ in data)

    for label, count in data:

        # The ASCII block elements come in chunks of 8, so we work out how
        # many fractions of 8 we need.
        # https://en.wikipedia.org/wiki/Block_Elements
        bar_chunks, remainder = divmod(int(count * 8 / increment), 8)

        # First draw the full width chunks
        bar = "█" * bar_chunks

        # Then add the fractional part.  The Unicode code points for
        # block elements are (8/8), (7/8), (6/8), ... , so we need to
        # work backwards.
        if remainder > 0:
            bar += chr(ord("█") + (8 - remainder))

        # If the bar is empty, add a left one-eighth block
        bar = bar or "▏"

        print(f"{label.rjust(longest_label_length)} ▏ {count:#6.2f} {bar}")


if __name__ == "__main__":

    stored_sizes = {}

    for group in describe_all_log_groups():

        # All our log group names are of one of two forms:
        #
        #   platform/:service_name
        #   /aws/lambda/:lambda_name
        #
        # We don't care about the prefix, so we can strip it off to make the
        # results easier to read.
        #
        name = group["logGroupName"].split("/")[-1]

        # The CloudWatch API counts stored bytes.  This isn't an especially
        # useful metric, so convert it to gigabytes instead.
        size = group["storedBytes"] / 1024 / 1024 / 1024

        stored_sizes[name] = size
    # stored_sizes = {
    #     : group['storedBytes']
    #     for group in describe_all_log_groups()
    # }

    import collections

    chattiest_groups = collections.Counter(stored_sizes).most_common(10)

    print_bar_chart(chattiest_groups)
